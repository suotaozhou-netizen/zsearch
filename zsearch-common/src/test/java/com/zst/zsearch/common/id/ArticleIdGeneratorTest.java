package com.zst.zsearch.common.id;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleIdGeneratorTest {

    /**
     * 黄金向量：Python 采集侧与 Java 侧必须产出同一个 id。
     * 这个值一旦变了，说明契约被破坏，需要全量重建索引。
     */
    @Test
    void generatesMd5FromTrimmedUrl() {
        assertThat(ArticleIdGenerator.fromUrl(" https://example.com/article/1 "))
                .isEqualTo("b04d24a4a3296bd9a9072594c717bfe6");
    }

    @Test
    void isStableForSameUrl() {
        String url = "https://www.xiaohongshu.com/explore/abc123";
        assertThat(ArticleIdGenerator.fromUrl(url)).isEqualTo(ArticleIdGenerator.fromUrl(url));
    }

    @Test
    void rejectsBlankUrl() {
        assertThatThrownBy(() -> ArticleIdGenerator.fromUrl("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void rejectsNullUrl() {
        assertThatThrownBy(() -> ArticleIdGenerator.fromUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }
}
