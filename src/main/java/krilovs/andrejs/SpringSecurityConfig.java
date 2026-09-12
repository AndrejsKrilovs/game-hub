package krilovs.andrejs;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SpringSecurityConfig {

    public static final String CHESS_ALLOWED = "CHESS_ALLOWED";
    public static final String CHESS_ACTIVE = "CHESS_ACTIVE";

    @Bean
    AuthenticationProvider customAuthenticationProvider() {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                var username = authentication.getName();
                if (username == null || username.trim().isEmpty()) {
                    throw new BadCredentialsException("Имя не может быть пустым");
                }

                return new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_PLAYER"))
                );
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((req, res, exc) -> res.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                        .permitAll()
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**", "/*/assets/**", "/api/me", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/games/*/start", "/games/*/exit")
                        .authenticated()

                        .requestMatchers("/chess", "/chess/", "/chess/**").access((authentication, context) -> {
                            var request = context.getRequest();
                            var session = request.getSession(false);

                            var allowed = session != null
                                    && (
                                    Boolean.TRUE.equals(session.getAttribute(CHESS_ALLOWED))
                                            || Boolean.TRUE.equals(session.getAttribute(CHESS_ACTIVE))
                            );

                            return new AuthorizationDecision(allowed);
                        })

                        .anyRequest().permitAll()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/chess")) {
                                response.sendRedirect("/");
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (request.getRequestURI().startsWith("/chess")) {
                                response.sendRedirect("/");
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            }
                        })
                )

                .build();
    }
}