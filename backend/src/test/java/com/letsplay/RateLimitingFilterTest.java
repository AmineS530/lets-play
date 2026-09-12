package com.letsplay;

import com.letsplay.security.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    @Test
    void testBypassWhenTestProfileActive() throws ServletException, IOException {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        RateLimitingFilter filter = new RateLimitingFilter(env);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testRateLimitingEnforcedWhenNotTestProfile() throws ServletException, IOException {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        RateLimitingFilter filter = new RateLimitingFilter(env);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50");
        FilterChain filterChain = mock(FilterChain.class);

        // Consume 100 tokens
        for (int i = 0; i < 100; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(request, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 101st request should be rate-limited to 429
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, filterChain);

        assertEquals(429, blockedResponse.getStatus());
    }
}
