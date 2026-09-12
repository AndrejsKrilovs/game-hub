package krilovs.andrejs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SpringSecurityConfig {

    public static final String CHESS_ALLOWED = "CHESS_ALLOWED";
    public static final String CHESS_ACTIVE = "CHESS_ACTIVE";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/*/assets/**",
                                "/games/*/start",
                                "/games/*/exit",
                                "/favicon.ico"
                        ).permitAll()

                        .requestMatchers(
                                "/chess",
                                "/chess/",
                                "/chess/**"
                        ).access((authentication, context) -> {
                            HttpServletRequest request = context.getRequest();
                            HttpSession session = request.getSession(false);

                            boolean allowed = session != null
                                    && (
                                    Boolean.TRUE.equals(session.getAttribute(CHESS_ALLOWED))
                                            || Boolean.TRUE.equals(session.getAttribute(CHESS_ACTIVE))
                            );

                            return new AuthorizationDecision(allowed);
                        })

                        .anyRequest().permitAll()
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/")
                        )
                )

                .build();
    }
}