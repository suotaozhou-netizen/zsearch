package com.zst.zsearch.biz.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleInteractionSumVo {

    private long repostCount;
    private long commentCount;
    private long likeCount;
    private long followerCount;
    private long collectCount;
}
