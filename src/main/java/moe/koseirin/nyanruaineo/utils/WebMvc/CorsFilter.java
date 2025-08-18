package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;


import java.io.IOException;

@Configuration
public class CorsFilter implements Filter {
    private static final String REQUEST_OPTIONS = "OPTIONS";
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 获取源站
        String origin  = request.getHeader(HttpHeaders.ORIGIN);
        // 允许的跨域的域名
        response.addHeader("Access-Control-Allow-Origin", origin);
        // 允许携带 cookies 等认证信息
        response.addHeader("Access-Control-Allow-Credentials", "true");
        // 允许跨域的方法
        response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH, PUT, HEAD");
        // 允许跨域请求携带的请求头
        response.addHeader("Access-Control-Allow-Headers", "LoginForWeb, verifyCode,Token, Event, Authorization, Content-Type, Content-Length, auth-token, Accept, X-Requested-With`");
        // 返回结果可以用于缓存的最长时间，单位是秒。-1表示禁用
        response.addHeader("Access-Control-Max-Age", "36000");


        // 跨域预检请求，直接返回
        if (REQUEST_OPTIONS.equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }
        //   }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}