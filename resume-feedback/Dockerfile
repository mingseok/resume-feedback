# OpenJDK 17을 기반으로 한 Docker 이미지 사용
FROM openjdk:17-jdk-slim


# -- 오류 뜬다면 이걸 삭제하기 --
# Tesseract와 OpenCV 설치
RUN apt-get update && apt-get install -y tesseract-ocr libtesseract-dev libopencv-dev


# 작업 디렉토리 설정
WORKDIR /app

# 프로젝트의 JAR 파일을 컨테이너에 복사
COPY build/libs/resume-feedback-0.0.1-SNAPSHOT.jar /app/app.jar

# 컨테이너가 실행될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
