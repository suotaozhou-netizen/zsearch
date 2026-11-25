package com.zst.zsearch.streaming.validate;

import com.zst.zsearch.common.id.ArticleIdGenerator;
import com.zst.zsearch.common.model.IngestEnvelope;
import com.zst.zsearch.common.model.RawArticle;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleValidatorTest {

    @Test
    void acceptsValidEnvelope() {
        assertThat(ArticleValidator.validate(validEnvelope())).isNull();
    }

    @Test
    void rejectsNullEnvelope() {
        assertThat(ArticleValidator.validate(null).type()).isEqualTo(ArticleValidator.SCHEMA_ERROR);
    }

    @Test
    void rejectsUnsupportedSchemaVersion() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setSchemaVersion(2);

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.SCHEMA_ERROR);
    }

    @Test
    void rejectsUnsupportedIdVersion() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setIdVersion(2);

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.SCHEMA_ERROR);
    }

    @Test
    void rejectsMalformedArticleId() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setArticleId("NOT-A-HASH");

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.ID_INVALID);
    }

    @Test
    void rejectsNonPositiveRawVersion() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setRawVersion(0L);

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.VERSION_INVALID);
    }

    @Test
    void rejectsMissingArticle() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setArticle(null);

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.ARTICLE_EMPTY);
    }

    @Test
    void rejectsBlankUrl() {
        IngestEnvelope envelope = validEnvelope();
        envelope.getArticle().setUrl("  ");

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.URL_EMPTY);
    }

    @Test
    void rejectsEmptyTitleAndText() {
        IngestEnvelope envelope = validEnvelope();
        envelope.getArticle().setTitle(null);
        envelope.getArticle().setText(null);

        assertThat(ArticleValidator.validate(envelope).type()).isEqualTo(ArticleValidator.CONTENT_EMPTY);
    }

    @Test
    void rejectsUnknownSiteId() {
        IngestEnvelope envelope = validEnvelope();
        envelope.getArticle().setSiteId("99");

        ArticleValidator.Error error = ArticleValidator.validate(envelope);

        assertThat(error.type()).isEqualTo(ArticleValidator.SITE_INVALID);
    }

    @Test
    void rejectsArticleIdMismatch() {
        IngestEnvelope envelope = validEnvelope();
        envelope.setArticleId(ArticleIdGenerator.fromUrl("https://example.com/other"));

        ArticleValidator.Error error = ArticleValidator.validate(envelope);

        assertThat(error.type()).isEqualTo(ArticleValidator.ID_MISMATCH);
        assertThat(error.message()).contains("expected=");
    }

    static IngestEnvelope validEnvelope() {
        RawArticle article = new RawArticle();
        article.setUrl("https://example.com/a/1");
        article.setTitle("标题");
        article.setSiteId("2");

        IngestEnvelope envelope = new IngestEnvelope();
        envelope.setSchemaVersion(1);
        envelope.setIdVersion(1);
        envelope.setArticleId(ArticleIdGenerator.fromUrl(article.getUrl()));
        envelope.setRawVersion(1710000000000L);
        envelope.setArticle(article);
        return envelope;
    }
}
