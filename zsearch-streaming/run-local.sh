#!/usr/bin/env bash
# 本地 MiniCluster 启动数据流作业，日志同时输出到控制台和 target/ingest.log
# 用法：bash zsearch-streaming/run-local.sh [额外的 --参数]
# 可通过环境变量覆盖：KAFKA_BOOTSTRAP / KAFKA_TOPIC / ES_URIS / ES_INDEX / PARALLELISM / CHECKPOINT_DIR / ZSEARCH_LOG_LEVEL
set -euo pipefail
cd "$(dirname "$0")"

BOOTSTRAP="${KAFKA_BOOTSTRAP:-8.163.135.98:9092}"
KAFKA_TOPIC="${KAFKA_TOPIC:-search-topic}"
ES_URIS="${ES_URIS:-http://8.163.135.98:9200}"
ES_INDEX="${ES_INDEX:-zsearch_article}"
PARALLELISM="${PARALLELISM:-1}"
ZSEARCH_LOG_LEVEL="${ZSEARCH_LOG_LEVEL:-info}"
if [ -z "${CHECKPOINT_DIR:-}" ]; then
  CHECKPOINT_DIR="file:///$(cygpath -m "$PWD" 2>/dev/null || pwd)/target/checkpoints"
fi

if [ ! -s target/cp.txt ] || [ pom.xml -nt target/cp.txt ]; then
  echo "[run-local] resolving classpath ..."
  mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=test
fi
CP="target/classes;../zsearch-common/target/classes;$(cat target/cp.txt)"

echo "[run-local] kafka=$BOOTSTRAP topic=$KAFKA_TOPIC es=$ES_URIS index=$ES_INDEX log-level=$ZSEARCH_LOG_LEVEL"
echo "[run-local] 逐条消费日志: ZSEARCH_LOG_LEVEL=debug bash zsearch-streaming/run-local.sh"
echo "[run-local] log=target/ingest.log  (Ctrl+C 停止)"

java -Dorg.slf4j.simpleLogger.defaultLogLevel=info \
  -Dorg.slf4j.simpleLogger.log.org.apache.kafka=warn \
  -Dorg.slf4j.simpleLogger.log.com.zst.zsearch="$ZSEARCH_LOG_LEVEL" \
  -Dorg.slf4j.simpleLogger.showDateTime=true \
  --add-opens java.base/java.util=ALL-UNNAMED \
  --add-opens java.base/java.lang=ALL-UNNAMED \
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED \
  --add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED \
  --add-opens java.base/java.text=ALL-UNNAMED \
  --add-opens java.base/java.time=ALL-UNNAMED \
  --add-opens java.base/java.math=ALL-UNNAMED \
  --add-opens java.base/java.io=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  --add-opens java.base/java.nio=ALL-UNNAMED \
  -cp "$CP" com.zst.zsearch.streaming.IngestJob \
  --kafka.bootstrap.servers "$BOOTSTRAP" \
  --kafka.topic "$KAFKA_TOPIC" \
  --es.uris "$ES_URIS" \
  --es.index "$ES_INDEX" \
  --checkpoint.dir "$CHECKPOINT_DIR" \
  --parallelism "$PARALLELISM" \
  "$@" 2>&1 | tee -a target/ingest.log
