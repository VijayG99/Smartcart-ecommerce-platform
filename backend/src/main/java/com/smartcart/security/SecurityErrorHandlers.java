package com.smartcart.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/** Security filters run before Spring MVC, so @RestControllerAdvice cannot see their errors. We write the JSON ourselves. */
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        Object jwtError = req.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTR);
        write(req, res, HttpStatus.UNAUTHORIZED, jwtError != null ? jwtError.toString() : "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        write(req, res, HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    private void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getWriter(),
                new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI(), null));
    }
}
