#!/bin/bash
# 잠금 경합: 동시 UPDATE (TX/TM Lock)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Lock Contention] Starting concurrent updates for ${DURATION} seconds"

# 동일 리소스에 대한 동시 업데이트로 잠금 경합 유도
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 8 \
  -tu "$DURATION" \
  -min 1 \
  -max 3 \
  -n "Lock Contention Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Lock Contention] Completed successfully"
else
  echo "[Lock Contention] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

