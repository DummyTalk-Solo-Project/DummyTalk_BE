# EC2 측정 인스턴스 런북 — m7g.large / Ubuntu 24.04 arm64

재측정 캠페인용 인스턴스를 **처음부터 다시 만들 때** 따라 하는 순서다. 콘솔 클릭·자격증명·인증서·Secrets 는 사람이 하고,
호스트 셋업은 [`cloud-init.yaml`](cloud-init.yaml) 이 자동으로 한다. 여기에 비밀은 없다 (절차만 있다).

> 왜 m7g.large 인가 — Graviton3 물리 코어 2개(SMT 없음, vCPU=코어)로 VT 캐리어 수를 기존 측정과 같은 2 로 고정하면서
> 버스트 크레딧을 제거한다. c7g.large 와 CPU 가 같고 메모리만 4→8 GiB 라 동거 컴포넌트(ES·Prometheus·Grafana) 때문에
> 회차가 OOM/스왑으로 무효가 될 위험만 없앤다. 상한선이 낮아 코어를 늘릴 땐 **같은 세대인 m7g.xlarge** 로 간다
> (m6g 계열은 Graviton2 라 코어 수와 CPU 세대가 동시에 바뀌어 비교 불가).

---

## 0. 준비물
- AWS 콘솔 접근, 기존 키페어(.pem)
- Cloudflare 계정 (ddotg.dev DNS · Origin 인증서)
- GitHub 레포 Secrets 편집 권한 (`EC2_HOST` 갱신)
- 내 PC 공인 IP (`curl -s ifconfig.me`) — 보안그룹에 쓴다

## 1. AMI 선택
콘솔 → EC2 → 인스턴스 시작 → **애플리케이션 및 OS 이미지** → "추가 AMI 찾아보기" → **커뮤니티 AMI** 탭:

| 항목 | 값 |
|---|---|
| 소유자 | `099720109477` (Canonical) |
| 이름 검색 | `ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-arm64-server-` — 날짜가 가장 최신인 것 |
| 아키텍처 | **64비트(Arm)** |
| **Platform details** | **Linux/UNIX** ← Ubuntu Pro(`ubuntu-pro-server`) 가 아닌지 반드시 확인. Pro 는 시간당 추가 과금 |

CLI 가 있다면 (선택):
```bash
aws ec2 describe-images --owners 099720109477 --region ap-northeast-2 \
  --filters "Name=name,Values=ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-arm64-server-*" "Name=state,Values=available" \
  --query 'sort_by(Images,&CreationDate)[-1].[ImageId,Name,PlatformDetails]' --output text
```

## 2. 인스턴스 유형 · 스토리지
| 항목 | 값 | 이유 |
|---|---|---|
| 인스턴스 유형 | **m7g.large** | 2 vCPU = 물리 코어 2, 8 GiB, 비버스터블 |
| 키페어 | 기존 것 | deploy.yml 의 `EC2_PRIVATE_KEY` 와 동일해야 함 |
| 스토리지 | gp3 **20 GiB** | 이미지 5종 + Prometheus TSDB(1s 스크레이프) + Docker 로그(로테이션 50MB×3/컨테이너) |

## 3. 보안그룹 (새로 만들기 권장: `dummytalk-loadtest-sg`)
compose 가 **5432 / 6379 / 9200 / 9300 / 9090 / 3000 을 호스트에 그대로 공개**하므로 SG 가 유일한 방어선이다.

| 포트 | 소스 | 용도 |
|---|---|---|
| 22 | **내 IP/32** | SSH, deploy.yml 의 scp/ssh (GitHub 러너 IP 는 가변 → Actions 실행 시엔 일시적으로 0.0.0.0/0 허용하거나, 러너 IP 대역 허용. 측정 기간엔 전자가 현실적) |
| 80, 443 | 0.0.0.0/0 | nginx (Cloudflare 프록시 경유) |
| 3000 | 내 IP/32 | Grafana 캡쳐 |
| 9090 | 내 IP/32 | Prometheus 직접 조회 (선택) |
| 5432, 6379, 9200, 9300 | **열지 않음** | DB/Redis/ES — 외부 노출 금지 |
| 8080 | 열지 않음 | spring 은 `expose` 만, nginx 가 프록시 |

> `22` 를 GitHub Actions 에 열어야 하는 문제는 측정 캠페인이 끝나면 인스턴스를 종료하므로 감수한다.
> 상시 배포(Oracle)에서는 배포 방식을 바꿀 때 다시 다룬다.

