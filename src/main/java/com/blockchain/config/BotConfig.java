package com.blockchain.config;

import com.blockchain.listener.BotMessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.context.annotation.Bean;

//@Configuration
public class BotConfig {

    @Bean
    public JDA jda() {
        JDA jda = JDABuilder.createDefault("MTM0MzA0MjMzMTQ1NjE3NjI1MA.G-IjfE.qXv5aNbIn5rhiNCisNP8MhEI-EVGm67tfb9ZXM")
                .build();
        // 注册事件监听器
        jda.addEventListener(new BotMessageListener());
        return jda;
    }

}
