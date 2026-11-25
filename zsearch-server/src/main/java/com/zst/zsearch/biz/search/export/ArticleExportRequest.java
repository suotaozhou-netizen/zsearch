package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.biz.model.dto.ArticleQueryConditionDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ArticleExportRequest extends ArticleQueryConditionDto {

    /**
     * selected / page / first_1000 / all
     */
    @NotBlank(message = "导出范围不能为空")
    private String scope;

    /**
     * 选中导出时的文章 id
     */
    private List<String> ids;

    @Min(value = 1, message = "page 必须大于等于 1")
    private Integer page;

    @Min(value = 1, message = "size 必须大于等于 1")
    private Integer size;

    private String sortBy;

    private String sortOrder;

    @NotEmpty(message = "导出字段不能为空")
    private List<String> fields;
}