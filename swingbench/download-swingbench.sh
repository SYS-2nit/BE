#!/bin/bash

# SwingBench 및 Oracle JDBC Driver 다운로드 스크립트

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "SwingBench 설정 시작"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 디렉토리 생성
mkdir -p jar
mkdir -p scenarios
mkdir -p results

cd jar

# SwingBench jar 다운로드
echo ""
echo "📦 SwingBench jar 다운로드 중..."
if [ ! -f "swingbench.jar" ]; then
    curl -L -o swingbench.jar https://repo1.maven.org/maven2/net/sourceforge/swingbench/swingbench/2.7/swingbench-2.7.jar
    if [ $? -eq 0 ]; then
        echo "✅ SwingBench 다운로드 완료"
    else
        echo "❌ SwingBench 다운로드 실패"
    fi
else
    echo "ℹ️  SwingBench이 이미 다운로드되어 있습니다."
fi

# Oracle JDBC Driver 다운로드
echo ""
echo "📦 Oracle JDBC Driver 다운로드 중..."
if [ ! -f "ojdbc8.jar" ]; then
    # Oracle 공식 홈페이지에서 다운로드 권장
    echo "Oracle JDBC Driver는 아래 링크에서 수동으로 다운로드하세요:"
    echo "https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html"
    echo ""
    echo "또는 Gradle/Maven으로 의존성 추가하는 방법:"
    echo "- build.gradle에 의존성 추가:"
    echo "  implementation 'com.oracle.database.jdbc:ojdbc8:21.7.0.0'"
else
    echo "ℹ️  Oracle JDBC Driver가 이미 다운로드되어 있습니다."
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ 설정 완료"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

