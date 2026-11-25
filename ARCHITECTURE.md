# zSearch 技术架构（Kafka → Flink → ES）

> 版本：1.0
> 日期：2026-11-20
> 状态：设计定稿，待实现
> 适用读者：后续开发 Agent、后端/数据开发人员
> 维护约定：任何架构决策变更，必须同步更新本文的「决策记录」与「硬约束」两节。

---

## 0. 给后续 Agent 的阅读指南与硬约束（先读这一节）

### 0.1 本文覆盖的范围

```
Kafka 消息 → Flink（校验 / 去重 / 富化）→ Elasticsearch 7.17.29
```

**明确不在本链路范围内：**

- 数据如何进入 Kafka（由采集服务 MediaCrawler 负责，后续对接）
- 原始数据长期存档（由采集服务负责，本链路归档分支已删除）
- 实时监测告警（后续阶段）
- 搜索 API 与监测管理功能（沿用现有 `zsearch-server`，本期不改）

### 0.2 硬约束（MUST / MUST NOT）

1. **MUST**：Flink 只消费 Kafka，不直接读 MySQL 业务表。
2. **MUST NOT**：在本链路增加任何归档/存档逻辑（存档归采集服务）。
3. **MUST NOT**：给 `search-topic` 开启日志压缩（compact）。历史重放依赖完整日志。
4. **MUST NOT**：使用 `flink-connector-elasticsearch7` 自带 sink。它不支持外部版本号，必须自研 bulk sink。
5. **MUST**：ES 写入使用 `_id=articleId` + `_version=rawVersion` + `_version_type=external_gte`。
6. **MUST**：`articleId` 算法冻结为 v1（`md5(url.trim())`）。改算法等于全量重建索引，必须走独立变更流程。
7. **MUST NOT**：把 `pipelineVersion` 放进 Kafka 消息。它是作业配置，由作业写入 ES 文档。
8. **MUST**：重建/重放作业必须使用新消费组 + 空状态 + 目标索引；重建时遵守第 8.2 节（本项目不使用别名，不存在切别名这一步）。
9. **MUST**：ES mapping 变更遵循「先更新 index template，再发布作业」的顺序。
10. **MUST NOT**：`zsearch-common` 引入 Spring、Flink、ES Client 依赖。
11. **MUST NOT**：在迁移完成前同时运行旧的 `ArticlePipelineWorker` 和新 Flink 作业写同一个索引（影子索引对比期除外）。
12. **MUST**：不对外承诺 exactly-once，统一表述为「有效一次（effectively-once）」。

### 0.3 当前代码事实（2026-11-21）

- `zsearch-common/` 已按第 9.2 节落地（模型、SiteId、ID/指纹、富化链、契约常量、JSON 编解码），`mvn test` 全绿。
- `zsearch-streaming/` 已按第 6、9.3 节落地：Source → 校验 → 去重 → 富化 → 自研 ES sink + DLQ，`mvn test` 全绿，`mvn package` 产出 `*-job.jar`。尚未接入真实 Kafka/ES 做端到端验证（Testcontainers 集成测试待补）。
- `zsearch-server/` 是现有 Spring Boot 3.5.16 服务，已改为依赖 `zsearch-common`：
  - `zsearch-server/src/main/java/com/zst/zsearch/pipeline/service/ArticlePipelineWorker.java`（`@Scheduled` 每秒轮询 `article_ingest_task`，P2 由 Flink 取代）
  - `pipeline/service/ArticleDocumentAssembler.java`（薄适配器，内部持有 `EnricherChain`，仅负责注入 `summaryLength`）
  - `es/service/ArticleExcelImportService.java`（直写 ES 的旁路，P3 统一走消息契约）
- 采集端已删除旧的 MySQL `article_ingest_task` 入库链路；7 个平台（xhs/dy/ks/bili/wb/tieba/zhihu）的 content/comment 统一发布到 Kafka `search-topic`。Flink 适配层当前仅支持 `bili`/`dy` 的 content，其余平台事件进入适配 DLQ。
- `SiteId` 当前只有 1..5（抖音/小红书/bilibili/微博/今日头条），**缺少快手/贴吧/知乎编码**，见第 13 节未决问题。
- 现有 ES mapping 的字段命名是 **SNAKE_CASE**（`created_at`、`risk_level`、`text_length`…），仅 `homeUrl/siteId/userId/imageList/videoList` 为显式驼峰。Flink sink 与 server 必须使用同一个 SNAKE_CASE 映射器。

