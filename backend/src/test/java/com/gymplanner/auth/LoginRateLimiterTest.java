package com.gymplanner.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void sixthAttemptForSameKeyIsDenied() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        for (int attempt = 0; attempt < LoginRateLimiter.CAPACITY; attempt++) {
            assertThat(limiter.tryAcquire("203.0.113.21").allowed()).isTrue();
        }

        assertThat(limiter.tryAcquire("203.0.113.21").allowed()).isFalse();
    }

    @Test
    void differentKeysUseDifferentBuckets() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        for (int attempt = 0; attempt < LoginRateLimiter.CAPACITY; attempt++) {
            limiter.tryAcquire("203.0.113.22");
        }

        assertThat(limiter.tryAcquire("203.0.113.22").allowed()).isFalse();
        assertThat(limiter.tryAcquire("198.51.100.22").allowed()).isTrue();
    }

    @Test
    void inactiveBucketExpires() {
        MutableTicker ticker = new MutableTicker();
        LoginRateLimiter limiter = new LoginRateLimiter(ticker);
        limiter.tryAcquire("203.0.113.23");
        assertThat(limiter.bucketCount()).isEqualTo(1);

        ticker.advance(LoginRateLimiter.BUCKET_EXPIRY.plusSeconds(1));

        assertThat(limiter.bucketCount()).isZero();
    }

    @Test
    void bucketCacheDoesNotExceedMaximumSize() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        for (int index = 0; index <= LoginRateLimiter.MAXIMUM_BUCKETS; index++) {
            limiter.tryAcquire("client-" + index);
        }

        assertThat(limiter.bucketCount()).isLessThanOrEqualTo(LoginRateLimiter.MAXIMUM_BUCKETS);
    }

    private static final class MutableTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        private void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
