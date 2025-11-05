#!/bin/bash
# Buffer Cache 미스: 대용량 Full Scan 쿼리 반복

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Buffer Cache Miss] Starting full scan queries for ${DURATION} seconds"

# Order Entry 시나리오로 대용량 테이블 접근 유도
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 5 \
  -tu "$DURATION" \
  -min 0 \
  -max 0 \
  -n "Buffer Cache Miss Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Buffer Cache Miss] Completed successfully"
else
  echo "[Buffer Cache Miss] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

