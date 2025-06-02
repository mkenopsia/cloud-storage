package com.cloudstorage.config.filter;

import com.cloudstorage.config.UserValidationProperties;
import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.exceptions.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class RestLoginFilter extends AbstractAuthenticationProcessingFilter {

    private final UserValidationProperties userValidationProperties;
    private final MessageSource messageSource;
    private final SecurityContextRepository securityContextRepository;

    public RestLoginFilter(String loginPath, AuthenticationManager authenticationManager, UserValidationProperties userValidationProperties, MessageSource messageSource, SecurityContextRepository securityContextRepository) {
        super(loginPath);
        setAuthenticationManager(authenticationManager);
        this.userValidationProperties = userValidationProperties;
        this.messageSource = messageSource;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        try {
            UserPayload userPayload = new ObjectMapper().readValue(request.getInputStream(), UserPayload.class);

            if (userPayload.username().isEmpty()) {
                throw new ValidationException(
                        "users.errors.blank_username"
                );
            }
            if (userPayload.password().isEmpty()) {
                throw new ValidationException(
                        "users.errors.blank_password"
                );
            }
            if (userPayload.username().trim().length() < userValidationProperties.getMinUsernameLength()) {
                throw new ValidationException(
                        "users.errors.invalid_username_size_less"
                );
            }
            if (userPayload.username().trim().length() > userValidationProperties.getMaxUsernameLength()) {
                throw new ValidationException(
                        "users.errors.invalid_username_size_more"
                );
            }
            if (userPayload.password().trim().length() < userValidationProperties.getMinPasswordLength()) {
                throw new ValidationException(
                        "users.errors.invalid_password_size_less"
                );
            }
            if (userPayload.password().trim().length() > userValidationProperties.getMaxPasswordLength()) {
                throw new ValidationException(
                        "users.errors.invalid_password_size_more"
                );
            }
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(userPayload.username(), userPayload.password()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.getWriter().write(
                new ObjectMapper().writeValueAsString(
                        new UsernamePayload(authResult.getName())
                )
        );
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        String message = "Error";
        if (failed instanceof BadCredentialsException || failed instanceof UsernameNotFoundException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = this.messageSource.getMessage(failed.getMessage(), null, "wrong username or password", Locale.getDefault());
        } else if (failed instanceof ValidationException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            message = this.messageSource.getMessage(failed.getMessage(), null, "invalid data", Locale.getDefault());
        } else {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            message = "Unexpected error";
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of("message", message)));
    }
}
