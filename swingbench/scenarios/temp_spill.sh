#!/bin/bash
# Temp 스필: 대형 Sort/Hash Join (PGA 부족 유도)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Temp Spill] Starting large sort/hash joins for ${DURATION} seconds"

# 복잡한 쿼리 패턴으로 Sort/Hash Join 유도
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 4 \
  -tu "$DURATION" \
  -min 2 \
  -max 5 \
  -n "Temp Spill Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Temp Spill] Completed successfully"
else
  echo "[Temp Spill] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

