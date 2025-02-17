## OpenJDK 21 사용
FROM openjdk:21-jdk-slim

# 컨테이너 내의 작업 디렉토리 설정(이후의 파일들은 이 작업 디렉토리 안에서 작동)
WORKDIR /app

# JAR 파일 복사 <호스트 파일 경로> <컨테이너 내부 경로, 지금은 컨테이너 내의 작업 디렉토리를 지정했으니, 그 지점을 기준으로 상대경로로 파일 위치 설정>
COPY build/libs/*.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]