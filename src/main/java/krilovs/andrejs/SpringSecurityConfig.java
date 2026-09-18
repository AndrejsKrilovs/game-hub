package krilovs.andrejs;

import jakarta.servlet.http.HttpServletRequest;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/*/assets/**",
                                "/api/me",
                                "/api/login",
                                "/api/logout",
                                "/favicon.ico"
                        )
                        .permitAll()

                        .requestMatchers("/games/*/start", "/games/*/exit")
                        .authenticated()

                        .requestMatchers("/chess", "/chess/", "/chess/**")
                        .access((authenticationSupplier, context) -> {
                            var request = context.getRequest();
                            var authentication = authenticationSupplier.get();
                            var authenticated = isAuthenticated(authentication);
                            var session = request.getSession(false);

                            var allowed = session != null
                                    && (
                                    Boolean.TRUE.equals(session.getAttribute(CHESS_ALLOWED))
                                            || Boolean.TRUE.equals(session.getAttribute(CHESS_ACTIVE))
                            );

                            var inviteLink = hasGameSessionId(request);
                            return new AuthorizationDecision(authenticated && (allowed || inviteLink));
                        })

                        .anyRequest().permitAll()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (isChessRequest(request)) {
                                redirectToLoginWithReturnUrl(request, response);
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            if (isChessRequest(request)) {
                                response.sendRedirect("/");
                            }
                            else {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            }
                        })
                )

                .build();
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private static boolean hasGameSessionId(HttpServletRequest request) {
        var gameSessionId = request.getParameter("gameSessionId");
        return gameSessionId != null && !gameSessionId.isBlank();
    }

    private static boolean isChessRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/chess");
    }

    private static void redirectToLoginWithReturnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var target = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            target += "?" + request.getQueryString();
        }

        var encodedTarget = URLEncoder.encode(target, StandardCharsets.UTF_8);
        response.sendRedirect("/?redirect=" + encodedTarget);
    }
}