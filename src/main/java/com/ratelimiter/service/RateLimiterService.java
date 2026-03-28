package com.ratelimiter.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    public int bucketCapacity = 5;
    public int refillRate = 5; // number of tokens added per window
    public int refillWindowTime = 60000; // time window in milliseconds (60 seconds)
    
    // Concurrent map for thread-safe access to user buckets
    private Map<String, RequestInfo> requestMap = new ConcurrentHashMap<>();

    public boolean allowed(String userId) {
        long currentTime = System.currentTimeMillis();
        
        // Initialize bucket with full capacity for a new user
        requestMap.putIfAbsent(userId, new RequestInfo(bucketCapacity, currentTime));
        RequestInfo requestInfo = requestMap.get(userId);
        
        synchronized (requestInfo) {
            long timeElapsed = currentTime - requestInfo.lastRefillTimestamp;
            // Calculate how many tokens to add based on time elapsed
            double tokensToAdd = timeElapsed * ((double) refillRate / refillWindowTime);
            
            // Add tokens and cap at maximum bucket capacity
            requestInfo.tokens = Math.min(bucketCapacity, requestInfo.tokens + tokensToAdd);
            requestInfo.lastRefillTimestamp = currentTime;
            
            // Check if there's at least one token available
            if (requestInfo.tokens >= 1.0) {
                requestInfo.tokens -= 1.0;
                return true;
            } else {
                return false;
            }
        }
    }
}