---

## 1. 背景与目标

### 1.1 现状

采集服务 → MySQL 任务表 → `zsearch-server` 定时轮询 → 富化 → 批量写 ES → 搜索 API / 前端。

问题：

- 加工能力与 Web 服务耦合，扩缩容互相拖累
- 轮询 + claim 的重试语义自己维护，缺少回放能力
- 无统一消息契约，Excel 导入等旁路会出现「未富化、无版本号」的文档
- 采集端与写库强耦合

### 1.2 目标

- 采集与加工解耦：Kafka 作为唯一入口流
- Flink 承担校验、去重、富化，可独立扩缩容
- ES 写入幂等且不会被旧数据覆盖
- 支持在 Kafka 保留期内自助重建索引
- 为后续实时监测告警留出多消费者空间

### 1.3 非目标

- 不做采集端改造
- 不做长期存档
- 不做 exactly-once
- 不改搜索/监测业务语义

---

## 2. 系统上下文

```
                          ┌──────────────────────┐
  MediaCrawler / 采集服务 ─▶│ Kafka 3.9.1 (KRaft)  │  8.163.135.98:9092
                          │ search-topic (原生事件)│
                          └──────────┬───────────┘
                                     │ consume
                                     ▼
                          ┌──────────────────────┐
                          │ Flink 1.20.1 作业     │  JM 8081 / TM
                          │ 适配→校验→去重→富化→Sink│
                          └──────────┬───────────┘
                                     │ bulk (external_gte)
                                     ▼
                          ┌──────────────────────┐
                          │ ES 7.17.29 单节点     │  8.163.135.98:9200
                          │ zsearch_article      │
                          └──────────┬───────────┘
                                     │ query
                                     ▼
                          ┌──────────────────────┐
                          │ zsearch-server :8888 │  /api/search /api/monitor
                          └──────────┬───────────┘
                                     ▼
                          ┌──────────────────────┐
                          │ Vue3 前端 :5173      │
                          └──────────────────────┘

  另有 MySQL 192.168.88.101:3306（监测配置等控制面数据）
```

---

## 3. 决策记录

| # | 决策 | 结论 | 原因 |
|---|---|---|---|
| D1 | 入口拓扑 | Kafka 为唯一入口（生产者后续由采集服务实现） | 解耦、缓冲、可重放 |
| D2 | 加工方式 | Flink 流处理，不再用 DB 轮询 | 去掉 claim/retry 自维护语义 |
| D3 | 存档 | 采集服务负责，本链路不实现 | 范围收窄，避免双写 |
| D4 | Kafka 保留期 | **7 天** | 自助重放窗口；更早数据由采集服务回灌 |
| D5 | ES 版本 | **7.17.29**（冻结） | 现有服务端已锁定该版本 |
| D6 | 一致性 | 有效一次（at-least-once + 幂等写） | ES 无事务，不承诺 exactly-once |
| D7 | 去重 | Flink Keyed State（内容哈希 + 版本） | 省富化和 ES 写入 |
| D8 | 富化逻辑归属 | `zsearch-common` 纯函数链 | server/Flink 共用同一套规则 |
| D9 | 作业形态 | 单作业、单 ES sink | 最小运维面 |
| D10 | 宿主机 | 现在 8G，后续可能 16G | 决定并行度与堆预算 |
| D11 | 状态后端 | `hashmap`（数据量增长后换 RocksDB） | 当前量级内存足够 |
| D12 | JDK | 17 | Flink 1.20 / Spring Boot 3 均支持 |

---

## 4. 消息契约（本链路的生产者接口）

### 4.1 基本要求

| 项 | 要求 |
|---|---|
| Topic | `search-topic`（采集端原生事件，3 分区 / 7 天） |
| Kafka key | 采集端 `eventId`（如 `bili:content:117...`）；适配后内部按 `articleId` 分区 |
| 序列化 | UTF-8 JSON |
| 单条大小 | < 1MB（超过需拆分或外部存储引用） |
| 生产者 | `acks=all`、`enable.idempotence=true` |
| 重复投递 | 允许，下游去重与 ES 版本号会吸收 |

采集端原生事件由 `CollectorEventAdapter` 转换为 4.2 的内部信封。`dataType=ping` 与评论事件在适配层直接跳过，不进入 DLQ；当前支持 `platform=bili|bilibili` 与 `platform=dy|douyin` 的 `dataType=content` 事件，其他平台进入适配 DLQ（`UNSUPPORTED_PLATFORM`）。

