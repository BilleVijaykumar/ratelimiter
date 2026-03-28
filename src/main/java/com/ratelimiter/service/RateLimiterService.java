package com.ratelimiter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int LIMIT = 60; // 60 requests
    private static final int WINDOW_IN_SECONDS = 60; // per minute

    // Lua script for Sliding Window Log Algorithm
    private static final String SLIDING_WINDOW_LUA =
            "local key = KEYS[1]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local window_start = tonumber(ARGV[2])\n" +
            "local limit = tonumber(ARGV[3])\n" +
            "local member = ARGV[4]\n" +
            "local ttl = tonumber(ARGV[5])\n" +
            
            "redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)\n" +
            "local current_count = redis.call('ZCARD', key)\n" +
            
            "if current_count < limit then\n" +
            "    redis.call('ZADD', key, now, member)\n" +
            "    redis.call('EXPIRE', key, ttl)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    public boolean allowRequestSlidingWindow(String userId) {
        String key = "rate_limit:sliding_window:" + userId;
        long currentTimeMillis = Instant.now().toEpochMilli();
        long windowStartMillis = currentTimeMillis - (WINDOW_IN_SECONDS * 1000L);
        String member = currentTimeMillis + "-" + UUID.randomUUID().toString();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(SLIDING_WINDOW_LUA);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(currentTimeMillis),
                String.valueOf(windowStartMillis),
                String.valueOf(LIMIT),
                member,
                String.valueOf(WINDOW_IN_SECONDS)
        );

        boolean allowed = result != null && result == 1L;
        if (allowed) {
            log.info("Sliding Window: Request allowed for user {}", userId);
        } else {
            log.warn("Sliding Window: Rate limit exceeded for user {}", userId);
        }
        return allowed;
    }

    // Lua script for Token Bucket Algorithm
    private static final String TOKEN_BUCKET_LUA =
            "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refill_rate = tonumber(ARGV[2])\n" + // tokens per second
            "local now = tonumber(ARGV[3])\n" +
            "local ttl = tonumber(ARGV[4])\n" +
            
            "local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')\n" +
            "local tokens = tonumber(bucket[1])\n" +
            "local last_refill = tonumber(bucket[2])\n" +
            
            "if not tokens then\n" +
            "    tokens = capacity\n" +
            "    last_refill = now\n" +
            "else\n" +
            "    local elapsed_seconds = math.max(0, now - last_refill)\n" +
            "    local refill = math.floor(elapsed_seconds * refill_rate)\n" +
            "    if refill > 0 then\n" +
            "       tokens = math.min(capacity, tokens + refill)\n" +
            "       last_refill = now\n" +
            "    end\n" +
            "end\n" +
            
            "if tokens >= 1 then\n" +
            "    tokens = tokens - 1\n" +
            "    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)\n" +
            "    redis.call('EXPIRE', key, ttl)\n" +
            "    return 1\n" +
            "else\n" +
            "    redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)\n" +
            "    redis.call('EXPIRE', key, ttl)\n" +
            "    return 0\n" +
            "end";

    public boolean allowRequestTokenBucket(String userId) {
        String key = "rate_limit:token_bucket:" + userId;
        long nowSeconds = Instant.now().getEpochSecond();
        int refillRate = 1; // 1 token per second (which is 60 tokens per minute)
        int ttl = LIMIT / refillRate; // TTL is exactly how long it takes to fully refill the bucket

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(TOKEN_BUCKET_LUA);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(LIMIT),
                String.valueOf(refillRate),
                String.valueOf(nowSeconds),
                String.valueOf(ttl)
        );

        boolean allowed = result != null && result == 1L;
        if (allowed) {
            log.info("Token Bucket: Request allowed for user {}", userId);
        } else {
             log.warn("Token Bucket: Rate limit exceeded for user {}", userId);
        }
        return allowed;
    }
}
