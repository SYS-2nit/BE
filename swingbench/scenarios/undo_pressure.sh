#!/bin/bash
# Undo 포화: 롱 트랜잭션 실행 (커밋 지연)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Undo Pressure] Starting long transaction for ${DURATION} seconds"

# 롱 트랜잭션 시나리오 (thinkTime 증가로 트랜잭션 지속)
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 3 \
  -tu "$DURATION" \
  -min 5 \
  -max 10 \
  -n "Undo Pressure Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Undo Pressure] Completed successfully"
else
  echo "[Undo Pressure] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