```json
{
  "eventId": "bili:content:117178586240848",
  "platform": "bili",
  "dataType": "content",
  "crawlerType": "search",
  "crawledAt": 1795279742029,
  "payload": {
    "video_url": "https://www.bilibili.com/video/av117178586240848",
    "title": "标题",
    "desc": "简介",
    "create_time": 1788003504,
    "last_modify_ts": 1795279742022,
    "nickname": "作者",
    "creator_hash": "8d8e7a39c35f792c",
    "liked_count": "15238",
    "video_comment": "1344",
    "video_share_count": "3148",
    "video_favorite_count": "38158",
    "video_cover_url": "http://i0.hdslb.com/bfs/archive/cover.jpg"
  }
}
```

### 4.2 内部信封结构（适配后）

```json
{
  "schemaVersion": 1,
  "idVersion": 1,
  "articleId": "5d41402abc4b2a76b9719d911017c592",
  "rawVersion": 1763630000123,
  "crawledAt": "2026-11-20T23:20:00.000+08:00",
  "article": {
    "url": "https://www.xiaohongshu.com/explore/xxxx",
    "title": "标题",
    "text": "正文",
    "summary": null,
    "name": "作者",
    "source": "小红书",
    "homeUrl": null,
    "avatar": null,
    "createdAt": "2026-11-20T20:00:00.000+08:00",
    "crawlerTime": "2026-11-20T23:20:00.000+08:00",
    "emotion": null,
    "riskLevel": null,
    "ocr": null,
    "asr": null,
    "repostCount": 0,
    "commentCount": 0,
    "likeCount": 0,
    "followerCount": null,
    "collectCount": 0,
    "siteId": "2",
    "userId": "author-id",
    "imageList": [],
    "videoList": []
  }
}
```

字段规则：

| 字段 | 规则 |
|---|---|
| `schemaVersion` | 消息结构版本，当前 1；字段只增不删，破坏性改动升版本 |
| `idVersion` | `articleId` 算法版本，当前 1 |
| `articleId` | 32 位小写十六进制 = `md5(url.trim())`；同一 URL 必须稳定 |
| `rawVersion` | 毫秒时间戳，**同一 articleId 单调递增**；适配层优先取 `last_modify_ts`，缺失时回退 `crawledAt` 或 Kafka 记录时间戳 |
| `crawledAt` | 本次采集时间，ISO-8601 带时区 |
| `article` | 原始业务字段。bili 映射：`video_url→url`、`desc→text`、`nickname→name`、`creator_hash→userId`、`create_time→createdAt`、`video_cover_url→imageList`、互动计数同名映射。dy 映射：`aweme_url→url`、`title→title`、`desc→text`、`nickname→name`、`creator_hash→userId`、`create_time→createdAt`、`cover_url→imageList`、`liked_count/comment_count/share_count/collected_count` 对应点赞/评论/转发/收藏 |

### 4.3 必填校验（Flink 侧）

- `articleId` 32 位 hex
- `rawVersion > 0`
- `article.url` 非空
- `article.title` 与 `article.text` 至少一个非空
- `siteId` 属于合法枚举
- `articleId` 必须等于 `md5(article.url.trim())`（用 common 的 `ArticleIdGenerator` 重算比对）

任一项失败 → DLQ，并发告警。**ID 不一致视为契约漂移，不允许放行。**

---

## 5. Kafka 设计

### 5.1 Topic

| Topic | 分区 | 副本 | 保留 | 说明 |
|---|---|---|---|---|
| `search-topic` | 3 | 1（单机） | 7 天 | 采集端原生事件（`cleanup.policy=delete`） |
| `zsearch.raw.article.dlq` | 1 | 1 | 30 天 | 适配/校验/富化/写入失败数据 |

### 5.2 Broker 配置

```properties
log.cleanup.policy=delete
log.retention.ms=604800000          # 7 天
log.retention.bytes=3221225472      # 每分区 3GB 上限（注意：按分区计算）
log.segment.bytes=134217728         # 128MB
log.segment.ms=86400000             # 1 天滚动，保证过期数据及时删除
auto.create.topics.enable=false
```

说明：

- 删除按日志段进行，实际保留约 7~8 天。
- `retention.bytes` 是每分区上限，6 分区约 18GB 总盘上限。
- 保留期是**唯一自助重放窗口**，超出后需采集服务从存档回灌。

