package com.blockchain.listener;

import jakarta.annotation.Resource;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

//@Component
public class BotMessageListener extends ListenerAdapter {

    private static final String CHANNEL_ID = "1229962956884807740";

    @Resource
    private JDA jda;

    // 监听新消息
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannel().getId().equals(CHANNEL_ID)) {
            System.out.println("[新消息] " + event.getMessage().getContentDisplay());
        }
    }

    // 获取最新1条消息（异步）
    public void getLatestMessage() {
        TextChannel channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel != null) {
            channel.getHistory().retrievePast(1).queue(messages -> {
                if (!messages.isEmpty()) {
                    Message latest = messages.get(0);
                    System.out.println("[最新消息] " + latest.getContentDisplay());
                }
            });
        }
    }

    // 获取历史消息（分页，默认100条）
    public void getHistoryMessages(int limit) {
        TextChannel channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel != null) {
            MessageHistory history = channel.getHistory();
            history.retrievePast(limit).queue(messages -> {
                System.out.println("===== 历史消息（最近" + limit + "条） =====");
                for (Message msg : messages) {
                    System.out.printf("[%s] %s: %s\n",
                            msg.getTimeCreated(),
                            msg.getAuthor().getName(),
                            msg.getContentDisplay());
                }
            });
        }
    }

    // 获取第一条消息（异步）
    public void getFirstMessage() {
        TextChannel channel = jda.getTextChannelById(CHANNEL_ID);
        if (channel != null) {
            channel.getHistoryFromBeginning(1).queue(history -> {
                List<Message> messages = history.getRetrievedHistory();
                if (!messages.isEmpty()) {
                    Message first = messages.get(0);
                    System.out.println("[第一条消息] " + first.getContentDisplay());
                }
            });
        }
    }

}
