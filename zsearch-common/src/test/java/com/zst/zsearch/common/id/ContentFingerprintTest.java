package com.zst.zsearch.common.id;

import com.zst.zsearch.common.model.RawArticle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContentFingerprintTest {

    private RawArticle sample() {
        RawArticle article = new RawArticle();
        article.setUrl("https://example.com/a/1");
        article.setTitle("标题");
        article.setText("正文");
        article.setName("账号");
        article.setSource("微博");
        article.setSiteId("4");
        article.setUserId("u-1");
        article.setLikeCount(10L);
        article.setImageList(List.of("https://img/1.jpg"));
        return article;
    }

    @Test
    void isStableForSameContent() {
        assertThat(ContentFingerprint.of(sample())).isEqualTo(ContentFingerprint.of(sample()));
    }

    @Test
    void ignoresCrawlerTimeAndRawVersion() {
        RawArticle first = sample();
        first.setCrawlerTime("2026-01-01T00:00:00");
        first.setRawVersion(1L);

        RawArticle second = sample();
        second.setCrawlerTime("2026-05-05T12:00:00");
        second.setRawVersion(2L);

        assertThat(ContentFingerprint.of(first)).isEqualTo(ContentFingerprint.of(second));
    }

    @Test
    void changesWhenTextChanges() {
        RawArticle changed = sample();
        changed.setText("正文（已编辑）");

        assertThat(ContentFingerprint.of(changed)).isNotEqualTo(ContentFingerprint.of(sample()));
    }

    @Test
    void normalizesNullAndEmptyCollections() {
        RawArticle withNulls = sample();
        withNulls.setImageList(null);
        withNulls.setVideoList(null);

        RawArticle withEmpty = sample();
        withEmpty.setImageList(List.of());
        withEmpty.setVideoList(List.of());

        assertThat(ContentFingerprint.of(withNulls)).isEqualTo(ContentFingerprint.of(withEmpty));
    }

    @Test
    void normalizesNullAndBlankStrings() {
        RawArticle withNull = sample();
        withNull.setSummary(null);

        RawArticle withBlank = sample();
        withBlank.setSummary("   ");

        assertThat(ContentFingerprint.of(withNull)).isEqualTo(ContentFingerprint.of(withBlank));
    }
}
