#!/bin/bash
# 라이브러리 캐시 압박: literal SQL 반복 실행 (하드파스 유도)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[LibCache Pressure] Starting literal SQL execution for ${DURATION} seconds"

# Sales Order Entry 시나리오로 다양한 쿼리 실행 (literal SQL 패턴)
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 10 \
  -tu "$DURATION" \
  -min 1 \
  -max 5 \
  -n "LibCache Pressure Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[LibCache Pressure] Completed successfully"
else
  echo "[LibCache Pressure] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

