#!/bin/bash
# CPU 포화 시나리오: Order Entry 시나리오 실행 (users 증가, thinkTime=0)

DURATION=${1:-60}
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SWINGBENCH_JAR=${SWINGBENCH_JAR:-"$SCRIPT_DIR/../jar/swingbench.jar"}
CS=${WHA_DB_URL:-${DB_CONNECTION_STRING:-localhost:1521:XE}}
USER=${WHA_DB_USERNAME:-${DB_USER:-system}}
PASS=${WHA_DB_PASSWORD:-${DB_PASSWORD:-password}}

# URL에서 호스트:포트:SID 추출 (jdbc:oracle:thin:@ 형식일 경우)
if [[ "$CS" == jdbc:* ]]; then
  CS=$(echo "$CS" | sed -E 's|jdbc:oracle:thin:@(.+)|\1|')
fi

# CPU 코어 수 확인 (최소 2, 기본 4 users)
CORES=$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 2)
USERS=$((CORES * 2))

echo "[CPU Stress] Starting Order Entry scenario for ${DURATION} seconds"
echo "[CPU Stress] Users: ${USERS}, Cores: ${CORES}, Connection: ${CS}"

# Order Entry 시나리오 실행 (users 증가, thinkTime=0)
java -jar "$SWINGBENCH_JAR" \
  -cs "$CS" \
  -u "$USER" \
  -p "$PASS" \
  -soe \
  -uam "$USERS" \
  -tu "$DURATION" \
  -min 0 \
  -max 0 \
  -n "CPU Stress Test" 2>&1

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
  echo "[CPU Stress] Completed successfully"
else
  echo "[CPU Stress] Completed with exit code: $EXIT_CODE"
fi
exit $EXIT_CODE

