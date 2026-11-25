package com.zst.zsearch.biz.search.parser;

import com.zst.zsearch.common.enums.MonitorType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MonitorSearchParserRegistry {

    private final Map<MonitorType, MonitorSearchParser> parsers;

    public MonitorSearchParserRegistry(List<MonitorSearchParser> parserList) {
        Map<MonitorType, MonitorSearchParser> map = new EnumMap<>(MonitorType.class);
        for (MonitorSearchParser parser : parserList) {
            if (map.putIfAbsent(parser.type(), parser) != null) {
                throw new IllegalStateException("监测解析器重复: " + parser.type());
            }
        }
        this.parsers = Collections.unmodifiableMap(map);
    }

    public MonitorSearchParser get(MonitorType type) {
        MonitorSearchParser parser = parsers.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("不支持的监测类型: " + type);
        }
        return parser;
    }
}
