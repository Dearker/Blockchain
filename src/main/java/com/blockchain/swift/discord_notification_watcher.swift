#!/usr/bin/env swift
//
// discord_notification_watcher.swift
// 监听 macOS 通知中心数据库，提取 Discord 新消息并 POST 给 Spring Boot
//
// 编译方式（推荐）：
//   swiftc discord_notification_watcher.swift -o discord_watcher
// 运行：
//   ./discord_watcher
//
// 或直接解释执行：
//   swift discord_notification_watcher.swift
//

import Foundation
import SQLite3

// ─── 配置 ───────────────────────────────────────────────────────────────────
let SPRING_BOOT_URL = "http://localhost:8000/api/notifications/discord"
let POLL_INTERVAL_SECONDS: Double = 3.0          // 轮询间隔（秒）
let DISCORD_BUNDLE_ID    = "com.hnc.Discord"     // Discord 的 bundle identifier

// macOS 通知数据库路径（Ventura 及以下）
// macOS Sonoma (14+) 路径相同，但需要完全磁盘访问权限
let DB_PATH = NSHomeDirectory() + "/Library/Application Support/com.apple.notificationcenter/db2/db"

// ─── 数据模型 ─────────────────────────────────────────────────────────────────
struct DiscordNotification: Codable {
    let title: String        // 发送者 / 频道名
    let message: String      // 消息内容
    let subtitle: String     // 子标题（如频道名）
    let bundleId: String
    let deliveredAt: Int64   // Unix 时间戳（秒）
    let recordId: Int64      // 数据库记录 ID，用于去重
}

// ─── SQLite 辅助 ──────────────────────────────────────────────────────────────
class NotificationDB {
    private var db: OpaquePointer?
    private let path: String

    init(path: String) {
        self.path = path
    }

    func open() -> Bool {
        // 以只读方式打开，避免锁冲突
        let flags = SQLITE_OPEN_READONLY | SQLITE_OPEN_NOMUTEX
        if sqlite3_open_v2(path, &db, flags, nil) != SQLITE_OK {
            let err = String(cString: sqlite3_errmsg(db))
            fputs("[ERROR] 无法打开通知数据库: \(err)\n路径: \(path)\n提示: 请在「系统偏好设置 > 安全性与隐私 > 完全磁盘访问权限」中添加终端或本程序\n", stderr)
            return false
        }
        return true
    }

    func close() {
        if db != nil {
            sqlite3_close(db)
            db = nil
        }
    }

    /// 查询指定 recordId 之后的 Discord 新通知
    func fetchNewDiscordNotifications(afterRecordId: Int64) -> [DiscordNotification] {
        guard db != nil else { return [] }

        var results: [DiscordNotification] = []

        // macOS 通知数据库表结构：
        // record_id, app_id, uuid, data(BLOB plist), presented, delivered_date, ...
        // app 表: app_id, identifier (bundle id)
        let sql = """
            SELECT
                r.record_id,
                a.identifier,
                r.data,
                r.delivered_date
            FROM record r
            JOIN app a ON r.app_id = a.app_id
            WHERE a.identifier = '\(DISCORD_BUNDLE_ID)'
              AND r.record_id > \(afterRecordId)
            ORDER BY r.record_id ASC
            LIMIT 50
            """

        var stmt: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) != SQLITE_OK {
            let err = String(cString: sqlite3_errmsg(db))
            fputs("[ERROR] SQL 准备失败: \(err)\n", stderr)
            return results
        }
        defer { sqlite3_finalize(stmt) }

        while sqlite3_step(stmt) == SQLITE_ROW {
            let recordId   = sqlite3_column_int64(stmt, 0)
            let bundleId   = String(cString: sqlite3_column_text(stmt, 1))
            let deliveredAt = sqlite3_column_int64(stmt, 3)

            // 解析 BLOB plist 数据
            guard let blobPtr = sqlite3_column_blob(stmt, 2) else { continue }
            let blobSize = sqlite3_column_bytes(stmt, 2)
            let data = Data(bytes: blobPtr, count: Int(blobSize))

            if let (title, body, subtitle) = parsePlistNotification(data: data) {
                // 过滤掉空消息
                guard !body.isEmpty else { continue }
                let notif = DiscordNotification(
                    title: title,
                    message: body,
                    subtitle: subtitle,
                    bundleId: bundleId,
                    deliveredAt: deliveredAt,
                    recordId: recordId
                )
                results.append(notif)
            }
        }
        return results
    }

    /// 获取当前最大 record_id（用于初始化游标，避免重放历史消息）
    func fetchMaxRecordId() -> Int64 {
        guard db != nil else { return 0 }
        var maxId: Int64 = 0
        let sql = "SELECT MAX(record_id) FROM record WHERE app_id IN (SELECT app_id FROM app WHERE identifier = '\(DISCORD_BUNDLE_ID)')"
        var stmt: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
            if sqlite3_step(stmt) == SQLITE_ROW {
                maxId = sqlite3_column_int64(stmt, 0)
            }
        }
        sqlite3_finalize(stmt)
        return maxId
    }
}

