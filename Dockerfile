#--------------------------------------------
#-- 작성자 배지원
#--------------------------------------------
FROM eclipse-temurin:17-jre

# bash 설치 (스크립트 실행용)
RUN apt-get update && \
    apt-get install -y bash curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# JAR 파일 복사
COPY ./build/libs/*SNAPSHOT.jar app.jar

# SwingBench 스크립트 복사
COPY ./swingbench/scenarios /app/swingbench/scenarios
RUN chmod +x /app/swingbench/scenarios/*.sh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]