package com.blockchain.config;

import com.blockchain.listener.BotMessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

//@Configuration
public class BotConfig {

    @Value("bot.token")
    private String token;

    @Bean
    public JDA jda() {
        JDA jda = JDABuilder.createDefault(token)
                .build();
        // 注册事件监听器
        jda.addEventListener(new BotMessageListener());
        return jda;
    }

}
