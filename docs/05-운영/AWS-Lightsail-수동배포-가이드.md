---
title: AWS Lightsail 수동 배포 가이드
tags: [ops, deploy, aws, docker]
updated: 2026-08-22
---
cu
# AWS Lightsail 수동 배포 가이드

Docker Compose 구성(백엔드 Spring + 프론트엔드 + MySQL)을 Lightsail 인스턴스 1대에 수동으로 올리는 순서.

## 구성 개요

| 컨테이너 | 이미지 | 포트 | 비고 |
|---|---|---|---|
| `board-back-end` | `board:latest` (루트 `Dockerfile`) | 8090 (내부) | Spring Boot |
| `board-fe-react` | `board-fe-react` (`frontend/Dockerfile`) | 80 → 공개 | nginx + React 정적 서빙 + `/api` 프록시 |
| `board-fe` | `board-fe` (`frontend_vanilla/Dockerfile`) | 3000 | 선택 — 안 쓰면 배포에서 제외 |
| `mysql8` | `mysql:8` | 3306 (내부) | **compose에 없음. 별도로 직접 띄워야 함** |

세 컨테이너는 외부 네트워크 `board-net`으로 통신한다. 브라우저 → nginx(80) → `board-back-end:8090` 경로이므로
**백엔드/DB 포트는 외부에 열지 않는다.**

---

## 1. 인스턴스 생성

