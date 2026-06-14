import Foundation
import UserNotifications

@main
struct NotificationReader {
    static func main() {
        let center = UNUserNotificationCenter.current()

        // 第一步：请求权限
        center.requestAuthorization(options: [.alert]) { granted, error in
            guard granted else {
                print("权限被拒绝: \(error?.localizedDescription ?? "")")
                exit(1)
            }

            // 第二步：获取通知
            center.getDeliveredNotifications { notifications in
                for notification in notifications {
                    let content = notification.request.content
                    print("""
                    [App] \(content.userInfo["CFBundleIdentifier"] ?? "Unknown")
                    [Title] \(content.title)
                    [Body] \(content.body)
                    [Time] \(notification.date)
                    ---
                    """)
                }
                exit(0)
            }
        }

        // 保持运行
        RunLoop.current.run()
    }
}