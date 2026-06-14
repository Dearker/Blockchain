package com.blockchain.controller;

import com.blockchain.domain.DiscordNotification;
import com.blockchain.service.DiscordNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Discord 通知接口控制器
 *
 * <pre>
 * POST /api/notifications/discord         - Swift 监听程序推送新通知
 * GET  /api/notifications/discord/stream  - 前端 SSE 订阅实时推送
 * GET  /api/notifications/discord/history - 查询最近历史消息
 * GET  /api/notifications/discord/status  - 查看服务状态
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final DiscordNotificationService discordService;

    // ─── Swift 监听程序推送入口 ──────────────────────────────────────────────────

    /**
     * 接收来自 Swift 监听程序的 Discord 通知
     * Swift 程序每发现一条新通知就 POST 到这里
     */
    @PostMapping("/discord")
    public ResponseEntity<Map<String, Object>> receiveDiscordNotification(
            @RequestBody DiscordNotification notification) {

        boolean isNew = discordService.receiveNotification(notification);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "isNew", isNew,
                "recordId", notification.getRecordId() != null ? notification.getRecordId() : -1
        ));
    }

    // ─── SSE 实时推送（前端订阅）───────────────────────────────────────────────

    /**
     * SSE 长连接：前端订阅 Discord 实时消息流
     * <p>
     * 前端使用示例：
     * <pre>
     * const es = new EventSource('http://localhost:8000/api/notifications/discord/stream');
     * es.addEventListener('discord-message', e => {
     *     const msg = JSON.parse(e.data);
     *     console.log(msg.title, ':', msg.message);
     * });
     * es.addEventListener('history', e => {
     *     const msgs = JSON.parse(e.data);
     *     console.log('历史消息:', msgs);
     * });
     * </pre>
     */
    @GetMapping(value = "/discord/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDiscordMessages(
            @RequestParam(value = "clientId", required = false) String clientId) {

        if (clientId == null || clientId.isBlank()) {
            clientId = UUID.randomUUID().toString();
        }
        log.info("[SSE] 客户端订阅: {}", clientId);
        return discordService.createSseEmitter(clientId);
    }

    // ─── 查询接口 ────────────────────────────────────────────────────────────────

    /**
     * 查询最近历史消息（默认最近 50 条）
     */
    @GetMapping("/discord/history")
    public ResponseEntity<List<DiscordNotification>> getHistory(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        List<DiscordNotification> messages = discordService.getRecentMessages(
                Math.min(limit, 200));
        return ResponseEntity.ok(messages);
    }

    /**
     * 服务状态查询
     */
    @GetMapping("/discord/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "activeConnections", discordService.getActiveConnectionCount(),
                "queueSize", discordService.getQueueSize(),
                "service", "Discord Notification Monitor",
                "status", "running"
        ));
    }

    // ─── 兼容旧接口（保留原 /api/notifications POST）──────────────────────────

    /**
     * 兼容旧版 Swift 代码（NotificationListener.swift 用的旧接口）
     */
    @PostMapping
    public ResponseEntity<String> receiveNotificationLegacy(@RequestBody LegacyNotificationDTO dto) {
        // 转换为新格式
        DiscordNotification notification = DiscordNotification.builder()
                .title(dto.getTitle())
                .message(dto.getMessage())
                .subtitle("")
                .bundleId("com.hnc.Discord")
                .recordId(System.currentTimeMillis()) // 无 recordId 用时间戳代替
                .build();
        discordService.receiveNotification(notification);
        return ResponseEntity.ok("Received");
    }

    @lombok.Data
    public static class LegacyNotificationDTO {
        private String title;
        private String message;
    }
}
