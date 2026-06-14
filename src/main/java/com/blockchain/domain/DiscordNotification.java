package com.blockchain.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Discord 通知消息模型
 * 由 Swift 监听程序从 macOS 通知中心数据库提取后推送而来
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscordNotification {

    /** 通知标题（通常是发送者用户名或频道名） */
    private String title;

    /** 消息正文 */
    private String message;

    /** 副标题（如频道名 #general） */
    private String subtitle;

    /** Discord 应用 bundle id: com.hnc.Discord */
    private String bundleId;

    /** macOS 通知数据库中的原始投递时间（Unix 秒级时间戳） */
    private Long deliveredAt;

    /** macOS 通知数据库 record_id，用于客户端去重 */
    private Long recordId;

    /** 服务端接收时间（由 Spring Boot 赋值） */
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    /**
     * 将 Unix 时间戳转换为 LocalDateTime（Asia/Shanghai）
     */
    public LocalDateTime getDeliveredAtDateTime() {
        if (deliveredAt == null || deliveredAt == 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(deliveredAt),
                ZoneId.of("Asia/Shanghai")
        );
    }

    @Override
    public String toString() {
        return String.format("[Discord][record=%d] %s / %s: %s",
                recordId, subtitle, title, message);
    }
}
