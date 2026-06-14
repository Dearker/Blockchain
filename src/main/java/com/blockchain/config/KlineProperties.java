package com.blockchain.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * K线数据存储路径配置
 * 支持跨平台，可在 application.yml 中配置 kline.data.path
 * 未配置时自动按操作系统选择默认值
 *
 * @author blockchain
 */
@ConfigurationProperties(prefix = "kline.data")
@Component
public class KlineProperties {

    /**
     * 本地K线文件存储根目录
     * 在 application.yml 中配置，未配置则使用各平台默认值
     */
    private String path;

    /**
     * OS 自动检测的默认路径（当 yml 未配置时使用）
     */
    private static final String DEFAULT_PATH_WINDOWS = "D:\\kdata\\lines";
    private static final String DEFAULT_PATH_MAC     = "/Volumes/husky/kdata/lines";
    private static final String DEFAULT_PATH_LINUX   = "/tmp/kdata/lines";

    @PostConstruct
    public void init() {
        if (path == null || path.isEmpty()) {
            String os = System.getProperty("os.name", "generic").toLowerCase();
            if (os.contains("win")) {
                path = DEFAULT_PATH_WINDOWS;
            } else if (os.contains("mac")) {
                path = DEFAULT_PATH_MAC;
            } else {
                path = DEFAULT_PATH_LINUX;
            }
        }
        // 统一分隔符为当前平台风格，并去除末尾分隔符，避免拼接时出现双分隔符
        path = path.replace("\\", "/");
        path = path.replace("/", java.io.File.separator);
        path = path.replaceAll("[" + java.io.File.separator + "]+$", "");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
