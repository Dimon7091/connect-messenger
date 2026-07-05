package ru.gorbunov.connect.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import ru.gorbunov.connect.core.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,     // ✅ @PreAuthorize, @PostAuthorize
        securedEnabled = true,     // ✅ @Secured
        jsr250Enabled = true       // ✅ @RolesAllowed
)  // Для Spring Security 6+
public class SecurityConfig {

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtBanFilter jwtBanFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector)
            throws Exception {
        // По умолчанию все запрещено
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ===== СТАТИЧЕСКИЕ РЕСУРСЫ (ВАЖНО!) =====
                        .requestMatchers("/", "/index.html").permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/**.js", "/**.css").permitAll()
                        .requestMatchers("/chat-ws/**", "/chat-ws/info/**").permitAll()

                        // ===== ПУБЛИЧНЫЕ API =====
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/login-admin").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()

                        // ===== ВСЕ ОСТАЛЬНОЕ =====
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer((rs) -> rs.jwt((jwt) -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .httpBasic(Customizer.withDefaults())

                // 2. Добавляем фильтр проверки черного списка сразу ПОСЛЕ валидации OAuth2 токена
                .addFilterAfter(jwtBanFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }

    @Bean
    public AuthenticationProvider daoAuthProvider(AuthenticationManagerBuilder auth) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // 1. Указываем, что в токене роли лежат в поле "roles" (или как оно у вас называется в Payload)
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // 2. Добавляем префикс ROLE_, чтобы hasRole('ADMIN') работал
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
