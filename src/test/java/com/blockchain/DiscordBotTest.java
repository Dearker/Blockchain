package com.blockchain;

import com.blockchain.adapter.DiscordBot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.junit.Test;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class DiscordBotTest {

    @Test
    public void botTest(){
        // 使用你的机器人 Token 初始化
        JDABuilder.createDefault("2c479536564cb24ae3b8dc552990d99ed04e23485ea76b379054bc25a9a908d5")
                .enableIntents(GatewayIntent.MESSAGE_CONTENT) // 必须启用才能读取消息内容
                .addEventListeners(new DiscordBot())
                .build();
    }

    @Test
    public void discordTest() throws Exception {
        // 日志文件路径
        String logFilePath = System.getProperty("user.home") +
                "/Library/Application Support/discord/Cache/Cache_Data/ffff66ead86896db_0";
        System.out.println(logFilePath);
        Path path = Paths.get(logFilePath);
        WatchService watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        System.out.println("Listening for Discord log changes...");

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.context().toString().equals(path.getFileName().toString())) {
                    System.out.println("Log file changed!");
                    // 读取文件内容并解析
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        System.out.println(line);
                    }
                }
            }
            key.reset();
        }
    }

    @Test
    public void parseLogTest() throws Exception {
        // 日志文件路径
        String logFilePath = System.getProperty("user.home") +
                "/Library/Application Support/discord/Cache/Cache_Data/ffff66ead86896db_0";

        // 检查文件是否存在
        Path path = Paths.get(logFilePath);
        if (!Files.exists(path)) {
            System.out.println("警告：Discord 日志文件不存在：" + logFilePath);
            return;
        }

        String binary = "binary";
        // 检测文件编码
        String encoding = detectEncoding(logFilePath);
        System.out.println("检测到的文件编码: " + encoding);
        
        // 使用检测到的编码读取文件
        Charset charset = encoding != null ? Charset.forName(binary) : StandardCharsets.UTF_8;
        List<String> lines = Files.readAllLines(path, Charset.forName("binary"));
        
        ObjectMapper mapper = new ObjectMapper();
        for (String line : lines) {
            try {
                if (line.trim().isEmpty()) {
                    continue;
                }
                parseLog(line, mapper);
            } catch (Exception e) {
                System.out.println("解析行时出错: " + line);
                System.out.println("错误信息: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void parseLog(String logContent, ObjectMapper mapper) throws Exception {
        JsonNode rootNode = mapper.readTree(logContent);
        // 提取通知内容
        String message = rootNode.path("message").asText();
        if (!message.isEmpty()) {
            System.out.println("新的 Discord 通知: " + message);
        }
    }
    
    private static String detectEncoding(String filePath) throws Exception {
        byte[] buf = new byte[16384000];
        FileInputStream fis = new FileInputStream(filePath);

        UniversalDetector detector = new UniversalDetector(null);
        int nread;
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        detector.dataEnd();

        String encoding = detector.getDetectedCharset();
        detector.reset();
        fis.close();

        return encoding;
    }

    @Test
    public void fileCheckTest(){
        String filePath = System.getProperty("user.home") +
                "/Library/Application Support/discord/Cache/Cache_Data/ffff66ead86896db_0";
        try {
            if (Files.size(Paths.get(filePath)) == 0) {
                System.out.println("文件为空！");
            } else {
                System.out.println("文件不为空！");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