## 4. 사용자 데이터 (cloud-init)
인스턴스 시작 화면 맨 아래 **고급 세부 정보 → 사용자 데이터** 텍스트 상자에 [`cloud-init.yaml`](cloud-init.yaml) **전체 내용**을 붙여넣고 시작.
(파일의 첫 줄 `#cloud-config` 가 반드시 포함돼야 cloud-init 이 cloud-config 로 인식한다)

## 5. 부팅 확인 (SSH)
```bash
ssh -i <키>.pem ubuntu@<퍼블릭 IP>
cloud-init status --wait          # "status: done" 까지 대기 (패키지 업그레이드 포함 2~4분)
cat /var/log/dummytalk-cloud-init.log
```
기대값:
```
arch:      aarch64
os:        Ubuntu 24.04.x LTS
docker:    Docker version 29.x
compose:   Docker Compose version v5.x (또는 v2.x)
sysctl:    262144 (vm.max_map_count)
mem:       7xxx MB total
swap:      0 MB (0 이어야 정상)
```
`docker ps` 가 sudo 없이 되려면 재로그인 1회.

## 6. Cloudflare Origin 인증서
Cloudflare 대시보드 → ddotg.dev → **SSL/TLS → Origin Server → Create Certificate** (호스트명 `ddotg.dev`, `*.ddotg.dev`, RSA 2048, 15년).
발급된 두 텍스트를 EC2 에 배치:
```bash
sudo tee /etc/ssl/cloudflare/cert.pem > /dev/null   # Origin Certificate 붙여넣기 → Ctrl+D
sudo tee /etc/ssl/cloudflare/key.pem  > /dev/null   # Private Key 붙여넣기 → Ctrl+D
sudo chmod 600 /etc/ssl/cloudflare/key.pem
```
SSL/TLS 암호화 모드는 **Full (Strict)** 유지 ([docker/nginx.conf](../../docker/nginx.conf) 상단 주석과 일치).
기존 t3.small 의 키를 복사하지 말고 새로 발급한다 (Lumo 교훈: 개인키는 반출하지 않는다).

## 7. Cloudflare DNS
DNS → `ddotg.dev` A 레코드 → **새 퍼블릭 IP**, 프록시 상태 **Proxied(주황 구름) 유지**.
k6 가 `https://ddotg.dev` 로 쏘면 Cloudflare 엣지를 지나므로 k6 체감 지연에 엣지 RTT 가 섞인다 — 그래서 결과 파일의
**서버측 p95/p99(히스토그램 델타)** 를 전략 비교에 쓴다. (엣지를 우회해 재고 싶으면 SG 443 을 내 IP 에 열고 `BASE_URL=https://<IP>` 에
`--insecure-skip-tls-verify` — Origin 인증서는 공개 체인이 아니라서 검증을 꺼야 함)

## 8. GitHub Secrets
레포 → Settings → Secrets and variables → Actions:

| Secret | 값 |
|---|---|
| `EC2_HOST` | **새 퍼블릭 IP** (이것만 바뀜) |
| `EC2_USERNAME` | `ubuntu` |
| `EC2_PRIVATE_KEY` | 키페어 .pem 전체 (기존과 동일) |
| 나머지 | [`.env.example`](.env.example) 참고 — 기존 값 그대로 |

## 9. 첫 배포
`develop → main` 머지(또는 main 에 push) → Actions `dummytalk github_action deploy!`:
1. `build-and-push` — Dockerfile 을 **linux/arm64** 로 빌드·푸시 (Dockerfile-es 는 QEMU 로 플러그인 설치, 1~2분)
2. `ec2-deploy` — `.env` 생성 → scp(`docker/`, `docker/grafana/`) → pull → up
3. 로그에서 확인:
   ```
   ===== [arch check] host=aarch64 =====
   dummytalk image arch: arm64
   elasticsearch-nori image arch: arm64
   ```
   `amd64` 가 보이면 **측정 무효** — Dockerfile/deploy.yml 의 platforms 를 확인.
4. Discord 성공 알림의 `RAM Avail / SWAP Used` 에서 SWAP 0 확인.

## 10. 측정용 env 추가 (배포 후 매번)
deploy.yml 은 배포마다 `~/dummytalk/.env` 를 **새로 덮어쓴다.** 측정용 3개는 Secrets 에 없으므로 배포 후 직접 추가:
```bash
cd ~/dummytalk
cat >> .env <<'EOF'
JAVA_HEAP=2g
TEST_LOAD_USERS_COUNT=3000
HIKARI_POOL_SIZE=10
EOF
cp .env docker/.env
sudo docker compose -p dummytalk -f docker/docker-compose.yml --env-file .env up -d spring
sudo docker logs -f DummyTalk_Spring 2>&1 | grep -m1 "테스트 유저"   # "테스트 유저 1200명 생성 완료" (기존 300 제외)
```
- `TEST_LOAD_USERS_COUNT` = 본측정 최대 RATE × INTERVAL (600 × 5s = 3000). 시딩은 멱등 — 늘려서 재기동만 하면 된다.
  - k6 는 회차마다 `RATE × INTERVAL` 명만 로그인시키므로, 많이 시딩해 둬도 회차 비용은 늘지 않는다.
