# OpenJDK 17을 기반으로 한 Docker 이미지 사용
FROM openjdk:17-jdk-slim

# Tesseract와 OpenCV 설치
RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-kor tesseract-ocr-eng libtesseract-dev

# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트의 JAR 파일을 컨테이너에 복사
COPY build/libs/resume-feedback-0.0.1-SNAPSHOT.jar /app/app.jar

# 컨테이너가 실행될 때 실행할 명령어
# ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# ENTRYPOINT ["java", "$CATALINA_OPTS", "-jar", "/app/app.jar"]
CMD java \
-Dcom.sun.management.jmxremote.local.only=false \
-Dcom.sun.management.jmxremote=true \
-Dcom.sun.management.jmxremote.rmi.port=1099 \
-Dcom.sun.management.jmxremote.port=1099 \
-Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.authenticate=false \
-Djava.rmi.server.hostname=98.83.237.21 \
-jar /app/app.jar

