package recruitment.dev.applicationservice.client;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class UserTokenFeignConfig {

    @Bean
    RequestInterceptor userTokenRelay() {
        return template -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                template.header("Authorization", "Bearer " + jwt.getTokenValue());
            }
        };
    }
}
