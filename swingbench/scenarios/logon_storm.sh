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

# CPU 코어 수 확인하여 사용자 수 동적 설정
CORES=$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 2)
# Host CPU 부하를 높이기 위해 사용자 수 증가 (코어 수의 4배)
USERS=$((CORES * 4))
# 최소 사용자 수 보장 (최소 15명)
if [ $USERS -lt 15 ]; then
  USERS=15
fi

echo "[Logon Storm] Users: ${USERS}, Cores: ${CORES}"

# 짧은 세션 수명으로 연결 폭주 유도 (thinkTime 짧게, users 많게)
# CPU 과부하를 지속적으로 유지하기 위해 루프로 실행
START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))
ITERATION_DURATION=15
EXIT_CODE=0

while [ $(date +%s) -lt $END_TIME ]; do
  REMAINING=$((END_TIME - $(date +%s)))
  CURRENT_ITER_DURATION=$ITERATION_DURATION
  
  if [ $REMAINING -lt $CURRENT_ITER_DURATION ]; then
    CURRENT_ITER_DURATION=$REMAINING
  fi
  
  if [ $CURRENT_ITER_DURATION -le 0 ]; then
    break
  fi
  
  echo "[Logon Storm] Running iteration (${CURRENT_ITER_DURATION}s, remaining: ${REMAINING}s)"
  
  java -jar "$SWINGBENCH_JAR" \
    -cs "$CS" \
    -u "$USER" \
    -p "$PASS" \
    -soe \
    -uam "$USERS" \
    -tu "$CURRENT_ITER_DURATION" \
    -min 0 \
    -max 1 \
    -n "Logon Storm Test" 2>&1
  
  ITER_EXIT=$?
  if [ $ITER_EXIT -ne 0 ]; then
    EXIT_CODE=$ITER_EXIT
    echo "[Logon Storm] Iteration failed with exit code: $ITER_EXIT"
  fi
  
  sleep 1
done

if [ $EXIT_CODE -eq 0 ]; then
  echo "[Logon Storm] Completed successfully (${DURATION}s duration maintained)"
else
  echo "[Logon Storm] Completed with exit code: $EXIT_CODE (${DURATION}s duration maintained)"
fi
exit $EXIT_CODE

