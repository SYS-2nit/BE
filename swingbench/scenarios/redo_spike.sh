#!/bin/bash
# Redo 폭증: 대량 DML 실행 (짧은 커밋 간격)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Redo Spike] Starting heavy DML for ${DURATION} seconds"

# 쓰기 집중 시나리오 (Order Entry의 INSERT/UPDATE 중심)
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 8 \
  -tu "$DURATION" \
  -min 0 \
  -max 1 \
  -n "Redo Spike Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Redo Spike] Completed successfully"
else
  echo "[Redo Spike] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

