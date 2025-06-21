package com.cloudstorage.config.filter;

import com.cloudstorage.config.filter.validator.PayloadValidator;
import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.exception.UserCredentialsValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final MessageSource messageSource;
    private final SecurityContextRepository securityContextRepository;
    private final ObjectMapper objectMapper;
    private final PayloadValidator payloadValidator;

    public RestLoginFilter(String loginPath,
                           AuthenticationManager authenticationManager,
                           MessageSource messageSource,
                           SecurityContextRepository securityContextRepository,
                           ObjectMapper objectMapper,
                           PayloadValidator payloadValidator) {
        super(loginPath);
        setAuthenticationManager(authenticationManager);
        this.messageSource = messageSource;
        this.securityContextRepository = securityContextRepository;
        this.objectMapper = objectMapper;
        this.payloadValidator = payloadValidator;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        try {
            UserPayload userPayload = objectMapper.readValue(request.getInputStream(), UserPayload.class);
            payloadValidator.validate(userPayload);

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
                objectMapper.writeValueAsString(
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
        } else if (failed instanceof UserCredentialsValidationException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            System.out.println(failed.getMessage());
            message = this.messageSource.getMessage(failed.getMessage(), null, "invalid data", Locale.getDefault());
        } else {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            message = "Unexpected error";
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Map.of("message", message)));
    }
}
