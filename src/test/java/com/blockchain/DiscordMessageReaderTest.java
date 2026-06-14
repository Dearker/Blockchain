package com.blockchain;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class DiscordMessageReaderTest {

    @Test
    public void testReadMessages() throws IOException {
        System.out.println("开始测试读取 Discord 消息...");

        DiscordMessageReader reader = new DiscordMessageReader();

        // 读取消息
        List<DiscordMessageReader.DiscordMessage> messages = reader.readMessages();

        // 打印所有消息
        System.out.println("\n找到的消息列表：");
        System.out.println("----------------------------------------");
        for (DiscordMessageReader.DiscordMessage message : messages) {
            System.out.println(message);
        }
        System.out.println("----------------------------------------");

    }

    @Test
    public void testRealTimeListening() throws IOException, InterruptedException {
        System.out.println("开始测试实时监听 Discord 消息...");

        DiscordMessageReader reader = new DiscordMessageReader();

        try {
            // 开始监听
            reader.startListening();

            System.out.println("正在实时监听 Discord 消息...");
            System.out.println("按 Ctrl+C 停止监听");

            // 保持程序运行
            while (true) {
                List<DiscordMessageReader.DiscordMessage> messages = reader.readMessages();
                if (!messages.isEmpty()) {
                    System.out.println("\n新消息：");
                    System.out.println("----------------------------------------");
                    for (DiscordMessageReader.DiscordMessage message : messages) {
                        System.out.println(message);
                    }
                    System.out.println("----------------------------------------");
                }
                Thread.sleep(1000); // 每秒检查一次
            }
        } catch (Exception e) {
            System.out.println("监听过程中出错：" + e.getMessage());
            e.printStackTrace();
        } finally {
            reader.stopListening();
        }
    }

    @Test
    public void readNotifications() {
        try {
            String script = "tell application \"System Events\"\n" +
                    "get every notification of notification center\n" +
                    "end tell";

            Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            System.out.println(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void readNotificationTest() throws Exception{
        String script = "tell application \"display notification '测试 Safari' with title '测试'" +
                "end tell";

        Process process = Runtime.getRuntime().exec(new String[]{"osascript", "-e", script});
    }

}