package com.letsplay.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.letsplay.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, RequestBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public RateLimitingFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        cleanupOldBucketsIfFull();

        String clientIp = getClientIP(request);
        RequestBucket bucket = buckets.computeIfAbsent(clientIp, k -> new RequestBucket(MAX_REQUESTS_PER_MINUTE));

        if (!bucket.tryConsume()) {
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse errorResponse = ErrorResponse.builder()
                    .status(429)
                    .message("Too Many Requests: Rate limit exceeded. Please try again later.")
                    .timestamp(LocalDateTime.now())
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void cleanupOldBucketsIfFull() {
        if (buckets.size() > 5000) {
            long cutoff = System.currentTimeMillis() - 300000; // 5 minutes inactive
            buckets.entrySet().removeIf(entry -> entry.getValue().getLastRefillTimestamp() < cutoff);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "unknown";
        }
        return xfHeader.split(",")[0].trim();
    }

    private static class RequestBucket {
        private final int capacity;
        private int tokens;
        private long lastRefillTimestamp;

        public RequestBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public long getLastRefillTimestamp() {
            return lastRefillTimestamp;
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefillTimestamp > 60000) { // Refill every 60 seconds
                tokens = capacity;
                lastRefillTimestamp = now;
            }
        }
    }
}
