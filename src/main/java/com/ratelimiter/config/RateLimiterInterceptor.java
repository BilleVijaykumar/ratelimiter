package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterInterceptor.class);

    @Autowired
    private RateLimiterService rateLimiterService;

    // Switch this flag to quickly test the other algorithm
    private static final boolean USE_TOKEN_BUCKET = true;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userId = request.getHeader("User-Id");
        if (userId == null || userId.isEmpty()) {
            userId = request.getRemoteAddr(); // Fallback to IP matching
        }
        
        boolean isAllowed;
        if (USE_TOKEN_BUCKET) {
            isAllowed = rateLimiterService.allowRequestTokenBucket(userId);
        } else {
            isAllowed = rateLimiterService.allowRequestSlidingWindow(userId);
        }

        if (!isAllowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too Many Requests");
            log.warn("Rate limited request from user: {}", userId);
            
            // Add metrics/headers context 
            response.setHeader("X-RateLimit-Error", "Limit Exceeded");
            response.setHeader("Retry-After", "1");
            return false;
        }

        return true;
    }
}
