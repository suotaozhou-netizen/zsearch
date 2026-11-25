package com.zst.zsearch.es.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleSumStatsResponse {

    private long total;
    private long took;
    private Sums sums;
    private List<EmotionSumBucket> byEmotion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sums {

        private long repostCount;
        private long commentCount;
        private long likeCount;
        private long followerCount;
        private long collectCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionSumBucket {

        private String key;
        private long count;
        private Sums sums;
    }
}
