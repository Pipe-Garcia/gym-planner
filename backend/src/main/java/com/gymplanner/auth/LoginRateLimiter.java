package com.gymplanner.auth;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Decision tryAcquire(String clientIp) {
        Bucket bucket = buckets.computeIfAbsent(clientIp, ignored -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return Decision.permit();
        }

        long retryAfterSeconds = Math.max(
                1,
                (probe.getNanosToWaitForRefill() + NANOS_PER_SECOND - 1) / NANOS_PER_SECOND);
        return Decision.deny(retryAfterSeconds);
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
