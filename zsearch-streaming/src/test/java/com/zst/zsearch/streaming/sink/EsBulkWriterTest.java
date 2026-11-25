package com.zst.zsearch.streaming.sink;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EsBulkWriterTest {

    @Test
    void retriesOnlyTransientStatuses() {
        assertThat(EsBulkWriter.isRetryableStatus(429)).isTrue();
        assertThat(EsBulkWriter.isRetryableStatus(502)).isTrue();
        assertThat(EsBulkWriter.isRetryableStatus(503)).isTrue();

        assertThat(EsBulkWriter.isRetryableStatus(400)).isFalse();
        assertThat(EsBulkWriter.isRetryableStatus(404)).isFalse();
        assertThat(EsBulkWriter.isRetryableStatus(409)).isFalse();
    }
}
