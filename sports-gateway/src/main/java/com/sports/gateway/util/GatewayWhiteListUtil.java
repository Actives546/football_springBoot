package com.sports.gateway.util;

import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * 网关白名单工具类
 * 职责：统一管理网关白名单相关逻辑，包括路径白名单和IP白名单
 */
public class GatewayWhiteListUtil {

    /**
     * 路径匹配器，用于匹配URL路径模式
     * 声明为static final，防止被修改
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 本地IP白名单列表
     * 包含允许访问的本地IP地址和主机名
     */
    private static final List<String> LOCALHOST_IPS = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "localhost"
    );

    /**
     * 私有构造方法，防止实例化
     */
    private GatewayWhiteListUtil() {
    }

    /**
     * 检查请求路径是否在白名单中
     *
     * @param path      请求路径
     * @param whiteList 白名单路径模式列表
     * @return true-在白名单中，false-不在白名单中
     */
    public static boolean isWhiteList(String path, List<String> whiteList) {
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        for (String pattern : whiteList) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查IP地址是否为本地IP
     *
     * @param clientIp 客户端IP地址
     * @param hostName 客户端主机名
     * @return true-是本地IP，false-不是本地IP
     */
    public static boolean isLocalhost(String clientIp, String hostName) {
        if (clientIp == null && hostName == null) {
            return false;
        }
        return LOCALHOST_IPS.stream()
                .anyMatch(ip -> (clientIp != null && ip.equalsIgnoreCase(clientIp))
                        || (hostName != null && ip.equalsIgnoreCase(hostName)));
    }

    /**
     * 获取路径匹配器
     *
     * @return AntPathMatcher实例
     */
    public static AntPathMatcher getPathMatcher() {
        return PATH_MATCHER;
    }

    /**
     * 获取本地IP白名单列表
     *
     * @return 本地IP白名单列表
     */
    public static List<String> getLocalhostIps() {
        return LOCALHOST_IPS;
    }
}
