package com.zst.zsearch.streaming.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOptionsTest {

    @Test
    void appliesDefaults() {
        JobOptions options = JobOptions.fromArgs(new String[0]);

        assertThat(options.getKafkaBootstrapServers()).isEqualTo("8.163.135.98:9092");
        assertThat(options.getTopic()).isEqualTo("search-topic");
        assertThat(options.getGroupId()).isEqualTo("zsearch-ingest-v1");
        assertThat(options.getDlqTopic()).isEqualTo("zsearch.raw.article.dlq");
        assertThat(options.getEsIndex()).isEqualTo("zsearch_article");
        assertThat(options.getPipelineVersion()).isEqualTo(1);
        assertThat(options.getSummaryLength()).isEqualTo(120);
        assertThat(options.getDedupTtlDays()).isEqualTo(30);
        assertThat(options.getFromTimestamp()).isNull();
        assertThat(options.getToTimestamp()).isNull();
    }

    @Test
    void parsesOverrides() {
        JobOptions options = JobOptions.fromArgs(new String[]{
                "--kafka.group.id", "zsearch-replay-20261121",
                "--es.index", "zsearch_article_rebuild_20261121",
                "--pipeline.version", "2",
                "--from.timestamp", "1000",
                "--to.timestamp", "2000",
                "--checkpoint.dir", "file:///data/flink/checkpoints"
        });

        assertThat(options.getGroupId()).isEqualTo("zsearch-replay-20261121");
        assertThat(options.getEsIndex()).isEqualTo("zsearch_article_rebuild_20261121");
        assertThat(options.getPipelineVersion()).isEqualTo(2);
        assertThat(options.getFromTimestamp()).isEqualTo(1000L);
        assertThat(options.getToTimestamp()).isEqualTo(2000L);
        assertThat(options.getCheckpointDir()).isEqualTo("file:///data/flink/checkpoints");
    }

    @Test
    void rejectsInvertedReplayWindow() {
        assertThatThrownBy(() -> JobOptions.fromArgs(new String[]{
                "--from.timestamp", "2000",
                "--to.timestamp", "1000"
        })).isInstanceOf(IllegalArgumentException.class);
    }
}
