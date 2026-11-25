package com.zst.zsearch.common.enums;

import lombok.Getter;

@Getter
public enum Emotion {

    POSITIVE("正面"),
    NEUTRAL("中性"),
    NEGATIVE("负面");

    private final String label;

    Emotion(String label) {
        this.label = label;
    }

    public static Emotion fromLabel(String label) {
        for (Emotion emotion : values()) {
            if (emotion.label.equals(label)) {
                return emotion;
            }
        }
        throw new IllegalArgumentException("不支持的情感类型: " + label);
    }
}
