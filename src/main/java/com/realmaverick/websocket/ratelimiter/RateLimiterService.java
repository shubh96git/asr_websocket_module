package com.realmaverick.websocket.ratelimiter;

import io.github.bucket4j.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String username) {
        return buckets.computeIfAbsent(username, key ->
                Bucket4j.builder()
                        .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)))) // 100 requests/min
                        .build()
        );
    }
}