- `JAVA_HEAP` 은 회차 사이에 바꾸지 않는다 (바꾸면 다른 실험). compose 기본값이 2g 라 이 줄을 빠뜨려도 2g 로 뜬다.
- CP 스윕은 `HIKARI_POOL_SIZE` 만 바꿔 `up -d spring` (nginx 는 resolver 로 새 IP 를 따라감).

## 10-b. 관리자 승격 (재구축할 때마다 필요)

k6 의 `/api/admin/load-test/reset`(회차 사이 reqCount 초기화)·`/state`(Lost Update 판정) 는 **ADMIN 권한**을 요구하는데,
**코드에 ADMIN 을 만드는 경로가 없다** — 회원가입도 `TestMemberDataLoader` 도 전부 `MemberRole.MEMBER` 로 만든다.
그래서 시딩된 테스트 유저 하나를 DB 에서 직접 승격한다. (인스턴스를 새로 만들면 다시 해야 한다 — 볼륨과 함께 사라짐)

```bash
cd ~/dummytalk
sudo docker exec DummyTalk_Postgres psql   -U "$(grep ^DB_USERNAME= .env | cut -d= -f2)"   -d "$(grep ^DB_NAME= .env | cut -d= -f2)"   -c "UPDATE member SET role='ADMIN' WHERE email='test1@test.com'"
# 확인 (1 이어야 한다)
sudo docker exec DummyTalk_Postgres psql -U "$(grep ^DB_USERNAME= .env | cut -d= -f2)"   -d "$(grep ^DB_NAME= .env | cut -d= -f2)" -tAc "SELECT count(*) FROM member WHERE role='ADMIN'"
```

- 비밀번호는 `TestMemberDataLoader` 의 공통값 `Test1234!` → k6 인자: `-e ADMIN_EMAIL=test1@test.com -e ADMIN_PASSWORD='Test1234!'`
- `test1@test.com` 은 부하 유저 풀에도 포함되지만 `getDummy` 는 역할을 보지 않으므로 측정에 영향 없고, 리셋 대상(`test%@test.com`)에도 포함돼 일관된다.
- 자격증명은 문서·스크립트에 남기지 않고 `-e` 로만 넘긴다.

## 10-c. V1~V4 동시성 전략 전환 (재배포 불필요)

`.env` 3줄만 바꾸고 spring 컨테이너만 다시 띄우면 된다 (약 40초). 이미지 재빌드·GitHub Actions 불필요.

| 회차 | `GETDUMMY_VERSION` | `INTERCEPTOR_ENABLED` | `VIRTUAL_THREADS` | 전략 |
|---|---|---|---|---|
| **V1** | 1 | false | false | 순수 `@Transactional` (동시성 보호 없음) |
| **V2** | 2 | false | false | + `@DistributedLock(waitTime=0)` |
| **V3** | 3 | true | false | + `IdempotentRequestInterceptor`(SETNX) |
| **V4** | 3 | true | true | + Virtual Thread (요청 처리 스레드) |

```bash
cd ~/dummytalk
# 예: V1 로 전환 (sed 로 세 줄을 덮어쓰고 없으면 추가)
for kv in "GETDUMMY_VERSION=1" "INTERCEPTOR_ENABLED=false" "VIRTUAL_THREADS=false"; do
  k="${kv%%=*}"; grep -q "^$k=" .env && sed -i "s|^$k=.*|$kv|" .env || echo "$kv" >> .env
done
cp .env docker/.env
sudo docker compose -p dummytalk -f docker/docker-compose.yml --env-file .env up -d spring
```

**바꾼 뒤 반드시 실제 반영을 확인할 것** — 오늘 "V3 인 줄 알았는데 VT 가 켜져 있던" 일이 있었다:
```bash
curl -s https://ddotg.dev/api/admin/load-test/state -H "Authorization: Bearer <ADMIN AT>" | jq .result
# getDummyVersion / interceptorEnabled / virtualThreads 가 의도대로인지
```
VT 가 실제로 꺼졌는지는 Tomcat 지표로도 교차 확인된다:
```bash
sudo docker exec DummyTalk_Prometheus wget -qO- http://spring:8080/actuator/prometheus | grep tomcat_threads_config_max
#   VT off -> 200 (플랫폼 스레드 풀)   /   VT on -> -1 (풀 자체가 없음)
```

