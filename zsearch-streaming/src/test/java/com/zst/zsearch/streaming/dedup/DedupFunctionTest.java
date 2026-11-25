package com.zst.zsearch.streaming.dedup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DedupFunctionTest {

    @Test
    void passesWhenNoState() {
        assertThat(DedupFunction.decide(null, 100L, "hash-a"))
                .isEqualTo(DedupFunction.Decision.PASS);
    }

    @Test
    void dropsStaleOrDuplicateVersion() {
        assertThat(DedupFunction.decide("hash-a|100", 100L, "hash-b"))
                .isEqualTo(DedupFunction.Decision.DROP_STALE_VERSION);
        assertThat(DedupFunction.decide("hash-a|100", 99L, "hash-b"))
                .isEqualTo(DedupFunction.Decision.DROP_STALE_VERSION);
    }

    @Test
    void dropsSameContentWithNewerVersion() {
        assertThat(DedupFunction.decide("hash-a|100", 101L, "hash-a"))
                .isEqualTo(DedupFunction.Decision.DROP_SAME_CONTENT);
    }

    @Test
    void passesChangedContentWithNewerVersion() {
        assertThat(DedupFunction.decide("hash-a|100", 101L, "hash-b"))
                .isEqualTo(DedupFunction.Decision.PASS);
    }

    @Test
    void passesOnMalformedState() {
        assertThat(DedupFunction.decide("oops", 101L, "hash-b"))
                .isEqualTo(DedupFunction.Decision.PASS);
        assertThat(DedupFunction.decide("hash-a|not-a-number", 101L, "hash-b"))
                .isEqualTo(DedupFunction.Decision.PASS);
    }
}
