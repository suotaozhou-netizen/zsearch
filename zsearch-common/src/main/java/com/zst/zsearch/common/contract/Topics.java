package com.zst.zsearch.common.contract;

public final class Topics {

    // 采集端 MediaCrawler 实际写入 search-topic，由适配层转换为流水线信封
    public static final String RAW_ARTICLE = "search-topic";
    public static final String RAW_ARTICLE_DLQ = "zsearch.raw.article.dlq";

    private Topics() {
    }
}
