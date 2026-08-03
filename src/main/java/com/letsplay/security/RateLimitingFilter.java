package com.letsplay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.letsplay.exception.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter implements Filter {

    private final Environment environment;
    private final Map<String, RequestCounter> limitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RateLimitingFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        // Bypass rate limiting in test profile to allow rapid API integration tests
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("test")) {
            chain.doFilter(request, response);
            return;
        }

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String ip = httpRequest.getRemoteAddr();
            long currentTime = System.currentTimeMillis();
            
            RequestCounter counter = limitMap.compute(ip, (key, value) -> {
                if (value == null || (currentTime - value.timestamp) > 60000) {
                    return new RequestCounter(currentTime, new AtomicInteger(1));
                } else {
                    value.count.incrementAndGet();
                    return value;
                }
            });

            if (counter.count.get() > MAX_REQUESTS_PER_MINUTE) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .message("Rate limit exceeded. Maximum " + MAX_REQUESTS_PER_MINUTE + " requests per minute.")
                        .path(httpRequest.getRequestURI())
                        .build();

                httpResponse.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }
        
        chain.doFilter(request, response);
    }

    private static class RequestCounter {
        final long timestamp;
        final AtomicInteger count;

        RequestCounter(long timestamp, AtomicInteger count) {
            this.timestamp = timestamp;
            this.count = count;
        }
    }
}