### 5.3 消费组

| 用途 | 消费组 |
|---|---|
| 正常加工 | `zsearch-ingest-v1` |
| 重建/回放 | `zsearch-replay-<yyyyMMdd>`（每次新建） |

---

## 6. Flink 作业设计

### 6.1 拓扑

```
KafkaSource(search-topic)
  │  KafkaRecordDeserializationSchema（保留 topic/partition/offset 元数据）
  ▼
CollectorEventAdapter（原生事件 → IngestEnvelope，跳过 ping/评论）──side output──▶ DLQ Sink
  ▼
解析 + 校验 ──side output──▶ DLQ Sink
  ▼
keyBy(articleId)
  ▼
DedupFunction（ValueState<String> "contentHash|lastSeenRawVersion"，TTL 30 天）
  ▼
EnrichFunction（common 规则链，固定顺序）
  ▼
EsBulkSink（_id=articleId，_version=rawVersion，external_gte）
```

无 watermark，去重按处理时间 TTL。作业内部用 `IngestRecord` 包装「原始消息 + 解析结果 + 加工后文档」，便于 DLQ 回传原始数据。

### 6.2 Source

```java
KafkaSource.<String>builder()
    .setBootstrapServers("8.163.135.98:9092")
    .setTopics("search-topic")
    .setGroupId("zsearch-ingest-v1")
    .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
    .setProperty("partition.discovery.interval.ms", "60000")
    .setDeserializer(new ArticleRecordDeserializer())
    .build();
```

- 必须用记录级反序列化器，DLQ 需要原始 `topic/partition/offset`。
- offset 只靠 checkpoint 提交（开启 checkpoint 后默认 `commit.offsets.on.checkpoint=true`），不启用自动提交。

### 6.3 去重

状态编码（一个 String，避免 POJO 状态演变问题）：

```
contentHash|lastSeenRawVersion
```

判定规则：

| 情况 | 动作 |
|---|---|
| 无状态 | 放行，写入状态 |
| `rawVersion <= lastSeenRawVersion` | 丢弃（乱序/重放/重复） |
| `contentHash == 上次哈希` | 丢弃，仅更新 `lastSeenRawVersion` |
| 内容变化且版本更新 | 放行，更新状态 |

- `contentHash`：对 `article` 中排除 `crawlerTime`、`rawVersion` 后的规范化 JSON 做 SHA-256。
- TTL 30 天，RocksDB/compaction 模式或 hashmap 惰性清理。
- 去重只是省写入的优化；正确性下线由 ES 外部版本号保证。

### 6.4 富化

- 实现位于 `zsearch-common`，纯函数、无 IO、无可变共享状态。
- 固定顺序：`TextLength → InteractionCount → Summary → Emotion → RiskLevel`。
- Summary 长度等参数由作业配置传入，不读 Spring 配置。
- 单条异常 → DLQ，作业继续。
- 未来接 LLM/模型：使用 Async I/O + 缓存 + 限流，不得在同步算子中调用外部 HTTP。

### 6.5 ES Sink（自研）

- 客户端：`co.elastic.clients:elasticsearch-java:7.17.29`（与服务端一致）。
- 直接写索引 `zsearch_article`，**不使用别名**；`--es.index` 默认值同为 `zsearch_article`。
- 批量策略：500 条 / 1 秒 / 5MB，先到先刷；checkpoint 时必须 flush。
- 写入动作必须携带版本：

```java
builder.operations(op -> op.index(idx -> idx
    .index("zsearch_article")
    .id(articleId)
    .version(rawVersion)
    .versionType(VersionType.ExternalGte)
    .document(document)));
```

错误处理：

| 情况 | 处理 |
|---|---|
| 429 / 502 / 503 | 指数退避重试，带抖动 |
| 409 `version_conflict_engine_exception` | **视为成功**（旧版本被拒），计入指标 |
| 其余 4xx（mapping/字段错误） | 重试无意义，写 DLQ（sink 内部持有 KafkaProducer） |
| 部分失败 | 只重试失败项，超过次数后 DLQ |

禁止 `Refresh.WaitFor`，统一 `Refresh.False`。

### 6.6 DLQ

- Topic：`zsearch.raw.article.dlq`。
- 消息体：原始记录 + 错误信息。
- Headers：`error_type`、`error_msg`（截断）、`src_topic`、`src_partition`、`src_offset`、`failed_at`。
- 配套重放工具：消费 DLQ → 修正 → 重投主 topic；不做无限自动重试。

