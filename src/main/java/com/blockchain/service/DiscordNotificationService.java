package com.blockchain.service;

import com.blockchain.domain.DiscordNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Discord 通知服务
 * <p>
 * 职责：
 * 1. 接收来自 Swift 监听程序的通知（HTTP POST）
 * 2. 去重（基于 recordId）
 * 3. 存入内存队列（最近 500 条）
 * 4. 通过 SSE 实时推送给所有订阅的前端客户端
 * </p>
 */
@Slf4j
@Service
public class DiscordNotificationService {

    /** SSE 连接池：key = 客户端唯一ID */
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 内存消息队列（最近 500 条，线程安全） */
    private final Deque<DiscordNotification> messageQueue =
            new ArrayDeque<>(500);

    /** 已接收的 recordId 集合，用于去重（最多保留 2000 个） */
    private final Set<Long> seenRecordIds =
            Collections.newSetFromMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > 2000;
                }
            });

    private static final int MAX_QUEUE_SIZE = 500;

    // ─── 接收新通知 ─────────────────────────────────────────────────────────────

    /**
     * 接收并处理一条来自 Swift 监听程序的 Discord 通知
     *
     * @param notification 通知对象
     * @return true=新消息已处理；false=重复消息已忽略
     */
    public boolean receiveNotification(DiscordNotification notification) {
        // 1. 去重检查
        if (notification.getRecordId() != null) {
            synchronized (seenRecordIds) {
                if (seenRecordIds.contains(notification.getRecordId())) {
                    log.debug("[Discord] 重复消息忽略 recordId={}", notification.getRecordId());
                    return false;
                }
                seenRecordIds.add(notification.getRecordId());
            }
        }

        // 2. 补充接收时间
        if (notification.getReceivedAt() == null) {
            notification.setReceivedAt(LocalDateTime.now());
        }

        // 3. 入队
        synchronized (messageQueue) {
            if (messageQueue.size() >= MAX_QUEUE_SIZE) {
                messageQueue.pollFirst(); // 移除最旧的
            }
            messageQueue.addLast(notification);
        }

        log.info("[Discord] 新消息 | record={} | {}/{}: {}",
                notification.getRecordId(),
                notification.getSubtitle(),
                notification.getTitle(),
                notification.getMessage());

        // 4. SSE 广播
        broadcastToAll(notification);

        return true;
    }

    // ─── SSE 订阅管理 ──────────────────────────────────────────────────────────

    /**
     * 创建并注册一个新的 SSE 连接
     *
     * @param clientId 客户端唯一标识（由调用方生成，如 UUID）
     * @return SseEmitter 实例
     */
    public SseEmitter createSseEmitter(String clientId) {
        // 超时设为 0 = 不超时（持久连接）
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            log.info("[SSE] 客户端断开: {}", clientId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            log.info("[SSE] 客户端超时: {}", clientId);
        });
        emitter.onError(e -> {
            emitters.remove(clientId);
            log.warn("[SSE] 客户端错误: {} - {}", clientId, e.getMessage());
        });

        emitters.put(clientId, emitter);
        log.info("[SSE] 新客户端连接: {}，当前连接数: {}", clientId, emitters.size());

        // 立即推送最近历史消息（可选，最多 20 条）
        List<DiscordNotification> recent = getRecentMessages(20);
        if (!recent.isEmpty()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("history")
                        .data(recent));
            } catch (IOException e) {
                log.warn("[SSE] 推送历史消息失败: {}", e.getMessage());
            }
        }

        return emitter;
    }

    /**
     * 广播一条新通知给所有 SSE 客户端
     */
    private void broadcastToAll(DiscordNotification notification) {
        if (emitters.isEmpty()) {
            log.debug("[SSE] 无连接客户端，跳过广播");
            return;
        }

        List<String> deadClients = new ArrayList<>();

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("discord-message")
                        .id(String.valueOf(notification.getRecordId()))
                        .data(notification));
            } catch (IOException e) {
                deadClients.add(clientId);
                log.warn("[SSE] 推送失败，移除客户端: {}", clientId);
            }
        });

        deadClients.forEach(emitters::remove);
    }

    // ─── 查询接口 ───────────────────────────────────────────────────────────────

    /**
     * 获取最近 N 条消息（从新到旧）
     */
    public List<DiscordNotification> getRecentMessages(int limit) {
        synchronized (messageQueue) {
            List<DiscordNotification> list = new ArrayList<>(messageQueue);
            // 倒序（最新的在前）
            Collections.reverse(list);
            return list.subList(0, Math.min(limit, list.size()));
        }
    }

    /**
     * 获取当前 SSE 连接数
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * 获取队列中消息总数
     */
    public int getQueueSize() {
        synchronized (messageQueue) {
            return messageQueue.size();
        }
    }
}
