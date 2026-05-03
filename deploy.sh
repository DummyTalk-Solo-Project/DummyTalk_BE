#!/bin/bash
set -e

# .env 에서 DOCKER_USERNAME 읽기
DOCKER_USERNAME=$(grep '^DOCKER_USERNAME=' .env | cut -d= -f2)

echo "===== [1/4] 인프라 서비스 기동 (Prometheus, Grafana, Elasticsearch, Kibana) ====="
# docker compose up 을 먼저 실행해 dummytalk-network 를 생성한 뒤 Spring 컨테이너를 붙임
sudo docker compose up -d

echo "===== [2/4] Spring 앱 컨테이너 교체 ====="
sudo docker stop DummyTalk_Spring 2>/dev/null || true
sudo docker rm   DummyTalk_Spring 2>/dev/null || true

# 최신 이미지 pull (build-and-push-to-docker job 에서 Docker Hub 에 올린 이미지)
sudo docker pull "${DOCKER_USERNAME}/dummytalk:latest"

# Spring 앱 기동
# --network: docker-compose 프로젝트명(디렉토리명 dummytalk) + 네트워크명 = dummytalk_dummytalk-network
#            → elasticsearch 컨테이너를 hostname 'elasticsearch' 으로 참조 가능 (ES_URL 환경변수)
sudo docker run -d \
  --name DummyTalk_Spring \
  --network dummytalk_dummytalk-network \
  --restart always \
  -p 8080:8080 \
  --env-file .env \
  "${DOCKER_USERNAME}/dummytalk:latest"

echo "===== [3/4] 오래된 이미지 정리 ====="
sudo docker image prune -f

echo "===== [4/4] 배포 완료 ====="
sudo docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
