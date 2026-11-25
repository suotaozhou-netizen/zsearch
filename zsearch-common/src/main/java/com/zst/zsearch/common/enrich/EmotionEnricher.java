package com.zst.zsearch.common.enrich;

import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.common.model.RawArticle;

import java.util.Set;

public class EmotionEnricher implements ArticleEnricher {

    private static final Set<String> POSITIVE_WORDS = Set.of(
            "表彰", "获奖", "增长", "提升", "突破", "利好", "成功", "支持",
            "点赞", "优秀", "领先", "改善", "受益", "公益");
    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "事故", "死亡", "受伤", "投诉", "举报", "维权", "诈骗", "违法",
            "犯罪", "裁员", "欠薪", "曝光", "质疑", "冲突", "危机", "污染",
            "处罚", "判刑");

    @Override
    public void enrich(RawArticle source, ArticleIndexDocument target) {
        if (TextSupport.hasText(target.getEmotion())) {
            return;
        }
        String content = TextSupport.contentOf(source.getTitle(), source.getText());
        if (content.isEmpty()) {
            return;
        }

        int positive = count(content, POSITIVE_WORDS);
        int negative = count(content, NEGATIVE_WORDS);
        if (negative > positive) {
            target.setEmotion("负面");
        } else if (positive > negative) {
            target.setEmotion("正面");
        } else {
            target.setEmotion("中性");
        }
    }

    private int count(String content, Set<String> words) {
        int total = 0;
        for (String word : words) {
            int from = 0;
            int index;
            while ((index = content.indexOf(word, from)) >= 0) {
                total++;
                from = index + word.length();
            }
        }
        return total;
    }
}
