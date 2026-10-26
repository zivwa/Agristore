
package com.zivwa.agristore.security;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.DispatcherType;

@ConditionalOnProperty(name="app.auth-mode", havingValue="firebase")
@Configuration
public class SecurityConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors-origins:http://localhost:5173,http://127.0.0.1:5173}")
            String origins
    ) {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                Arrays.stream(origins.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toList()
        );

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        config.setAllowedHeaders(
                List.of("Authorization", "Content-Type")
        );

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /*
     * PUBLIC API SECURITY
     *
     * Products, categories, health and preflight requests
     * do not require Firebase authentication.
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicApi(HttpSecurity http) throws Exception {

        var publicPaths = new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/health", "GET"),
                new AntPathRequestMatcher("/api/v1/categories", "GET"),
                new AntPathRequestMatcher("/api/v1/categories/**", "GET"),
                new AntPathRequestMatcher("/api/v1/products", "GET"),
                new AntPathRequestMatcher("/api/v1/products/**", "GET"),
                new AntPathRequestMatcher("/**", "OPTIONS")
        );

        return http
                .securityMatcher(publicPaths)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll()
                )
                .build();
    }

    /*
     * PROTECTED API SECURITY
     *
     * Customers must authenticate with Firebase.
     * Admins additionally require the Firebase admin claim.
     */
    @Bean
    @Order(2)
    SecurityFilterChain protectedApi(
            HttpSecurity http,
            ObjectProvider<FirebaseAuth> authProvider
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Let Spring Boot return the original error
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()

                        // Admin access
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        // Customer authentication
                        .requestMatchers(
                                "/api/v1/auth/me",
                                "/api/v1/cart",
                                "/api/v1/cart/**",
                                "/api/v1/addresses",
                                "/api/v1/addresses/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        jsonError(
                                                response,
                                                401,
                                                "UNAUTHORIZED"
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        jsonError(
                                                response,
                                                403,
                                                "FORBIDDEN"
                                        )
                        )
                )

                .addFilterBefore(
                        new FirebaseTokenFilter(authProvider),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class
                )
                .build();

    }

    private static void jsonError(
            HttpServletResponse response,
            int status,
            String error
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"code\":\"" + error + "\"}"
        );
    }

    /*
     * FIREBASE TOKEN AUTHENTICATION FILTER
     *
     * The Firebase verification try/catch must not contain
     * chain.doFilter(). Otherwise a downstream cart error
     * can be incorrectly reported as INVALID_TOKEN.
     */
    private static final class FirebaseTokenFilter
            extends OncePerRequestFilter {

        private final ObjectProvider<FirebaseAuth> authProvider;

        private FirebaseTokenFilter(
                ObjectProvider<FirebaseAuth> authProvider
        ) {
            this.authProvider = authProvider;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain chain
        ) throws IOException, ServletException {

            String authorization =
                    request.getHeader("Authorization");

            // Spring Security decides whether an unauthenticated
            // request is permitted.
            if (authorization == null || authorization.isBlank()) {
                chain.doFilter(request, response);
                return;
            }

            if (!authorization.startsWith("Bearer ")
                    || authorization.substring(7).isBlank()) {

                jsonError(response, 401, "INVALID_TOKEN");
                return;
            }

            FirebaseAuth firebaseAuth =
                    authProvider.getIfAvailable();

            if (firebaseAuth == null) {
                jsonError(
                        response,
                        503,
                        "FIREBASE_AUTH_NOT_CONFIGURED"
                );
                return;
            }

            FirebaseToken token;

            // Catch ONLY Firebase token-verification errors.
            try {
                token = firebaseAuth.verifyIdToken(
                        authorization.substring(7).trim(),
                        true
                );

            } catch (FirebaseAuthException | IllegalArgumentException e) {

                SecurityContextHolder.clearContext();
                logger.warn("Firebase ID token verification failed");

                jsonError(response, 401, "INVALID_TOKEN");
                return;
            }

            boolean admin = Boolean.TRUE.equals(
                    token.getClaims().get("admin")
            );

            var authorities = admin
                    ? List.of(
                    new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            )
                    : List.of(
                    new SimpleGrantedAuthority("ROLE_CUSTOMER")
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            token.getUid(),
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            // Important: outside the token-verification catch.
            chain.doFilter(request, response);
        }
    }
}
