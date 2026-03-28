package com.ratelimiter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterController.class);

    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint(HttpServletRequest request) {
        String userId = request.getHeader("User-Id");
        if (userId == null) {
            userId = request.getRemoteAddr();
        }
        log.info("Handling request for user: {}", userId);
        return ResponseEntity.ok("Success! Request allowed for user: " + userId);
    }
}
