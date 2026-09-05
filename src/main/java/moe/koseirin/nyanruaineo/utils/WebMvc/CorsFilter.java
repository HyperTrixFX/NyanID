package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import java.io.IOException;

/**
 * CORS 过滤器。
 * <p>
 * 安全约束：<b>绝不反射任意 Origin</b>。仅当请求 Origin 命中配置白名单
 * {@code NyanidSetting.allowedOriginPatterns}（逗号分隔，支持 {@code *} 通配）时才回写
 * {@code Access-Control-Allow-Origin} 与 {@code Access-Control-Allow-Credentials: true}。
 * 未命中或未配置白名单时不输出任何 CORS 头。
 */
@Configuration
public class CorsFilter implements Filter {

    private static final String REQUEST_OPTIONS = "OPTIONS";

    @Value("${NyanidSetting.allowedOriginPatterns:}")
    private String allowedOriginPatterns;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (originAllowed(origin)) {
            response.addHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Access-Control-Allow-Credentials", "true");
            response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH, PUT, HEAD");
            response.addHeader("Access-Control-Allow-Headers", "LoginForWeb, Event, Authorization, Content-Type, Content-Length, auth-token, Accept, X-Requested-With");
            response.addHeader("Access-Control-Max-Age", "36000");
        }

        if (REQUEST_OPTIONS.equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean originAllowed(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        if (allowedOriginPatterns == null || allowedOriginPatterns.isBlank()) {
            return false;
        }
        for (String raw : allowedOriginPatterns.split(",")) {
            String pattern = raw.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            if ("*".equals(pattern)) {
                return true;
            }
            // `*` 通配转正则（`*` 匹配任意序列，其余按字面匹配）
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            if (origin.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
    }
}
