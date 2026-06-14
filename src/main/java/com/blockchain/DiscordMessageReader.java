package com.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiscordMessageReader {
    private final String logFilePath;
    private final ObjectMapper objectMapper;
    private WatchService watchService;
    private Path path;

    public DiscordMessageReader() {
        // 获取 Discord 缓存目录
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();
        String discordPath;
        
        if (osName.contains("mac")) {
            discordPath = userHome + "/Library/Application Support/discord/Cache/Cache_Data/";
        } else if (osName.contains("windows")) {
            discordPath = userHome + "/AppData/Roaming/discord/Cache/Cache_Data/";
        } else {
            discordPath = userHome + "/.config/discord/Cache/Cache_Data/";
        }
        
        // 查找最新的缓存文件
        this.logFilePath = findLatestCacheFile(discordPath);
        System.out.println("使用缓存文件: " + this.logFilePath);
        
        this.objectMapper = new ObjectMapper();
        this.path = Paths.get(logFilePath);
    }

    /**
     * 查找最新的缓存文件
     */
    private String findLatestCacheFile(String directory) {
        try {
            Path dir = Paths.get(directory);
            if (!Files.exists(dir)) {
                System.out.println("警告：Discord 缓存目录不存在：" + directory);
                return "";
            }

            return Files.list(dir)
                    .filter(path -> path.toString().endsWith("_0"))
                    .max(Comparator.comparingLong(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }))
                    .map(Path::toString)
                    .orElse("");
        } catch (IOException e) {
            System.out.println("查找缓存文件时出错：" + e.getMessage());
            return "";
        }
    }

    /**
     * 开始监听 Discord 消息
     */
    public void startListening() throws IOException {
        if (!Files.exists(path)) {
            System.out.println("警告：Discord 日志文件不存在：" + logFilePath);
            return;
        }

        watchService = FileSystems.getDefault().newWatchService();
        path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
        System.out.println("开始监听 Discord 消息...");
    }

    /**
     * 读取并解析消息
     * @return 解析后的消息列表
     */
    public List<DiscordMessage> readMessages() throws IOException {
        List<DiscordMessage> messages = new ArrayList<>();
        
        if (!Files.exists(path)) {
            System.out.println("错误：文件不存在：" + logFilePath);
            return messages;
        }

        try {
            String encoding = detectEncoding(logFilePath);
            System.out.println("检测到的文件编码: " + encoding);
            
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            List<String> lines = Files.readAllLines(path, charset);
            
            System.out.println("读取到 " + lines.size() + " 行数据");
            
            for (String line : lines) {
                try {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    DiscordMessage message = parseMessage(line);
                    if (message != null) {
                        messages.add(message);
                    }
                } catch (Exception e) {
                    System.out.println("解析行时出错: " + line);
                    System.out.println("错误信息: " + e.getMessage());
                }
            }
            
            System.out.println("成功解析 " + messages.size() + " 条消息");
            
        } catch (Exception e) {
            System.out.println("读取文件时出错：" + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * 解析单条消息
     */
    private DiscordMessage parseMessage(String content) throws IOException {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            
            // 检查必要的字段是否存在
            if (!rootNode.has("message") || !rootNode.has("author") || !rootNode.has("channel")) {
                return null;
            }
            
            String message = rootNode.path("message").asText();
            String author = rootNode.path("author").path("username").asText();
            String channel = rootNode.path("channel").path("name").asText();
            long timestamp = rootNode.path("timestamp").asLong();

            if (message.isEmpty() || author.isEmpty() || channel.isEmpty()) {
                return null;
            }

            return new DiscordMessage(author, message, channel, timestamp);
        } catch (Exception e) {
            System.out.println("解析消息内容时出错：" + content);
            System.out.println("错误信息：" + e.getMessage());
            return null;
        }
    }

    /**
     * 检测文件编码
     */
    private String detectEncoding(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buf = new byte[4096];
            UniversalDetector detector = new UniversalDetector(null);
            
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            
            String encoding = detector.getDetectedCharset();
            detector.reset();
            return encoding;
        }
    }

    /**
     * 停止监听
     */
    public void stopListening() throws IOException {
        if (watchService != null) {
            watchService.close();
            System.out.println("已停止监听 Discord 消息");
        }
    }

    /**
     * Discord 消息实体类
     */
    public static class DiscordMessage {
        private final String author;
        private final String content;
        private final String channel;
        private final long timestamp;

        public DiscordMessage(String author, String content, String channel, long timestamp) {
            this.author = author;
            this.content = content;
            this.channel = channel;
            this.timestamp = timestamp;
        }

        public String getAuthor() {
            return author;
        }

        public String getContent() {
            return content;
        }

        public String getChannel() {
            return channel;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s", channel, author, content);
        }
    }
} 