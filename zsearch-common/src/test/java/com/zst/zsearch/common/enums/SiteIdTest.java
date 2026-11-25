package com.zst.zsearch.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SiteIdTest {

    @Test
    void mapsCodesToSites() {
        assertThat(SiteId.fromCode(1)).isEqualTo(SiteId.DOUYIN);
        assertThat(SiteId.fromCode(2)).isEqualTo(SiteId.XIAOHONGSHU);
        assertThat(SiteId.fromCode(3)).isEqualTo(SiteId.BILIBILI);
        assertThat(SiteId.fromCode(4)).isEqualTo(SiteId.WEIBO);
        assertThat(SiteId.fromCode(5)).isEqualTo(SiteId.TOUTIAO);
        assertThat(SiteId.fromCode(6)).isEqualTo(SiteId.KUAISHOU);
        assertThat(SiteId.fromCode(7)).isEqualTo(SiteId.TIEBA);
        assertThat(SiteId.fromCode(8)).isEqualTo(SiteId.ZHIHU);
    }

    @Test
    void codesMatchMediaCrawlerPlatforms() {
        assertThat(SiteId.values()).hasSize(8);
        assertThat(SiteId.KUAISHOU.getDescription()).isEqualTo("快手");
        assertThat(SiteId.TIEBA.getDescription()).isEqualTo("贴吧");
        assertThat(SiteId.ZHIHU.getDescription()).isEqualTo("知乎");
    }

    @Test
    void fromCodeOrNullReturnsNullForUnknownCode() {
        assertThat(SiteId.fromCodeOrNull(6)).isEqualTo(SiteId.KUAISHOU);
        assertThat(SiteId.fromCodeOrNull(99)).isNull();
    }

    @Test
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> SiteId.fromCode(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的站点id");
    }
}
