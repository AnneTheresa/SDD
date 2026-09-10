package com.group1.banking.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger("API_AUDIT");
    private static final Logger errorLogger = LoggerFactory.getLogger("ERROR_LOGGER");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        filterChain.doFilter(request, response);

        long duration = System.currentTimeMillis() - start;
        int status = response.getStatus();

        String fullUri = (query == null || query.isBlank()) ? uri : uri + "?" + query;

        if (status >= 200 && status < 300) {
            auditLogger.info("method={}, uri={}, status={}, durationMs={}",
                    method, fullUri, status, duration);
        } else {
            errorLogger.error("method={}, uri={}, status={}, durationMs={}",
                    method, fullUri, status, duration);
        }
    }
}