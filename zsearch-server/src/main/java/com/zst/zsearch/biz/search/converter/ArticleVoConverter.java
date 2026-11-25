package com.zst.zsearch.biz.search.converter;

import com.zst.zsearch.biz.model.vo.ArticleListVo;
import com.zst.zsearch.biz.model.vo.ArticleVo;
import com.zst.zsearch.es.dto.Article;
import com.zst.zsearch.es.dto.ArticleSearchResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleVoConverter {

    public ArticleVo toVo(Article article) {
        ArticleVo vo = new ArticleVo();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setText(article.getText());
        vo.setSummary(article.getSummary());
        vo.setName(article.getName());
        vo.setSource(article.getSource());
        vo.setUrl(article.getUrl());
        vo.setHomeUrl(article.getHomeUrl());
        vo.setAvatar(article.getAvatar());
        vo.setSiteId(article.getSiteId());
        vo.setUserId(article.getUserId());
        vo.setImageList(article.getImageList());
        vo.setVideoList(article.getVideoList());
        vo.setCreatedAt(article.getCreatedAt());
        vo.setCrawlerTime(article.getCrawlerTime());
        vo.setEmotion(article.getEmotion());
        vo.setRiskLevel(article.getRiskLevel());
        vo.setOcr(article.getOcr());
        vo.setAsr(article.getAsr());
        vo.setRepostCount(article.getRepostCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setFollowerCount(article.getFollowerCount());
        vo.setCollectCount(article.getCollectCount());
        vo.setInteractionCount(article.getInteractionCount());
        return vo;
    }

    public ArticleListVo toListVo(ArticleSearchResponse response) {
        ArticleListVo result = new ArticleListVo();
        result.setTotal(response.getTotal());
        result.setPage(response.getPage());
        result.setSize(response.getSize());
        result.setTotalPages(response.getTotalPages());
        result.setTook(response.getTook());
        List<Article> articles = response.getArticleList();
        result.setArticleList(articles == null
                ? List.of()
                : articles.stream().map(this::toVo).toList());
        return result;
    }
}
