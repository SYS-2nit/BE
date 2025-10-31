#!/bin/bash
# 연결 폭주: 신규 세션 생성/종료 반복

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

echo "[Logon Storm] Starting connection storm for ${DURATION} seconds"

# 짧은 세션 수명으로 연결 폭주 유도 (thinkTime 짧게, users 많게)
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam 15 \
  -tu "$DURATION" \
  -min 0 \
  -max 1 \
  -n "Logon Storm Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[Logon Storm] Completed successfully"
else
  echo "[Logon Storm] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

