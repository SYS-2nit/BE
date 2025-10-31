#!/bin/bash
# 체크포인트/DBWR: 단시간 대량 DML (버스트 패턴)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Checkpoint/DBWR] Starting burst DML for ${DURATION} seconds"

# 단시간 집중 부하로 체크포인트 유도
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 6 \
  -tu "$DURATION" \
  -min 0 \
  -max 0 \
  -n "Checkpoint/DBWR Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Checkpoint/DBWR] Completed successfully"
else
  echo "[Checkpoint/DBWR] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