---

## 7. ES 索引设计

### 7.1 索引

| 名称 | 用途 |
|---|---|
| `zsearch_article` | 唯一索引，server 读、Flink 写都用这个名字 |

**不使用别名**：`zsearch-server` 的 `ES_INDEX` 与 Flink 的 `--es.index` 默认值都是 `zsearch_article`。

代价是重建时无法通过切别名做到读写无感切换：需要按第 8.2 节「停写 → 重建 → 回放 → 对账 → 恢复」。单节点内部系统可接受短暂搜索不可用。

### 7.2 Mapping 规则

- 以现有索引 mapping 为基线（先 `GET /zsearch_article/_mapping` 导出），保留 IK 分词器与 `createdAt` 日期格式。
- 新增字段（字段名遵循现有 SNAKE_CASE 策略）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `raw_version` | long | 文档数据版本 |
| `pipeline_version` | integer | 加工逻辑版本 |
| `content_hash` | keyword | 内容指纹 |
| `indexed_at` | date | 入库时间 |
| `text_length` | integer | 富化结果 |
| `interaction_count` | long | 富化结果 |

- `imageList` / `videoList` 使用 `keyword`（与现有 mapping 一致的显式驼峰）。
- `dynamic: strict`：字段未进模板即报错，防止契约漂移。
- 单节点部署 `number_of_replicas: 0`。
- 可直接执行的初始索引脚本：`zsearch-server/src/main/resources/es/zsearch_article.http`（创建、验证、样例写入、清理）。

### 7.3 升级顺序

**只增字段（常规）**，无需停写、无需重建：

1. 修改 `zsearch_article.http` 中的 mapping，仅追加字段
2. 对在线索引执行 `PUT /zsearch_article/_mapping` 追加同一批字段
3. 发布新作业（`pipelineVersion` 递增）

**不兼容变更**（改字段类型、改分词器、改 `articleId` 算法等）：走第 8.2 节全量重建，不允许原地修改。

---

## 8. 一致性、重放与故障处理

### 8.1 语义

| 层 | 保证 |
|---|---|
| Kafka 消费 | at-least-once（checkpoint 提交 offset） |
| Flink 去重 | 进程内状态；重启后从 checkpoint 恢复 |
| ES 写入 | 有效一次（确定性 ID + external_gte） |
| 端到端 | **有效一次**：恢复时可能重复处理，结果收敛且不回退 |

### 8.2 重建（不使用别名）

重建必须保留或回灌全量数据，因此顺序是「先停写，再删除重建，最后回放」：

1. 停写：`flink stop` 停作业，同时停 server 侧入库任务（`ArticlePipelineWorker` / Excel 导入）
2. 删除并重建 `zsearch_article`（执行 `zsearch_article.http` 第 2、3 步）
3. 回放 7 天内的 Kafka 数据到 `zsearch_article`：
   新消费组 `zsearch-replay-<yyyyMMdd>`、空状态、`--from.timestamp` 取 7 天前、`--to.timestamp` 取当前、`--es.index zsearch_article`
4. 早于 7 天的数据由采集服务从存档回灌（见 8.3），作业继续消费新数据
5. 对账：文档数、抽样字段、`contentHash` 一致性
6. 恢复实时作业（原消费组，从 checkpoint 恢复；若重建期间有重叠，`external_gte` 保证不回退）

单节点无法做到搜索零中断：重建期间 `zsearch_article` 不存在或不完整，搜索会返回空/报错，需在维护窗口执行。

### 8.3 超出 7 天

由采集服务从其存档回灌到 `search-topic`（保持原生事件格式；或独立回灌 topic），Flink 用新的消费组消费。本链路不实现回灌，只提供消费侧支持。

### 8.4 故障处理原则

- 数据错误（单条脏数据）→ DLQ，作业继续
- 基础设施错误（ES 不可用）→ sink 阻塞产生背压，lag 上升，不丢数据；依靠 7 天缓冲兜底
- 作业长期停止 → 超过 7 天后 Kafka 数据开始丢失，因此停机告警必须严格（见第 11 节）

---

## 9. 模块与代码结构

### 9.1 Maven 工程

