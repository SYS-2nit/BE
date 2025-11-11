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
# CPU 과부하를 강하게 하기 위해 코어 수의 6배로 사용자 수 설정 (기존 2배에서 증가)
USERS=$((CORES * 6))
# 최소 사용자 수 보장 (최소 20명)
if [ $USERS -lt 20 ]; then
  USERS=20
fi

echo "[CPU Stress] Starting Order Entry scenario for ${DURATION} seconds"
echo "[CPU Stress] Users: ${USERS}, Cores: ${CORES}, Connection: ${CS}"

# Host CPU Utilization을 높이기 위해 호스트 레벨 CPU 부하 생성
# 코어 수의 2배만큼 백그라운드 프로세스 실행하여 호스트 CPU 직접 사용
HOST_CPU_PROCESSES=$((CORES * 2))
# 최소 4개 프로세스 보장
if [ $HOST_CPU_PROCESSES -lt 4 ]; then
  HOST_CPU_PROCESSES=4
fi

HOST_CPU_PIDS=()
echo "[CPU Stress] Starting host-level CPU load processes (${HOST_CPU_PROCESSES} processes for ${CORES} cores)"

# CPU 집약적인 백그라운드 프로세스 실행
for ((i=0; i<HOST_CPU_PROCESSES; i++)); do
  # CPU 집약적인 작업을 백그라운드로 실행 (무한 루프로 CPU 사용)
  (while true; do :; done) &
  HOST_CPU_PIDS+=($!)
done

echo "[CPU Stress] Host CPU load processes started (PIDs: ${HOST_CPU_PIDS[*]})"

# 종료 시 호스트 CPU 프로세스 정리 함수
cleanup_host_cpu() {
  echo "[CPU Stress] Stopping host CPU load processes"
  for pid in "${HOST_CPU_PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  wait "${HOST_CPU_PIDS[@]}" 2>/dev/null || true
}

# 스크립트 종료 시 정리
trap cleanup_host_cpu EXIT INT TERM

# CPU 과부하를 지속적으로 유지하기 위해 루프로 실행
# 각 SwingBench 실행은 15초씩 실행하고, 전체 DURATION 동안 반복
START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))
ITERATION_DURATION=15  # 각 반복당 15초 실행
EXIT_CODE=0

while [ $(date +%s) -lt $END_TIME ]; do
  REMAINING=$((END_TIME - $(date +%s)))
  CURRENT_ITER_DURATION=$ITERATION_DURATION
  
  # 남은 시간이 반복 시간보다 짧으면 남은 시간만큼만 실행
  if [ $REMAINING -lt $CURRENT_ITER_DURATION ]; then
    CURRENT_ITER_DURATION=$REMAINING
  fi
  
  if [ $CURRENT_ITER_DURATION -le 0 ]; then
    break
  fi
  
  echo "[CPU Stress] Running iteration (${CURRENT_ITER_DURATION}s, remaining: ${REMAINING}s)"
  
  # Order Entry 시나리오 실행 (users 증가, thinkTime=0)
  java -jar "$SWINGBENCH_JAR" \
    -cs "$CS" \
    -u "$USER" \
    -p "$PASS" \
    -soe \
    -uam "$USERS" \
    -tu "$CURRENT_ITER_DURATION" \
    -min 0 \
    -max 0 \
    -n "CPU Stress Test" 2>&1
  
  ITER_EXIT=$?
  if [ $ITER_EXIT -ne 0 ]; then
    EXIT_CODE=$ITER_EXIT
    echo "[CPU Stress] Iteration failed with exit code: $ITER_EXIT"
    # 오류가 발생해도 계속 실행하여 지속적인 부하 유지
  fi
  
  # 다음 반복 전 짧은 대기 (1초) - 부하가 끊기지 않도록
  sleep 1
done

# 호스트 CPU 프로세스 정리
cleanup_host_cpu

if [ $EXIT_CODE -eq 0 ]; then
  echo "[CPU Stress] Completed successfully (${DURATION}s duration maintained)"
else
  echo "[CPU Stress] Completed with exit code: $EXIT_CODE (${DURATION}s duration maintained)"
fi
exit $EXIT_CODE

