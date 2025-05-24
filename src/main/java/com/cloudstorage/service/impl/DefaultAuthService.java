package com.cloudstorage.service.impl;

import com.cloudstorage.controller.payload.UserPayload;
import com.cloudstorage.controller.payload.UsernamePayload;
import com.cloudstorage.entity.User;
import com.cloudstorage.repository.UserRepository;
import com.cloudstorage.service.api.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DefaultAuthService implements AuthService {

    private final DaoAuthenticationProvider authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRepository sessionRepository;

    @Override
    public void loginUser(UserPayload userPayload) {

        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                userPayload.username(), userPayload.password());

        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(authRequest);
        } catch (UsernameNotFoundException exception) {
            throw new BadCredentialsException("users.errors.not_found");
        } catch (BadCredentialsException exception) {
            throw new BadCredentialsException("users.errors.invalid_password");
        }


        SecurityContextHolder.getContext().setAuthentication(authResult);

        // Сохраняем контекст через репозиторий
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

        securityContextRepository.saveContext(
                SecurityContextHolder.getContext(),
                request,
                response
        );
    }

    @Override
    public UsernamePayload getUsernameFromSession(String encodedSessionId) {
        Session session = sessionRepository.findById(this.decodeSessionId(encodedSessionId));
        SecurityContextImpl securityContext = session.getAttribute("SPRING_SECURITY_CONTEXT");
        Authentication auth = securityContext.getAuthentication();
        return new UsernamePayload(auth.getName());
    }

    private String decodeSessionId(String encodedString) {
        return new String(Base64.getDecoder().decode(encodedString));
    }
}
