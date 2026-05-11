#!/bin/bash
set -eo pipefail

# .env 에서 DOCKER_USERNAME 읽기
DOCKER_USERNAME=$(grep '^DOCKER_USERNAME=' .env | cut -d= -f2)

# Docker Compose v2 플러그인 확인 및 자동 설치
if ! docker compose version &>/dev/null; then
    echo "Docker Compose v2 플러그인 없음 → 설치 중..."
    sudo apt-get update -qq
    sudo apt-get install -y docker-compose-plugin
    echo "설치 완료: $(docker compose version)"
fi

COMPOSE="sudo docker compose -p dummytalk -f docker/docker-compose.yml --env-file .env"

# 뱃지 이미지 저장 디렉토리 사전 생성 (컨테이너 bind mount )
mkdir -p /home/ubuntu/data/badges

# 컨테이너명 변경 (기존 언더하이픈 충돌 대비 제거)
sudo docker rm -f DummyTalk_Spring 2>/dev/null || true

echo "===== [1/4] 최신 이미지 pull ====="
$COMPOSE pull

echo "===== [2/4] 모든 서비스 기동 / 업데이트 ====="
# Kibana는 profiles: monitoring 으로 제외됨, 필요 시 수동 기동:
# sudo docker compose -p dummytalk -f docker/docker-compose.yml --env-file .env --profile monitoring up -d kibana
$COMPOSE up -d

echo "===== [3/4] 오래된 이미지 정리 ====="
sudo docker image prune -f

echo "===== [4/4] 배포 완료 ====="
sudo docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"