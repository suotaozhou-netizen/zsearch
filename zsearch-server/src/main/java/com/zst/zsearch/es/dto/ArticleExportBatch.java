package com.zst.zsearch.es.dto;

import co.elastic.clients.elasticsearch._types.FieldValue;

import java.util.List;

/**
 * 离线导出时按 search_after 游标分批拉取的结果
 */
public record ArticleExportBatch(List<Article> articles, List<FieldValue> nextSearchAfter, long total) {
}