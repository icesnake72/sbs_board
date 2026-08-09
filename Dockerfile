# build 전용 스테이지
# FROM: 이미지이름:버전, 여기를 BASE로 두고 시작함
FROM eclipse-temurin:21-jdk AS build
# 작업 디렉토리 지정
WORKDIR /workspace

# gradlew settings.gradle build.gradle 파일들을 작업 디렉토리에 복사
COPY gradlew settings.gradle build.gradle ./

# gradle 디렉토리를 작업 디렉토리 밑에 같은 경로로 복사
COPY gradle ./gradle

# 복사된 gradlew 파일에 실행 권한을 부여함
# 빌드(build)시도 해보고, 출력 및 에러를 모두 버림(콘솔에 표시안함)
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# src 디렉토리를 작업 디렉토리 아래에 복사함
COPY src ./src

# 소스코드 복사 후 클린 빌드를 시도함(테스트 없음)
# plain 아카이브 생성하지 않음, gradle 데몬 생성하지 않음
RUN ./gradlew --no-daemon clean bootJar -x test

########################################################

# runtime stage
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# apt-get 프로그램 업데이트
# curl 설치
# curl 필요없어진 설치 파일 삭제
# 그룹 추가
# 사용자 추가
# /app/uploads 디렉토리 생성
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring \
    && mkdir -p /app/uploads

# build stage에서 만들어진 jar 파일을 app.jar 로 복사해옴
COPY --from=build /workspace/build/libs/*.jar app.jar

# /app 디렉토리에 그룹(spring)과 사용자(spring)에 소유권을 줌
RUN chown -R spring:spring /app

# 지금부터 실행은 spring user가 함
USER spring

# 환경변수 설정
ENV APP_UPLOAD_DIR=/app/uploads

# 포트번호 설정 (절대적인것은 아님)
EXPOSE 8090

# 실행 명령
ENTRYPOINT ["java", "-jar", "/app/app.jar"]