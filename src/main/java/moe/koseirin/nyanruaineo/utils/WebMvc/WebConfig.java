package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    private final IPSecurityDetection ipSecurityDetection;


    private final AuthenticateCheck authenticateCheck;

    @Value("${NyanidSetting.allowedOriginPatterns}")
    private String allowedOriginPatterns;

    public WebConfig(IPSecurityDetection ipSecurityDetection, AuthenticateCheck authenticateCheck) {
        this.ipSecurityDetection = ipSecurityDetection;
        this.authenticateCheck = authenticateCheck;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipSecurityDetection)
                .addPathPatterns("/api/yggdrasil/authserver/**")
                .addPathPatterns("/api/zako/v1/login")
                .addPathPatterns("/api/zako/v1/login/2fa");
        registry.addInterceptor(authenticateCheck)
                .addPathPatterns("/api/zako/v1/userdata")
                .addPathPatterns("/api/zako/v1/userinfo")
                .addPathPatterns("/api/zako/v1/bma")
                .addPathPatterns("/api/zako/v1/cl")
                .addPathPatterns("/api/zako/v1/user/devices")
                .addPathPatterns("/api/yggdrasil/open/account")
                .addPathPatterns("/api/zako/v1/user/violation/history")
                .addPathPatterns("/api/v3/zako/administration/validate")
                .addPathPatterns("/api/zako/v1/user/2fa/open2fa")
                .addPathPatterns("/api/zako/v1/user/2fa/close2fa")
                .addPathPatterns("/api/yggdrasil/textures/**")
                .addPathPatterns("/api/zako/v3/**");
    }

//    @Override
//    public void configureApiVersioning(@NonNull ApiVersionConfigurer configurer) {
//        configurer.useRequestHeader("API-Version");
//         configurer.useQueryParameter("version");
//         configurer.usePathPattern("/api/{version}");
//         configurer.useMediaType("application/vnd.api.v1+json");
//    }

    @PostConstruct
    private void a(){

    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

}