```
zSearch/
├── pom.xml                    # parent：JDK17、dependencyManagement、enforcer
│                              # <modules> 当前为 common / server；streaming 在 P2 加入
├── zsearch-common/            # 纯 Java，零框架依赖
├── zsearch-server/            # Spring Boot 控制面 + 查询面
├── zsearch-streaming/         # Flink 作业（shade 打包）
├── zsearch-frontend/          # Vue3（非 Maven 模块）
└── MediaCrawler-main/         # Python 采集（非 Maven 模块）
```

注意：

- 前端与采集端**不要**列入 parent 的 `<modules>`。
- Flink 1.15+ 发行包不自带 connector，作业依赖必须 shade 进 job jar。

### 9.2 zsearch-common

```
com.zst.zsearch.common
├── model/       IngestEnvelope, RawArticle, ArticleIndexDocument
├── enums/       SiteId
├── id/          ArticleIdGenerator, ContentFingerprint
├── enrich/      ArticleEnricher, 5 个实现, EnricherChain
├── contract/    Topics, SchemaVersion
└── json/        JsonCodec（FastJSON2 2.0.66）
```

约束：

- 不依赖 Spring、Flink、ES Client、MyBatis。
- POJO 必须有无参构造与 getter/setter，**不得使用 Java record**。
- 富化器不得有 IO，不得依赖时间/随机数（保证重放可复现）。

### 9.3 zsearch-streaming

```
com.zst.zsearch.streaming
├── IngestJob
├── config/JobOptions            # ParameterTool：group-id、from/to、index、pipeline-version
├── source/KafkaSourceFactory
├── source/ArticleRecordDeserializer
├── adapter/CollectorEventAdapter       # 原生事件 → IngestEnvelope，侧输出 DLQ
├── adapter/CollectorArticleMapper      # 平台 payload 映射（bili / dy content）
├── validate/ArticleValidator
├── validate/ArticleValidateFunction   # 解析 + 校验，侧输出 DLQ
├── model/IngestRecord
├── dedup/DedupFunction
├── enrich/EnrichFunction
├── sink/EsBulkSink
├── sink/EsBulkWriter
├── sink/EsClientFactory         # SNAKE_CASE JsonpMapper + basic auth
├── sink/EsSinkConfig
├── dlq/DlqSink
├── dlq/DlqProducer
├── dlq/DlqConfig
└── metrics/IngestMetrics
```

作业 jar 依赖：`flink-connector-kafka`（3.4.x-1.20）、`flink-connector-base`（1.20.1，发行包不含，必须 shade）、`elasticsearch-java 7.17.29`（含 `jakarta.json` 实现）、`commons-logging`（ES RestClient 运行时依赖）、`fastjson2`、`zsearch-common`。

运行方式（参数缺省值见 `JobOptions`）：

```bash
flink run -c com.zst.zsearch.streaming.IngestJob zsearch-streaming-1.0-SNAPSHOT-job.jar \
  --kafka.bootstrap.servers 8.163.135.98:9092 \
  --es.uris http://8.163.135.98:9200 \
  --pipeline.version 1
```

`--es.index` 缺省即 `zsearch_article`，无需显式传入。

重建时额外传 `--kafka.group.id zsearch-replay-<yyyyMMdd> --from.timestamp <ms> --to.timestamp <ms>`，且**不恢复状态**。

### 9.4 现有代码迁移映射

| 现有位置 | 目标 |
|---|---|
| `zsearch-server/.../pipeline/dto/RawArticle.java` | `common/model/RawArticle` |
| `zsearch-server/.../es/dto/ArticleIndexDocument.java` | `common/model/ArticleIndexDocument` |
| `zsearch-server/.../model/enums/SiteId.java` | `common/enums/SiteId` |
| `zsearch-server/.../es/util/ArticleIdGenerator.java` | `common/id/ArticleIdGenerator` |
| `zsearch-server/.../pipeline/enricher/*.java` | `common/enrich/*`（去掉 `@Component/@Order`） |
| `zsearch-server/.../pipeline/service/ArticleDocumentAssembler.java` | 由 `common/enrich/EnricherChain` 取代；server 侧如需保留，只做适配 |
| `zsearch-server/.../pipeline/service/ArticlePipelineWorker.java` | 迁移完成后下线 |
| `zsearch-server/.../es/service/ArticleExcelImportService.java` | 暂保留现状，后续决定是否走 Kafka（见第 13 节） |
| `zsearch-server/.../controller/ArticleIngestController.java` + `pipeline/service/ArticleIngestService.java` | 后续阶段改为 Kafka producer（本期不动） |

---

## 10. 部署与容量