1. Lightsail 콘솔 → **Create instance**
2. 리전: `Seoul (ap-northeast-2)`
3. 플랫폼: **Linux/Unix** → 블루프린트: **OS Only → Ubuntu 22.04 LTS**
4. 플랜: **최소 2GB RAM 이상** 권장
   - 인스턴스에서 Gradle/npm 빌드를 하기 때문에 1GB는 OOM으로 빌드가 죽는다.
   - 1GB로 가야 한다면 아래 [스왑 추가](#부록-a-스왑-메모리-추가) 또는 [로컬 빌드 후 이미지 전송](#부록-b-로컬에서-빌드해-올리기) 방식 사용.
5. SSH 키페어 다운로드 (`.pem` 보관)

## 2. 고정 IP · 방화벽

1. **Networking → Attach static IP** — 인스턴스 재시작 시 IP 변경 방지 (OAuth redirect URI가 IP 기반이면 필수)
2. **Networking → IPv4 Firewall** 규칙:

| Application | Protocol | Port | 용도 |
|---|---|---|---|
| SSH | TCP | 22 | 관리 접속 (가능하면 내 IP로 제한) |
| HTTP | TCP | 80 | 서비스 공개 |
| HTTPS | TCP | 443 | 도메인 + TLS 사용 시 |

`8090`, `3306`, `3000`은 **열지 않는다.**

## 3. 서버 접속 & Docker 설치

```bash
ssh -i board-key.pem ubuntu@<STATIC_IP>

sudo apt-get update && sudo apt-get upgrade -y

# Docker Engine + Compose plugin
curl -fsSL https://get.docker.com | sudo sh

# sudo 없이 docker 사용
sudo usermod -aG docker ubuntu
exit          # 재접속해야 그룹 반영됨
```

재접속 후 확인:

```bash
docker --version
docker compose version
```

## 4. 소스 배포

```bash
git clone <저장소 URL> ~/board
cd ~/board
```

비공개 저장소면 배포 키를 등록하거나, 로컬에서 전송:

```bash
# 로컬(Windows PowerShell)에서
scp -i board-key.pem -r C:\Users\User\Documents\backend\board ubuntu@<STATIC_IP>:~/board
```

## 5. 네트워크 · MySQL 컨테이너 준비

`docker-compose.yml`의 `board-net`은 `external: true`이고 MySQL 서비스는 정의되어 있지 않다.
**둘 다 먼저 수동 생성한다.**

```bash
docker network create board-net

docker run -d \
  --name mysql8 \
  --network board-net \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD='<강한_비밀번호>' \
  -e MYSQL_DATABASE=board \
  -e TZ=Asia/Seoul \
  -v mysql8-data:/var/lib/mysql \
  mysql:8 \
  --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

- 포트 `-p 3306:3306`은 **매핑하지 않는다** (외부 노출 금지). 백엔드는 같은 네트워크에서 `mysql8:3306`으로 접속한다.
- 데이터는 `mysql8-data` 볼륨에 남으므로 컨테이너를 지워도 유지된다.
- 테이블은 JPA `ddl-auto: update`로 자동 생성된다.

기동 확인:

```bash
docker logs -f mysql8      # "ready for connections" 확인 후 Ctrl+C
```

## 6. `.env` 작성

프로젝트 루트(`~/board/.env`)에 생성. → [[환경변수]]

```properties
DB_NAME=board
DB_USERNAME=root
DB_PASSWORD=<5단계에서_설정한_비밀번호>

APP_UPLOAD_DIR=/app/uploads
JWT_SECRET=<충분히_긴_랜덤_문자열>

# 브라우저가 실제로 보는 공개 주소. OAuth redirect-uri가 이 값으로 조립된다.
APP_PUBLIC_BASE_URL=http://<STATIC_IP>      # 도메인이 있으면 https://board.example.com

KAKAO_REST_API=<카카오_REST_API_키>
KAKAO_SECRET=<카카오_시크릿>
KAKAO_CALLBACK=http://<STATIC_IP>/api/oauth/kakao/callback
GOOGLE_CLIENT_ID=<구글_클라이언트_ID>
GOOGLE_CLIENT_SECRET=<구글_시크릿>
```

⚠️ 카카오 3개 키는 값이 없으면 `@PostConstruct` 검증에서 걸려 **앱이 기동되지 않는다.** 안 쓰더라도 더미 값이라도 채운다.

**소셜 로그인 콘솔 설정도 함께 변경한다:**
- 카카오 개발자 콘솔 → Redirect URI에 `http://<STATIC_IP>/api/oauth/kakao/callback` 추가
- Google Cloud Console → 승인된 리디렉션 URI에 `http://<STATIC_IP>/login/oauth2/code/google` 추가

## 7. `docker-compose.yml` 배포용 수정

서버에서 다음 두 곳을 손본다.

```yaml
  app:
    # ports:              ← 삭제 (백엔드는 board-net 내부에서만 접근)
    #   - "8090:8090"
```

```yaml
  frontend:               # frontend_vanilla — 배포에 불필요하면 통째로 삭제
```

React를 공개 서비스로 쓰므로 `frontend2`(`board-fe-react`)의 `80:80` 매핑은 그대로 둔다.
vanilla를 함께 띄운다면 `3000` 포트는 방화벽에서 닫힌 상태이므로 외부에서는 보이지 않는다.

## 8. 빌드 & 기동

```bash
cd ~/board
docker compose up -d --build          # 첫 빌드는 Gradle/npm 의존성 다운로드로 5~15분 소요

docker compose ps
docker compose logs -f app
```

`app` 컨테이너가 `healthy`가 되어야 프론트 컨테이너가 뜬다 (`depends_on: service_healthy`).
헬스체크는 `GET /api/board/all` (`start_period` 40초).

## 9. 동작 확인

```bash
curl -I http://localhost                     # nginx 200
curl -s http://localhost/api/board/all       # 프록시 → 백엔드 응답
```

브라우저에서 `http://<STATIC_IP>` 접속 → 목록 조회 · 로그인 · 파일 업로드까지 확인.

## 10. (선택) 도메인 + HTTPS

1. 도메인 A 레코드 → 고정 IP
2. `.env`의 `APP_PUBLIC_BASE_URL`을 `https://도메인`으로 변경, OAuth 콘솔 URI도 https로 갱신
3. TLS 적용 방법 중 택1
   - **Lightsail Load Balancer**: 콘솔에서 인증서 발급·연결 (가장 간단, 월 과금)
   - **호스트 nginx + certbot**: 호스트에 nginx 설치 → 443 종료 후 `proxy_pass http://127.0.0.1:80`, `certbot --nginx`로 인증서 발급/자동 갱신
4. 변경 후 `docker compose up -d` 재기동

---

## 재배포 (코드 수정 후)

```bash
cd ~/board
git pull
docker compose up -d --build
docker image prune -f          # 오래된 dangling 이미지 정리
```

## 백업

```bash
# DB 덤프
docker exec mysql8 mysqldump -uroot -p'<비밀번호>' board > ~/backup/board-$(date +%F).sql

# 업로드 파일 (board_uploads 볼륨)
docker run --rm -v board_uploads:/data -v ~/backup:/backup alpine \
  tar czf /backup/uploads-$(date +%F).tar.gz -C /data .
```

## 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| `network board-net not found` | 5단계의 `docker network create board-net` 누락 |
| 빌드 중 프로세스가 죽음 (`Killed`) | 메모리 부족 → 부록 A 스왑 추가 또는 부록 B 방식 |
| `app`이 계속 `unhealthy` | `docker compose logs app` — DB 접속 실패(비밀번호/`mysql8` 이름) 또는 카카오 키 누락 |
| 프론트는 뜨는데 `/api` 502 | 백엔드 미기동 또는 컨테이너 이름 불일치 (nginx.conf가 `board-back-end`를 참조) |
| 소셜 로그인 후 redirect 오류 | `APP_PUBLIC_BASE_URL`과 OAuth 콘솔의 Redirect URI 불일치 |
| 재부팅 후 서비스 없음 | `restart: unless-stopped`로 자동 복구됨. `mysql8`도 같은 옵션인지 확인 |

---

## 부록 A. 스왑 메모리 추가

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

## 부록 B. 로컬에서 빌드해 올리기

서버 사양이 낮을 때. 로컬에서 이미지를 만들어 tar로 전송한다.

```bash
# 로컬
docker compose build
docker save board:latest board-fe-react | gzip > images.tar.gz
scp -i board-key.pem images.tar.gz ubuntu@<STATIC_IP>:~/

# 서버
gunzip -c ~/images.tar.gz | docker load
docker compose up -d          # --build 없이 기동
```

또는 Docker Hub / ECR에 push 후 서버에서 pull.
