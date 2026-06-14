package com.blockchain.adapter;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
public class DiscordBot extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 排除机器人自身的消息
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();
        String channelName = event.getChannel().getName();

        System.out.printf("[%s] %s: %s\n",
                channelName,
                event.getAuthor().getName(),
                content
        );

        // 示例：响应特定命令
        if (content.equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
        }
    }

}
