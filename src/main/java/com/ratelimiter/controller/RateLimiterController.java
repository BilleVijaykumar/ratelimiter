package com.ratelimiter.controller;

import com.ratelimiter.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimiterController {

    @Autowired
    private RateLimiterService rateLimiterService;

    @GetMapping("/resource")
    public ResponseEntity<String> accessResource(@RequestHeader(value = "User-Id", defaultValue = "defaultUser") String userId) {
        if (rateLimiterService.allowed(userId)) {
            return ResponseEntity.ok("Request successful! Resource accessed.");
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests! Rate limit exceeded.");
        }
    }
}
