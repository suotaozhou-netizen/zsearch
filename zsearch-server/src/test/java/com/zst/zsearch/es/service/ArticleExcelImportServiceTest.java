package com.zst.zsearch.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.zst.zsearch.common.id.ArticleIdGenerator;
import com.zst.zsearch.common.model.ArticleIndexDocument;
import com.zst.zsearch.es.config.EsProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleExcelImportServiceTest {

    private static final List<String> HEADERS = List.of(
            "信息源", "标题", "超链接", "发布时间", "采集时间",
            "账号昵称", "账号主页链接", "信息属性", "正文",
            "转发数", "评论数", "点赞数", "粉丝数", "收藏数", "OCR", "ASR", "头像"
    );

    @Mock
    private ElasticsearchClient client;

    @Test
    void importsExcelAndMapsAllFields() throws Exception {
        BulkResponse bulkResponse = org.mockito.Mockito.mock(BulkResponse.class);
        BulkResponseItem item = org.mockito.Mockito.mock(BulkResponseItem.class);
        when(item.error()).thenReturn(null);
        when(bulkResponse.items()).thenReturn(List.of(item));
        when(client.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        ArticleImportResponse result = service().importExcel(workbook(headers ->
                writeRow(headers, new Object[]{
                        "新浪微博", "深圳男生讨论", " https://example.com/a/1 ",
                        "2026-11-01 12:30:00", "2026-11-01 13:00:00",
                        "测试账号", "https://weibo.com/u/1", "负面", "深圳男生在讨论新话题",
                        10, 20, 30, 40, 50, "图片文字", "音频文字", "https://img.example.com/avatar.jpg"
                })
        ));

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getFailed()).isZero();
        assertThat(result.getErrors()).isEmpty();

        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(client).bulk(captor.capture());
        BulkOperation operation = captor.getValue().operations().get(0);
        ArticleIndexDocument document = (ArticleIndexDocument) operation.index().document();

        assertThat(operation.index().id()).isEqualTo(ArticleIdGenerator.fromUrl("https://example.com/a/1"));
        assertThat(document.getId()).isEqualTo(operation.index().id());
        assertThat(document.getSource()).isEqualTo("新浪微博");
        assertThat(document.getHomeUrl()).isEqualTo("https://weibo.com/u/1");
        assertThat(document.getAvatar()).isEqualTo("https://img.example.com/avatar.jpg");
        assertThat(document.getEmotion()).isEqualTo("负面");
        assertThat(document.getCreatedAt()).isEqualTo("2026-11-01T12:30:00");
        assertThat(document.getCrawlerTime()).isEqualTo("2026-11-01T13:00:00");
        assertThat(document.getTextLength()).isEqualTo("深圳男生在讨论新话题".length());
        assertThat(document.getInteractionCount()).isEqualTo(150L);
    }

    @Test
    void rejectsMissingHeaders() throws Exception {
        MockMultipartFile file = workbook(
                List.of("标题", "超链接"),
                headers -> writeRow(headers, new Object[]{"标题", "https://example.com/a/1"})
        );

        assertThatThrownBy(() -> service().importExcel(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("发布时间")
                .hasMessageContaining("账号主页链接");
    }

    @Test
    void reportsRowsWithBlankUrl() throws Exception {
        MockMultipartFile file = workbook(headers ->
                writeRow(headers, new Object[]{
                        "新浪微博", "标题", "",
                        "2026-11-01 12:30:00", "2026-11-01 13:00:00",
                        "测试账号", "https://weibo.com/u/1", "负面", "正文",
                        0, 0, 0, 0, 0, "", ""
                })
        );

        ArticleImportResponse result = service().importExcel(file);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getImported()).isZero();
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).singleElement().asString().contains("第 2 行");
    }

    private ArticleExcelImportService service() {
        EsProperties properties = new EsProperties();
        properties.setIndexName("zsearch_article");
        return new ArticleExcelImportService(client, properties);
    }

    private MockMultipartFile workbook(RowWriter writer) throws Exception {
        return workbook(HEADERS, writer);
    }

    private MockMultipartFile workbook(List<String> headers, RowWriter writer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("文章");
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            var dataRow = sheet.createRow(1);
            writer.write(dataRow);
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "articles.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    private void writeRow(org.apache.poi.ss.usermodel.Row row, Object[] values) {
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            var cell = row.createCell(i);
            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(value == null ? "" : value.toString());
            }
        }
    }

    @FunctionalInterface
    private interface RowWriter {
        void write(org.apache.poi.ss.usermodel.Row row);
    }
}
