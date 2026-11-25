package com.zst.zsearch.common.enrich;

final class TextSupport {

    private TextSupport() {
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String contentOf(String title, String text) {
        StringBuilder builder = new StringBuilder();
        if (hasText(title)) {
            builder.append(title).append(' ');
        }
        if (hasText(text)) {
            builder.append(text);
        }
        return builder.toString();
    }
}
