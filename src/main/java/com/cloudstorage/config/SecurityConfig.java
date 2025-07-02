package com.cloudstorage.config;

import com.cloudstorage.config.filter.RestLoginFilter;
import com.cloudstorage.config.filter.validator.PayloadValidator;
import com.cloudstorage.config.handler.RestAuthenticationEntryPoint;
import com.cloudstorage.service.SecurityUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableRedisHttpSession
public class SecurityConfig {

    private final AuthenticationEntryPoint authEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager, MessageSource messageSource, SecurityContextRepository securityContextRepository, ObjectMapper objectMapper, PayloadValidator payloadValidator) throws Exception {
        RestLoginFilter restLoginFilter = new RestLoginFilter(
                "/api/auth/sign-in",
                authenticationManager,
                messageSource,
                securityContextRepository,
                objectMapper,
                payloadValidator);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/auth/sign-up", "/api/auth/sign-in").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(restLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer ->
                        httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(authEntryPoint))
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(((request, response, authentication) -> {
                            response.setStatus(204);
                        })))
                .addFilterAfter((req, res, chain) -> {
//                    System.out.println("Current auth: " + SecurityContextHolder.getContext().getAuthentication());
                    chain.doFilter(req, res);
                }, AuthorizationFilter.class)
                .build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(SecurityUserDetailsService securityUserDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setHideUserNotFoundExceptions(false);
        provider.setUserDetailsService(securityUserDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