### 10.1 部署顺序

1. Kafka 3.9.1 KRaft 单节点
2. 创建 topic（主 + DLQ）
3. ES 索引 `zsearch_article`（执行 `zsearch-server/src/main/resources/es/zsearch_article.http`）
4. 提交 Flink 作业
5. 接入监控

Kafka 单节点 KRaft 关键配置（Docker 示例）：

```yaml
services:
  kafka:
    image: apache/kafka:3.9.1
    ports: ["9092:9092"]
    environment:
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
      KAFKA_NODE_ID: "1"
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093"
      KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://8.163.135.98:9092"
      KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT"
      KAFKA_INTER_BROKER_LISTENER_NAME: "PLAINTEXT"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: "1"
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: "1"
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: "1"
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: "0"
      KAFKA_NUM_PARTITIONS: "6"
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
      KAFKA_LOG_RETENTION_HOURS: "168"
    volumes: ["./kafka-data:/var/lib/kafka/data"]
```

Flink 使用 `flink:1.20.1-scala_2.12-java17` 或 standalone 包；JDK 17 下本地 IDE 运行 MiniCluster 需补充 `--add-opens` 参数。

`RawArticle` / `ArticleIndexDocument` 的 `List<String>` 字段在 Flink 的 POJO 类型推导中会被降级为 `GenericType`，实际走 Kryo 序列化；chill 的 `ArraysAsListSerializer` 在初始化时会对 `java.util.Arrays$ArrayList.a` 做 `setAccessible(true)`，JDK 17 的模块封装会直接抛 `InaccessibleObjectException`。因此本地调试必须给 JVM 加 `--add-opens=java.base/java.util=ALL-UNNAMED`（其余参数沿用 Flink 发行版默认值以保持一致）。

- 本地 IDE：已提供共享运行配置 `.idea/runConfigurations/IngestJob_Local.xml`，在 Run/Debug Configurations 中选择 **IngestJob (Local MiniCluster)**；若继续用旧的临时配置，需在 VM options 中手工加入同样的参数。
- 集群：`flink-conf.yaml` 中设置 `env.java.opts.all`，值与上述 VM options 相同（Flink 官方要求默认列表只能追加、不能删减）。

最小可用参数（仅解决本问题）：

```text
--add-opens=java.base/java.util=ALL-UNNAMED
```


### 10.2 内存预算（192.168.88.101）

当前 8G：

| 组件 | 预算 | 关键参数 |
|---|---|---|
| ES 7.17.29 | 容器 2.0G / 堆 1.5G | 单节点、副本 0、`vm.max_map_count=262144` |
| Kafka 3.9.1 | 容器 1.5G / 堆 768M | 单节点 KRaft，其余靠页缓存 |
| Flink JobManager | 768M | REST 8081 |
| Flink TaskManager | 1.8G / 1 slot | 并行度 1 |
| MySQL | 约 700M | buffer pool 256-512M |
| OS / Docker | 约 1G | |
| 合计 | **≈ 7.8G** | 无余量，需限制容器内存防 OOM |

约定：`zsearch-server` **不在本机运行**（IDE 本地启动），否则需升级到 16G。

后续 16G：

| 组件 | 预算 |
|---|---|
| ES | 容器 5G / 堆 4G |
| Kafka | 容器 3G / 堆 1G |
| Flink JM | 1G |
| Flink TM | 4G / 2 slots，并行度 2-3 |
| MySQL | 1.5G |
| 其余 | 约 2.5G |

### 10.3 Checkpoint / 状态

```yaml
execution.checkpointing.interval: 60s
execution.checkpointing.min-pause: 30s
execution.checkpointing.timeout: 10min
execution.checkpointing.max-concurrent-checkpoints: 1
execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
state.backend.type: hashmap
state.checkpoints.dir: file:///data/flink/checkpoints
restart-strategy.type: fixed-delay
restart-strategy.fixed-delay.attempts: 3
restart-strategy.fixed-delay.delay: 10s
```

- 状态数据量到千万级 key 时切换 `rocksdb` + 增量 checkpoint。
- 升级作业：`flink stop` 打 savepoint → 换 jar → 从 savepoint 恢复。
- 重建作业：相反，**不恢复状态**。

---

## 11. 监控与告警

### 11.1 指标

