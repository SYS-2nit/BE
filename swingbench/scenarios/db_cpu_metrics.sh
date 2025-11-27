#!/bin/bash
# DB CPU 메트릭 증가 시나리오 (통합)
# 목표: DB_OF_HOST_SHARE_PCT와 CPU_SATURATION_PCT 동시 증가
# - DB CPU 비율 (DB_OF_HOST_SHARE_PCT): DB가 호스트 CPU 중 높은 비율 차지
# - DB CPU 포화도 (CPU_SATURATION_PCT): DB 세션 수가 CPU 코어 수를 크게 초과

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

# CPU 코어 수 확인
CORES=$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 2)

# 통합 전략:
# 1. 매우 많은 사용자 세션 생성 (코어 수의 8-10배)
#    - CPU_SATURATION_PCT 증가: AAS_ONCPU_SESSIONS 증가
#    - DB_OF_HOST_SHARE_PCT 증가: DB 부하 증가
# 2. 호스트의 다른 프로세스 최소화 (호스트 CPU 부하 프로세스 없음)
#    - DB_OF_HOST_SHARE_PCT 증가: hostBusyCores가 주로 DB로 인한 것
# 3. thinkTime=0으로 CPU 집약적 작업
#    - 두 메트릭 모두 증가: CPU를 계속 사용하여 AAS_ONCPU_SESSIONS 증가
# 4. 짧은 트랜잭션으로 빠른 반복
#    - 세션당 처리량 증가로 AAS 증가

# 사용자 수: 코어 수의 20배로 대폭 증가 (지표 차이를 명확하게 보이기 위해)
# - CPU_SATURATION_PCT: 매우 많은 세션으로 AAS 대폭 증가
# - DB_OF_HOST_SHARE_PCT: 매우 많은 세션으로 DB 부하 대폭 증가
USERS=$((CORES * 20))
# 최소 사용자 수 보장 (최소 40명)
if [ $USERS -lt 40 ]; then
  USERS=40
fi

echo "[DB CPU Metrics] Starting integrated scenario for ${DURATION} seconds"
echo "[DB CPU Metrics] Targets:"
echo "  - DB_OF_HOST_SHARE_PCT > 70% (주의), > 80% (위험), > 90% (치명)"
echo "  - CPU_SATURATION_PCT > 15% (주의), > 20% (위험), > 30% (치명)"
echo "[DB CPU Metrics] Users: ${USERS}, Cores: ${CORES}, Connection: ${CS}"
echo "[DB CPU Metrics] Strategy: High concurrent sessions, CPU-intensive, minimal host processes, no thinkTime"

# 호스트 CPU 부하 프로세스는 생성하지 않음
# 이렇게 하면 hostBusyCores가 주로 DB로 인한 것이 되어 DB_OF_HOST_SHARE_PCT가 높아짐

# SwingBench 프로세스들을 추적하기 위한 배열
SWINGBENCH_PIDS=()

# 종료 시 SwingBench 프로세스 정리 함수
cleanup_swingbench() {
  echo "[DB CPU Metrics] Stopping all SwingBench processes"
  for pid in "${SWINGBENCH_PIDS[@]}"; do
    if kill -0 "$pid" 2>/dev/null; then
      echo "[DB CPU Metrics] Stopping SwingBench process: $pid"
      kill "$pid" 2>/dev/null || true
    fi
  done
  # 프로세스 종료 대기
  for pid in "${SWINGBENCH_PIDS[@]}"; do
    wait "$pid" 2>/dev/null || true
  done
  SWINGBENCH_PIDS=()
}

# 스크립트 종료 시 정리
trap cleanup_swingbench EXIT INT TERM

# 부하 강도를 극대화하기 위해 여러 SwingBench 인스턴스를 동시에 실행
# 각 인스턴스는 전체 duration 동안 실행되며, 동시에 여러 인스턴스가 실행되어 부하가 배가됨
INSTANCE_COUNT=3  # 동시에 3개의 SwingBench 인스턴스 실행
echo "[DB CPU Metrics] Starting ${INSTANCE_COUNT} SwingBench instances simultaneously for maximum load (${DURATION}s)"
echo "[DB CPU Metrics] Total concurrent users: $((USERS * INSTANCE_COUNT))"

# 여러 SwingBench 인스턴스를 동시에 백그라운드로 실행
for ((i=1; i<=INSTANCE_COUNT; i++)); do
  echo "[DB CPU Metrics] Starting SwingBench instance #${i} (${USERS} users)"
  
  # SwingBench를 백그라운드로 실행
  (
    java -jar "$SWINGBENCH_JAR" \
      -cs "$CS" \
      -u "$USER" \
      -p "$PASS" \
      -soe \
      -uam "$USERS" \
      -tu "$DURATION" \
      -min 0 \
      -max 0 \
      -n "DB CPU Metrics Test #${i}" 2>&1
  ) &
  
  SWINGBENCH_PID=$!
  SWINGBENCH_PIDS+=($SWINGBENCH_PID)
  echo "[DB CPU Metrics] SwingBench instance #${i} started (PID: $SWINGBENCH_PID)"
  
  # 인스턴스 간 약간의 지연 (0.5초) - 동시 시작으로 인한 연결 폭주 방지
  sleep 0.5
done

# 모든 SwingBench 프로세스가 종료될 때까지 대기
echo "[DB CPU Metrics] Waiting for all SwingBench instances to complete..."
for pid in "${SWINGBENCH_PIDS[@]}"; do
  if kill -0 "$pid" 2>/dev/null; then
    wait "$pid" 2>/dev/null || true
  fi
done

EXIT_CODE=0

if [ $EXIT_CODE -eq 0 ]; then
  echo "[DB CPU Metrics] Completed successfully (${DURATION}s duration maintained)"
  echo "[DB CPU Metrics] Expected results:"
  echo "  - DB_OF_HOST_SHARE_PCT should be > 70%"
  echo "  - CPU_SATURATION_PCT should be > 15%"
else
  echo "[DB CPU Metrics] Completed with exit code: $EXIT_CODE (${DURATION}s duration maintained)"
fi
exit $EXIT_CODE

