import Foundation
import UserNotifications

class NotificationDelegate: NSObject, NSUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: NSUserNotificationCenter, didDeliver notification: NSUserNotification) {
        let title = notification.title ?? "无标题"
        let message = notification.informativeText ?? "无内容"

        // 发送到Spring Boot服务
        sendToSpringBoot(title: title, message: message)
    }

    func sendToSpringBoot(title: String, message: String) {
        let url = URL(string: "http://localhost:8080/api/notifications")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let json: [String: Any] = ["title": title, "message": message]
        request.httpBody = try? JSONSerialization.data(withJSONObject: json)

        URLSession.shared.dataTask(with: request).resume()
    }
}

let delegate = NotificationDelegate()
NSUserNotificationCenter.default.delegate = delegate
RunLoop.current.run()