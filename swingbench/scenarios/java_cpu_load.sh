#!/bin/bash

# Java 기반 CPU 부하 생성 스크립트
# 환경 변수: WHA_DB_URL, WHA_DB_USERNAME, WHA_DB_PASSWORD
# 인자: DURATION_SEC

DURATION=${1:-60}

if [ -z "$WHA_DB_URL" ] || [ -z "$WHA_DB_USERNAME" ] || [ -z "$WHA_DB_PASSWORD" ]; then
    echo "[JavaCpuLoad] 환경 변수가 설정되지 않았습니다."
    echo "[JavaCpuLoad] 필수 환경 변수: WHA_DB_URL, WHA_DB_USERNAME, WHA_DB_PASSWORD"
    exit 1
fi

# 프로젝트 루트 디렉토리
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT" || exit 1

# JAR 파일 경로 찾기 (Docker 환경: app.jar, 로컬: build/libs/*.jar)
if [ -f "app.jar" ]; then
    JAR_FILE="app.jar"
elif [ -f "build/libs/DBMonitor-0.0.1-SNAPSHOT.jar" ]; then
    JAR_FILE="build/libs/DBMonitor-0.0.1-SNAPSHOT.jar"
else
    # 동적으로 찾기
    JAR_FILE=$(find . -maxdepth 3 -name "DBMonitor-*.jar" -type f | head -n 1)
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "[JavaCpuLoad] JAR 파일을 찾을 수 없습니다. (찾은 경로: $JAR_FILE)"
    echo "[JavaCpuLoad] 현재 디렉토리: $(pwd)"
    exit 1
fi

echo "[JavaCpuLoad] 부하 생성 시작 - duration: ${DURATION}초"
echo "[JavaCpuLoad] JAR 파일: $JAR_FILE"
echo "[JavaCpuLoad] DB URL: $WHA_DB_URL"

# Spring Boot JAR는 특별한 구조를 가지고 있으므로, PropertiesLauncher를 사용하거나
# JAR를 압축 해제하여 클래스 경로를 직접 지정해야 합니다.
# 가장 간단한 방법: JAR를 임시 디렉토리에 압축 해제하고 클래스 경로 지정
TEMP_DIR=$(mktemp -d)
trap "rm -rf $TEMP_DIR" EXIT

# JAR 파일 압축 해제
cd "$TEMP_DIR" || exit 1
jar -xf "$PROJECT_ROOT/$JAR_FILE" || exit 1

# 클래스 경로 구성 (BOOT-INF/classes와 BOOT-INF/lib/*)
CLASSPATH="$TEMP_DIR/BOOT-INF/classes"
for jar in "$TEMP_DIR/BOOT-INF/lib"/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# Java 프로세스로 실행 (별도 프로세스로 분리)
java -cp "$CLASSPATH" \
    com.sys.dbmonitor.domains.diagnosis.runners.JavaCpuLoadMain \
    "$WHA_DB_URL" \
    "$WHA_DB_USERNAME" \
    "$WHA_DB_PASSWORD" \
    "$DURATION"

EXIT_CODE=$?
if [ $EXIT_CODE -eq 0 ]; then
    echo "[JavaCpuLoad] 부하 생성 완료"
else
    echo "[JavaCpuLoad] 부하 생성 실패 (exit code: $EXIT_CODE)"
fi

exit $EXIT_CODE

