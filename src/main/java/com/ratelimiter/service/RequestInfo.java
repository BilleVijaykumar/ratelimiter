package com.ratelimiter.service;

public class RequestInfo {
    public double tokens;
    public long lastRefillTimestamp;

    public RequestInfo(double tokens, long lastRefillTimestamp) {
        this.tokens = tokens;
        this.lastRefillTimestamp = lastRefillTimestamp;
    }

    public double getTokens() {
        return tokens;
    }

    public void setTokens(double tokens) {
        this.tokens = tokens;
    }

    public long getLastRefillTimestamp() {
        return lastRefillTimestamp;
    }

    public void setLastRefillTimestamp(long lastRefillTimestamp) {
        this.lastRefillTimestamp = lastRefillTimestamp;
    }
}
