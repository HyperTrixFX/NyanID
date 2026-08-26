package moe.koseirin.nyanruaineo.Minecraft.config;

/*
 * @author KoseiRin_
 * awa
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/*
 * @author KoseiRin_
 * awa
 */
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
