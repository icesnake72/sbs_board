set -euo pipefail

echo ".env 생성"
cat > .env << EOF
KAKAO_REST_API=${KAKAO_REST_API}
KAKAO_SECRET=${KAKAO_SECRET}
GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
SITE_ADDRESS=sbs.alldayai.org
APP_REFRESH_COOKIE=true
EOF

echo "mysql8준비 및 도커 네트워크 생성"
docker network create board-db-net 2>/dev/null || true
docker start mysql-8 2>/dev/null || docker run -d --name mysql-8 \
  --network board-db-net \
  -e MYSQL_ROOT_PASSWORD=${DB_PASSWORD} \
  -e MYSQL_DATABASE=${DB_NAME} \
  -v mysql-8-data:/var/lib/mysql \
  mysql:8.0.33 --default-time-zone=+09:00
docker network connect board-db-net mysql-8 2>/dev/null || true

echo "DB 응답 대기"
timeout 60 bash -c \
  'until docker exec mysql-8 mysqladmin ping -uroot -p"$DB_PASSWORD" --silent 2>/dev/null; do sleep 2; done'
echo "mysql-8 ready"

#echo "빌드 보장을 위한 메모리 스왑"
## 소형 인스턴스에서 gradle+npm 빌드 중 메모리 고갈로 서버가 멈춘 사례가 있어,
## 2G 스왑을 한 번 만들어 둔다(재부팅 후에도 이 스크립트가 다시 켜 준다).
#if ! swapon --show | grep -q /swapfile; then
#  sudo fallocate -l 2G /swapfile 2>/dev/null || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
#  sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
#  echo "  swap 2G 활성화"
#fi

echo "GHCR 로그인"
echo "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USER}" --password-stdin

echo "이미지 pull"
docker compose pull

echo "컨테이너 실행 및 헬스체크까지 대기"
docker compose up -d --no-build --wait
docker logout ghcr.io

echo "이전 이미지 정리 및 최종 상태보고"
docker image prune -f
docker compose ps