package com.gymplanner.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    static final int CAPACITY = 5;
    static final long MAXIMUM_BUCKETS = 10_000;
    static final Duration BUCKET_EXPIRY = Duration.ofMinutes(15);
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private final Cache<String, Bucket> buckets;

    public LoginRateLimiter() {
        this(Ticker.systemTicker());
    }

    LoginRateLimiter(Ticker ticker) {
        buckets = Caffeine.newBuilder()
                .maximumSize(MAXIMUM_BUCKETS)
                .expireAfterAccess(BUCKET_EXPIRY)
                .ticker(ticker)
                .build();
    }

    public Decision tryAcquire(String clientIp) {
        Bucket bucket = buckets.get(clientIp, ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return Decision.permit();
        }

        long retryAfterSeconds = Math.max(
                1,
                (probe.getNanosToWaitForRefill() + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
        return Decision.deny(retryAfterSeconds);
    }

    long bucketCount() {
        buckets.cleanUp();
        return buckets.estimatedSize();
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(CAPACITY)
                        .refillGreedy(CAPACITY, REFILL_PERIOD))
                .build();
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {

        static Decision permit() {
            return new Decision(true, 0);
        }

        static Decision deny(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }
}