// ─── Plist 解析 ───────────────────────────────────────────────────────────────
/// 解析 macOS 通知数据库中存储的二进制 plist，提取 title / body / subtitle
func parsePlistNotification(data: Data) -> (title: String, body: String, subtitle: String)? {
    guard let plist = try? PropertyListSerialization.propertyList(from: data, options: [], format: nil),
          let dict = plist as? [String: Any] else {
        return nil
    }

    // 通知 plist 结构（简化）：
    // req.content -> { title, body, subtitle, ... }
    // 或直接在顶层 { aps: { alert: { title, body } } }
    var title    = ""
    var body     = ""
    var subtitle = ""

    func extractFromDict(_ d: [String: Any]) {
        if let t = d["title"] as? String, !t.isEmpty { title = t }
        if let b = d["body"] as? String, !b.isEmpty { body = b }
        if let s = d["subtitle"] as? String, !s.isEmpty { subtitle = s }
    }

    extractFromDict(dict)

    // 嵌套在 req.content
    if let req = dict["req"] as? [String: Any],
       let content = req["content"] as? [String: Any] {
        extractFromDict(content)
    }

    // 嵌套在 aps.alert
    if let aps = dict["aps"] as? [String: Any] {
        if let alert = aps["alert"] as? [String: Any] {
            extractFromDict(alert)
        } else if let alertStr = aps["alert"] as? String, body.isEmpty {
            body = alertStr
        }
    }

    // 有时直接存 NSConcreteNotification 数据，title 在 "titl" 键
    if title.isEmpty, let t = dict["titl"] as? String { title = t }
    if body.isEmpty,  let b = dict["body"] as? String { body = b }

    guard !body.isEmpty || !title.isEmpty else { return nil }
    return (title, body, subtitle)
}

// ─── HTTP 推送 ────────────────────────────────────────────────────────────────
func postToSpringBoot(notifications: [DiscordNotification]) {
    guard !notifications.isEmpty else { return }
    guard let url = URL(string: SPRING_BOOT_URL) else { return }

    for notif in notifications {
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = 5

        let payload: [String: Any] = [
            "title"       : notif.title,
            "message"     : notif.message,
            "subtitle"    : notif.subtitle,
            "bundleId"    : notif.bundleId,
            "deliveredAt" : notif.deliveredAt,
            "recordId"    : notif.recordId
        ]

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { continue }
        request.httpBody = body

        // 同步发送（Swift 脚本环境下简化处理）
        let semaphore = DispatchSemaphore(value: 0)
        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                fputs("[WARN] POST 失败: \(error.localizedDescription)\n", stderr)
            } else {
                let code = (response as? HTTPURLResponse)?.statusCode ?? 0
                print("[PUSH] record_id=\(notif.recordId) -> HTTP \(code) | \(notif.title): \(notif.message.prefix(60))")
            }
            semaphore.signal()
        }.resume()
        semaphore.wait()
    }
}

// ─── 主循环 ───────────────────────────────────────────────────────────────────
print("=== Discord 通知监听器启动 ===")
print("数据库路径: \(DB_PATH)")
print("推送地址  : \(SPRING_BOOT_URL)")
print("轮询间隔  : \(POLL_INTERVAL_SECONDS)s")
print("按 Ctrl+C 停止\n")

let notifDB = NotificationDB(path: DB_PATH)
guard notifDB.open() else {
    exit(1)
}

// 初始化游标：跳过启动前的历史消息
var lastRecordId = notifDB.fetchMaxRecordId()
print("[INIT] 当前最大 record_id = \(lastRecordId)，只推送新消息\n")

// 优雅退出处理
signal(SIGINT)  { _ in print("\n[STOP] 收到中断信号，退出"); exit(0) }
signal(SIGTERM) { _ in print("\n[STOP] 收到终止信号，退出"); exit(0) }

// 主轮询循环
while true {
    // 重新打开数据库（每次轮询重开，避免 WAL 缓存问题）
    let db = NotificationDB(path: DB_PATH)
    if db.open() {
        let newNotifs = db.fetchNewDiscordNotifications(afterRecordId: lastRecordId)
        if !newNotifs.isEmpty {
            print("[\(Date())] 发现 \(newNotifs.count) 条新 Discord 通知")
            postToSpringBoot(notifications: newNotifs)
            lastRecordId = newNotifs.last!.recordId
        }
        db.close()
    }

    Thread.sleep(forTimeInterval: POLL_INTERVAL_SECONDS)
}
