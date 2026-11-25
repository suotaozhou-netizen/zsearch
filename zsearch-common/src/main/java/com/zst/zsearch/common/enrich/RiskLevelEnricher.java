package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

import java.util.Set;

public class RiskLevelEnricher implements ArticleEnricher {

    private static final Set<String> HIGH_RISK_WORDS = Set.of(
            "死亡", "伤亡", "事故", "火灾", "爆炸", "群体性", "暴力", "恐怖",
            "自杀", "犯罪", "违法", "泄露");
    private static final Set<String> MEDIUM_RISK_WORDS = Set.of(
            "投诉", "维权", "举报", "诈骗", "贪污", "处罚", "质疑", "危机",
            "污染", "欠薪", "冲突", "曝光");

    @Override
    public void enrich(RawArticle source, ArticleIndexDocument target) {
        if (TextSupport.hasText(target.getRiskLevel())) {
            return;
        }
        String content = TextSupport.contentOf(source.getTitle(), source.getText());
        if (content.isEmpty()) {
            return;
        }
        if (containsAny(content, HIGH_RISK_WORDS)) {
            target.setRiskLevel("重点关注");
        } else if (containsAny(content, MEDIUM_RISK_WORDS)) {
            target.setRiskLevel("关注风险");
        }
    }

    private boolean containsAny(String content, Set<String> words) {
        for (String word : words) {
            if (content.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
