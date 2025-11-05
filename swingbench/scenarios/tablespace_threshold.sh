#!/bin/bash
# 스토리지 용량 임계: 대량 INSERT

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Tablespace Threshold] Starting bulk inserts for ${DURATION} seconds"

# 대량 INSERT로 테이블스페이스 사용률 증가
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 5 \
  -tu "$DURATION" \
  -min 0 \
  -max 2 \
  -n "Tablespace Threshold Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Tablespace Threshold] Completed successfully"
else
  echo "[Tablespace Threshold] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

