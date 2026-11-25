package com.zst.zsearch.streaming.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zst.zsearch.common.model.RawArticle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectorArticleMapperTest {

    private static final String BILI_EVENT = """
            {
              "eventId": "bili:content:117178586240848",
              "platform": "bili",
              "dataType": "content",
              "crawlerType": "search",
              "sourceKeyword": "AI",
              "crawledAt": 1795279742029,
              "payload": {
                "video_id": "117178586240848",
                "video_type": "video",
                "title": "AI workflow",
                "desc": "",
                "create_time": 1788003504,
                "creator_hash": "8d8e7a39c35f792c",
                "nickname": "tester",
                "liked_count": "15238",
                "video_favorite_count": "38158",
                "video_share_count": "3148",
                "video_comment": "1344",
                "last_modify_ts": 1795279742022,
                "video_url": "https://www.bilibili.com/video/av117178586240848",
                "video_cover_url": "http://i0.hdslb.com/bfs/archive/cover.jpg"
              }
            }
            """;

    @Test
    void mapsBiliContentEvent() {
        JSONObject root = JSON.parseObject(BILI_EVENT);

        CollectorArticleMapper.Mapped mapped = CollectorArticleMapper.map(
                root.getString("platform"),
                root.getJSONObject("payload"),
                root.getLong("crawledAt"),
                0L);

        RawArticle article = mapped.article();
        assertThat(article.getUrl()).isEqualTo("https://www.bilibili.com/video/av117178586240848");
        assertThat(article.getTitle()).isEqualTo("AI workflow");
        assertThat(article.getText()).isEmpty();
        assertThat(article.getName()).isEqualTo("tester");
        assertThat(article.getSource()).isEqualTo("bilibili");
        assertThat(article.getSiteId()).isEqualTo("3");
        assertThat(article.getUserId()).isEqualTo("8d8e7a39c35f792c");
        assertThat(article.getLikeCount()).isEqualTo(15238L);
        assertThat(article.getCommentCount()).isEqualTo(1344L);
        assertThat(article.getCollectCount()).isEqualTo(38158L);
        assertThat(article.getRepostCount()).isEqualTo(3148L);
        assertThat(article.getImageList()).containsExactly("http://i0.hdslb.com/bfs/archive/cover.jpg");
        assertThat(article.getCreatedAt()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(1788003504), ZoneId.of("Asia/Shanghai")).toString());
        assertThat(article.getCrawlerTime()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(1795279742029L), ZoneId.of("Asia/Shanghai")).toString());
        assertThat(mapped.rawVersion()).isEqualTo(1795279742022L);
        assertThat(mapped.crawledAt()).isEqualTo(article.getCrawlerTime());
    }

    @Test
    void fallsBackToCrawledAtAndKafkaTimestamp() {
        JSONObject payload = JSON.parseObject("""
                {"title": "无时间字段", "video_url": "https://www.bilibili.com/video/av1"}
                """);

        CollectorArticleMapper.Mapped mapped =
                CollectorArticleMapper.map("bili", payload, null, 1710000000000L);

        assertThat(mapped.rawVersion()).isEqualTo(1710000000000L);
        assertThat(mapped.crawledAt()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(1710000000000L), ZoneId.of("Asia/Shanghai")).toString());
    }

    @Test
    void mapsDouyinContentEvent() {
        JSONObject root = JSON.parseObject("""
                {
                  "eventId": "dy:content:7710625308772158729",
                  "platform": "dy",
                  "dataType": "content",
                  "crawlerType": "search",
                  "sourceKeyword": "编程副业",
                  "crawledAt": 1795364415377,
                  "payload": {
                    "aweme_id": "7710625308772158729",
                    "aweme_type": "0",
                    "title": "0代码自建小程序赚了200块",
                    "desc": "0代码自建小程序赚了200块 #豆包",
                    "create_time": 1795320019,
                    "creator_hash": "5e1594828b4bf909",
                    "nickname": "程序员豆腐",
                    "liked_count": "564",
                    "collected_count": "96",
                    "comment_count": "7",
                    "share_count": "28",
                    "last_modify_ts": 1795364415351,
                    "aweme_url": "https://www.douyin.com/video/7710625308772158729",
                    "cover_url": "https://p9-pc-sign.douyinpic.com/obj/cover.jpg",
                    "video_download_url": "https://www.douyin.com/aweme/v1/play/?video_id=v0300",
                    "music_download_url": "https://sf11-cdn-tos.douyinstatic.com/obj/ies-music/1.mp3",
                    "note_download_url": ""
                  }
                }
                """);

        CollectorArticleMapper.Mapped mapped = CollectorArticleMapper.map(
                root.getString("platform"),
                root.getJSONObject("payload"),
                root.getLong("crawledAt"),
                0L);

        RawArticle article = mapped.article();
        assertThat(article.getUrl()).isEqualTo("https://www.douyin.com/video/7710625308772158729");
        assertThat(article.getTitle()).isEqualTo("0代码自建小程序赚了200块");
        assertThat(article.getText()).isEqualTo("0代码自建小程序赚了200块 #豆包");
        assertThat(article.getName()).isEqualTo("程序员豆腐");
        assertThat(article.getSource()).isEqualTo("douyin");
        assertThat(article.getSiteId()).isEqualTo("1");
        assertThat(article.getUserId()).isEqualTo("5e1594828b4bf909");
        assertThat(article.getLikeCount()).isEqualTo(564L);
        assertThat(article.getCommentCount()).isEqualTo(7L);
        assertThat(article.getCollectCount()).isEqualTo(96L);
        assertThat(article.getRepostCount()).isEqualTo(28L);
        assertThat(article.getImageList()).containsExactly("https://p9-pc-sign.douyinpic.com/obj/cover.jpg");
        assertThat(article.getCreatedAt()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(1795320019), ZoneId.of("Asia/Shanghai")).toString());
        assertThat(article.getCrawlerTime()).isEqualTo(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(1795364415377L), ZoneId.of("Asia/Shanghai")).toString());
        assertThat(mapped.rawVersion()).isEqualTo(1795364415351L);
    }

    @Test
    void acceptsDouyinPlatformAlias() {
        JSONObject payload = JSON.parseObject("""
                {"title": "别名平台", "aweme_url": "https://www.douyin.com/video/1"}
                """);

        CollectorArticleMapper.Mapped mapped = CollectorArticleMapper.map("DouYin", payload, 1000L, 2000L);

        assertThat(mapped.article().getSource()).isEqualTo("douyin");
        assertThat(mapped.rawVersion()).isEqualTo(1000L);
    }

    @Test
    void rejectsDouyinWithoutAwemeUrl() {
        JSONObject payload = JSON.parseObject("""
                {"title": "只有标题", "aweme_id": "123"}
                """);

        assertThatThrownBy(() -> CollectorArticleMapper.map("dy", payload, 1L, 1L))
                .isInstanceOfSatisfying(CollectorArticleMapper.MappingException.class,
                        e -> assertThat(e.type()).isEqualTo(CollectorArticleMapper.PAYLOAD_INVALID));
    }

    @Test
    void rejectsUnsupportedPlatform() {
        JSONObject payload = JSON.parseObject("""
                {"title": "标题", "video_url": "https://example.com/a"}
                """);

        assertThatThrownBy(() -> CollectorArticleMapper.map("xhs", payload, 1L, 1L))
                .isInstanceOfSatisfying(CollectorArticleMapper.MappingException.class,
                        e -> assertThat(e.type()).isEqualTo(CollectorArticleMapper.UNSUPPORTED_PLATFORM));
    }

    @Test
    void rejectsMissingUrl() {
        JSONObject payload = JSON.parseObject("""
                {"title": "只有标题"}
                """);

        assertThatThrownBy(() -> CollectorArticleMapper.map("bili", payload, 1L, 1L))
                .isInstanceOfSatisfying(CollectorArticleMapper.MappingException.class,
                        e -> assertThat(e.type()).isEqualTo(CollectorArticleMapper.PAYLOAD_INVALID));
    }
}