- Kafka：分区 lag、消费速率、最早 offset 变化
- Flink：checkpoint 时长/失败次数、背压、重启次数、消费速率
- ES：bulk 耗时、429 重试次数、409 版本冲突数、写入失败数
- 业务：DLQ 增长量、单位时间内新文档数
- 作业：水位线/处理时间（可选）

### 11.2 告警线（7 天窗口下）

| 指标 | 阈值 | 级别 |
|---|---|---|
| 消费 lag | > 6 小时 | warning |
| 消费 lag | > 24 小时 | page |
| 作业停止 | > 30 分钟 | page |
| checkpoint 连续失败 | 3 次 | page |
| 分区最早 offset 突增 | 立即 | page |
| DLQ 有新增 | 有 | warning |
| 采集时段内无新文档入库 | > 30 分钟 | warning |

---

## 12. 迁移计划

| 阶段 | 内容 | 验收 |
|---|---|---|
| P0 | 新建 parent + `zsearch-common`；迁移 POJO/枚举/ID/富化器；server 依赖 common，行为不变；补 golden 测试（ID、哈希、富化） | **已完成**：`mvn test` 全绿（common 16 + server 79） |
| P1 | `zsearch-streaming` 骨架；本地 MiniCluster + Testcontainers（Kafka 3.9.1 / ES 7.17.29）跑通全链路；写入 `zsearch_article` | **代码已完成**：作业拓扑、自研 sink、DLQ、25 个单测、`*-job.jar` 打包通过；端到端 Testcontainers 与 7 条验收用例待补 |
| P2 | 停用 `ArticlePipelineWorker`；旧任务表停止写入，由 Flink 独占写 `zsearch_article` | 搜索/监测回归通过；回滚方案 = 停 Flink 作业并重启旧 poller |
| P3 | 监控告警、DLQ 重放工具、bounded 重建演练 | 强杀 TM 可恢复；7 天窗口内重建成功；超窗告警正确触发 |

### 12.1 验收用例

1. 投 N 条 → ES 恰好 N 篇
2. 同版本重投 → 文档不变、无报错
3. 先 rawVersion=200 后 rawVersion=100 → 409 被忽略，文档保持 200 的内容
4. 内容不变、版本 +1 → 不写 ES（去重生效），状态版本更新
5. 坏消息 → 进 DLQ，作业继续
6. kill TaskManager → 从 checkpoint 恢复，文档数正确
7. 重建作业新建消费组 + 空状态 → 全量写入新索引

---

## 13. 未决问题

| # | 问题 | 建议 |
|---|---|---|
| Q1 | `SiteId` 扩展：快手/贴吧/知乎的编码 | 建议 6=快手、7=贴吧、8=知乎，同时更新前端 `constants.js`，待确认 |
| Q2 | Excel 导入（`ArticleExcelImportService`）是否改走 Kafka | 建议后续统一走 Kafka（保证富化与版本号一致）；本期保留现状 |
| Q3 | 采集端生产者实现（`acks`、幂等、重试、spool） | 不在本期，待采集服务对接时确定 |
| Q4 | `zsearch-server` 最终部署位置（101 或独立机器） | 当前按「不在 101」做容量预算 |
| Q5 | ES 是否启用认证（当前 `application.yml` 账号密码为空） | 建议启用 basic auth，凭据走环境变量 |
| Q6 | 日均消息量（用于磁盘上限与保留期复核） | 7 天保留已定；量级超预期时用 `retention.bytes` 兜底 |
| Q7 | 历史数据迁移：现有 ES 文档是否需要回填 `rawVersion`/`contentHash` | 建议通过一次全量重建完成，不做原地更新 |

---

## 14. 术语表

| 术语 | 含义 |
|---|---|
| `articleId` | 文章稳定标识，`md5(url.trim())`，ES 文档 `_id` |
| `rawVersion` | 文章数据版本，毫秒时间戳，同一文章单调递增 |
| `pipelineVersion` | 加工作业版本，写入 ES 文档，用于区分不同加工逻辑产物 |
| `contentHash` | 内容指纹，用于判断“仅版本变化、内容未变” |
| `external_gte` | ES 外部版本模式：只接受大于等于当前版本的写入 |
| 有效一次 | at-least-once 传输 + 幂等写入，允许重复处理但结果收敛 |
| bounded source | 有界消费（指定起止 offset/时间），用于重建作业 |
| bounded source | 有界消费（指定起止 offset/时间），用于重建作业 |
| DLQ | 死信队列，存放无法处理的消息 |
| TTL | 状态存活时间，超时自动清理 |
