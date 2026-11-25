package com.zst.zsearch.biz.search.export;

import com.zst.zsearch.es.dto.Article;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 流式写出文章数据，避免离线导出 10 万行时内存膨胀
 */
public class ArticleExcelWriter implements AutoCloseable {

    private static final int ROW_ACCESS_WINDOW_SIZE = 200;
    private static final int EXCEL_CELL_MAX_LENGTH = 32_767;
    private static final int SERIAL_COLUMN_WIDTH = 8 * 256;
    private static final String SHEET_NAME = "文章数据";
    private static final String SERIAL_HEADER = "序号";

    private final SXSSFWorkbook workbook;
    private final Sheet sheet;
    private final List<ExportColumn> columns;
    private final CellStyle textStyle;
    private int dataRows;

    public ArticleExcelWriter(List<ExportColumn> columns) {
        this.columns = columns;
        this.workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
        this.workbook.setCompressTempFiles(true);
        this.sheet = workbook.createSheet(SHEET_NAME);
        this.textStyle = buildTextStyle(workbook);
        writeHeader();
    }

    public void append(List<Article> articles) {
        for (Article article : articles) {
            Row row = sheet.createRow(dataRows + 1);
            row.createCell(0).setCellValue(dataRows + 1);
            dataRows++;
            int columnIndex = 1;
            for (ExportColumn column : columns) {
                writeValue(row.createCell(columnIndex++), column.extract(article));
            }
        }
    }

    public int rowCount() {
        return dataRows;
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
    }

    @Override
    public void close() throws IOException {
        try {
            workbook.close();
        } finally {
            workbook.dispose();
        }
    }

    private void writeHeader() {
        Row header = sheet.createRow(0);
        CellStyle headerStyle = buildHeaderStyle(workbook);
        Cell serial = header.createCell(0);
        serial.setCellValue(SERIAL_HEADER);
        serial.setCellStyle(headerStyle);
        sheet.setColumnWidth(0, SERIAL_COLUMN_WIDTH);
        int columnIndex = 1;
        for (ExportColumn column : columns) {
            Cell cell = header.createCell(columnIndex);
            cell.setCellValue(column.getHeader());
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(columnIndex, column.getWidth() * 256);
            columnIndex++;
        }
    }

    private void writeValue(Cell cell, Object value) {
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value == null) {
            return;
        }
        String text = value.toString();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > EXCEL_CELL_MAX_LENGTH) {
            text = text.substring(0, EXCEL_CELL_MAX_LENGTH);
        }
        if (needsEscape(text)) {
            text = "'" + text;
        }
        cell.setCellValue(text);
        cell.setCellStyle(textStyle);
    }

    /**
     * 防止以公式字符开头的内容被 Excel 当作公式执行
     */
    private static boolean needsEscape(String text) {
        char first = text.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r';
    }

    private static CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle buildTextStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }
}