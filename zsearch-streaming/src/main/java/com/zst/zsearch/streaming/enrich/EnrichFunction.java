package com.zst.zsearch.streaming.enrich;

import com.zst.zsearch.common.enrich.EnricherChain;
import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.streaming.model.IngestRecord;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class EnrichFunction extends ProcessFunction<IngestRecord, IngestRecord> {

    private static final Logger log = LoggerFactory.getLogger(EnrichFunction.class);

    public static final String ENRICH_ERROR = "ENRICH_ERROR";

    public static final OutputTag<IngestRecord> DLQ_TAG =
            new OutputTag<>("enrich-dlq", TypeInformation.of(IngestRecord.class));

    private final int summaryLength;
    private final int pipelineVersion;

    private transient EnricherChain chain;
    private transient Counter enrichedCounter;
    private transient Counter failedCounter;

    public EnrichFunction(int summaryLength, int pipelineVersion) {
        this.summaryLength = summaryLength;
        this.pipelineVersion = pipelineVersion;
    }

    @Override
    public void open(Configuration parameters) {
        chain = new EnricherChain(summaryLength);
        enrichedCounter = getRuntimeContext().getMetricGroup().counter("recordsEnriched");
        failedCounter = getRuntimeContext().getMetricGroup().counter("recordsEnrichFailed");
    }

    @Override
    public void processElement(IngestRecord record, Context ctx, Collector<IngestRecord> out) {
        try {
            String indexedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            ArticleIndexDocument document = buildDocument(record, chain, pipelineVersion, indexedAt);
            record.setDocument(document);
            record.setErrorType(null);
            record.setErrorMessage(null);
            enrichedCounter.inc();
            log.debug("富化完成, articleId={}, textLength={}, emotion={}, riskLevel={}",
                    record.getArticleId(), document.getTextLength(), document.getEmotion(), document.getRiskLevel());
            out.collect(record);
        } catch (RuntimeException e) {
            failedCounter.inc();
            record.setErrorType(ENRICH_ERROR);
            record.setErrorMessage("富化失败: " + e.getMessage());
            log.warn("富化失败, articleId={}, message={}", record.getArticleId(), e.getMessage());
            ctx.output(DLQ_TAG, record);
        }
    }

    public static ArticleIndexDocument buildDocument(IngestRecord record,
                                                     EnricherChain chain,
                                                     int pipelineVersion,
                                                     String indexedAt) {
        ArticleIndexDocument document = chain.assemble(record.getArticleId(), record.getEnvelope().getArticle());
        document.setRawVersion(record.getRawVersion());
        document.setPipelineVersion(pipelineVersion);
        document.setContentHash(record.getContentHash());
        document.setIndexedAt(indexedAt);
        return document;
    }
}