### 회차당 절차 (버전마다 반복)
1. 위 전환 → `/api/admin/load-test/state` 확인
2. **워밍업 필수** — 컨테이너 재시작으로 JIT 이 초기화된다. 생략하면 p50 이 158배로 나온 적이 있다
   `k6 run ... -e RATE=50 -e DURATION=2m -e TAG=warmup k6/dummy-arrival-test.js`
   (setup 로그에 `bucket 없음` 경고가 뜨면 콜드였다는 뜻 — 워밍업이 제 역할을 한 것)
3. 3분 간격 → **축 A 처리량·지연**: `k6 run ... -e RATE=200 k6/dummy-arrival-test.js` → Grafana 캡쳐 3장
4. 2분 간격 → **축 B 정합성**: `k6 run ... -e USERS=200 -e CONCURRENT=5 k6/dummy-spike-test.js`
5. 결과 파일 2쌍이 `dev_notes/DummyTalk/results/` 에 생겼는지 확인 (파일명 TAG 로 버전이 구분된다)

## 11. 회차 전 체크리스트 (매 회차)
```bash
free -m                              # Swap used 0
sudo docker stats --no-stream        # 컨테이너별 메모리 — ES ~600MB, spring ~1.3GB 안팎이면 정상
curl -s https://ddotg.dev/actuator/health          # {"status":"UP"}
curl -s https://ddotg.dev/actuator/prometheus | grep -c 'http_server_requests_seconds_bucket{'   # 0 이 아니어야
```
- Grafana `http://<IP>:3000` → DummyTalk 폴더 → "DummyTalk · K6 부하테스트 (open model)" 로드 확인 (admin 초기 비밀번호는 최초 로그인 시 변경)
- 회차 실행은 내 PC 에서 (레포 루트):
  ```bash
  k6 run -e BASE_URL=https://ddotg.dev -e ADMIN_EMAIL=test1@test.com -e ADMIN_PASSWORD='Test1234!' -e RATE=50 -e DURATION=2m -e TAG=warmup k6/dummy-arrival-test.js
  k6 run -e BASE_URL=https://ddotg.dev -e ADMIN_EMAIL=test1@test.com -e ADMIN_PASSWORD='Test1234!' -e RATE=50  k6/dummy-arrival-test.js
  ```
- 회차마다: 결과 파일(`dev_notes/DummyTalk/results/`) 확인 → Grafana 캡쳐.
  - 저장 위치: `dev_notes/DummyTalk/grafana dashboard/<회차 파일명>/1.jpg, 2.jpg, 3.jpg`
    (1 = Row 1·2 부하/지연, 2 = Row 3 포화 순서·CP, 3 = Row 4·5 JVM/보조)
  - 창은 결과 파일 헤더의 `시작 −30s ~ 종료 +30s`. 이보다 넓히면 setup(로그인) 구간이 섞여
    범례의 **Mean 이 희석**된다 — 회차 비교에는 **Max 와 k6 총계**만 쓸 것 (CLAUDE_INIT 수치 해석 규칙 ②③).
  - 회차마다 기록할 Grafana Max: 처리 rps · 429 비율 · 서버 p95/p99 · CP pending · CP acquire p95 · CPU system · GC max pause

## 12. 종료
측정이 끝나면 **중지가 아니라 종료**(스토리지 과금까지 끊김). 종료 전:
- `dev_notes/DummyTalk/results/` 에 모든 회차 `.txt/.json` 이 있는지
- Prometheus 원본이 필요하면 `sudo docker cp DummyTalk_Prometheus:/prometheus ./prom-tsdb && tar czf prom-tsdb.tgz prom-tsdb` 로 반출
- Grafana 에서 UI 로 고친 패널이 있으면 JSON export → `docker/grafana/dashboards/` 에 반영

---

## 이 호스트에만 존재하는 것 (레포에 없는 것) — 재구축 시 다시 만들어야 하는 목록
| 항목 | 만드는 곳 |
|---|---|
| `/etc/ssl/cloudflare/{cert,key}.pem` | 6단계 (Cloudflare) |
| `~/dummytalk/.env` | deploy.yml 이 Secrets 로 생성 + 10단계 측정용 키 |
| Postgres/Redis/ES/Grafana 볼륨 데이터 | 첫 기동 시 DataLoader 가 시딩 (더미·희귀도·뱃지·테스트 유저) |
| Grafana admin 비밀번호 | 최초 로그인 시 |
| **ADMIN 역할 부여** (`test1@test.com`) | 10-b 단계 SQL — 코드에 생성 경로가 없다 |
