# SSH 접속 오류 해결 (Windows PowerShell)

인스턴스를 중지/시작하면 **퍼블릭 IP 가 바뀐다**(탄력적 IP 미할당 정책). 아래 3가지가 반복해서 나오는 오류다.

---

## 0. 한 번에 해결 — 그냥 이 3줄을 복붙

```powershell
$KEY = "C:\Users\jijys\Desktop\MAIN\Github\dev_notes\DummyTalk\EC2_DummyTalk_BE_Key.pem"
icacls $KEY /inheritance:r /grant:r "$env:USERNAME:(R)"
ssh -i $KEY ubuntu@<새-퍼블릭-IP>
```

`$KEY` 에 **절대경로**를 쓰므로 어느 폴더에서 실행하든 된다. 키 파일을 새로 받았거나 덮어썼으면 2번째 줄을 다시 실행할 것
(새 파일은 상위 폴더의 권한을 다시 상속받기 때문).

---

## 오류 1. `Identity file ... not accessible: No such file or directory` → `Permission denied (publickey)`

```
Warning: Identity file EC2_DummyTalk_BE_Key.pem not accessible: No such file or directory.
ubuntu@ec2-...: Permission denied (publickey).
```

**원인**: 키 파일이 없는 폴더에서 상대경로로 실행했다. (예: `dev_notes\With_Run_V2` 에서 실행)
`Permission denied` 는 결과일 뿐 — **진짜 원인은 첫 줄**이다. 키를 못 찾아 아무 키도 제시하지 못한 것.

**해결**: 절대경로를 쓴다 (위 0번). 또는 키가 있는 폴더로 먼저 이동:

```powershell
cd C:\Users\jijys\Desktop\MAIN\Github\dev_notes\DummyTalk
ssh -i .\EC2_DummyTalk_BE_Key.pem ubuntu@<새-퍼블릭-IP>
```

> 파일명 대소문자도 확인할 것 — `..._key.pem` 과 `..._Key.pem` 은 Windows 에선 같지만 스크립트/문서에 섞여 있으면 헷갈린다.

---

## 오류 2. `WARNING: UNPROTECTED PRIVATE KEY FILE!` / `bad permissions`

```
Bad permissions. Try removing permissions for user: BUILTIN\Users (S-1-5-32-545) on file ...
Permissions for 'EC2_DummyTalk_BE_Key.pem' are too open.
```

**원인**: Windows 에서 새로 만들거나 다운로드한 파일은 상위 폴더 ACL 을 상속받아 `BUILTIN\Users` 가 읽을 수 있다.
OpenSSH 는 "나 말고 누구든 읽을 수 있는 개인키" 를 거부한다 (`chmod 600` 에 해당하는 Windows 규칙).

**해결**:

```powershell
$KEY = "C:\Users\jijys\Desktop\MAIN\Github\dev_notes\DummyTalk\EC2_DummyTalk_BE_Key.pem"
icacls $KEY /inheritance:r /grant:r "$env:USERNAME:(R)"
icacls $KEY          # 확인: 내 계정 (R) 한 줄만 남아야 한다
```

- `/inheritance:r` — 폴더에서 물려받은 권한을 끊는다
- `/grant:r "$env:USERNAME:(R)"` — 내 계정에 읽기만 부여 (`:r` 은 기존 권한을 대체)

**키를 새로 받을 때마다 다시 실행해야 한다.** 파일을 덮어쓰면 ACL 이 초기화된다.

---

## 오류 3. `REMOTE HOST IDENTIFICATION HAS CHANGED!` 또는 호스트 키 재확인

인스턴스를 새로 만들면 호스트 키가 바뀌고, IP 를 재사용하면 `known_hosts` 의 옛 기록과 충돌한다.

```powershell
ssh-keygen -R <옛-IP-또는-호스트명>          # 예: ssh-keygen -R ec2-54-164-253-72.compute-1.amazonaws.com
ssh-keygen -R 54.164.253.72
```

새 호스트에 물어보지 않고 접속하려면:

```powershell
ssh -i $KEY -o StrictHostKeyChecking=accept-new ubuntu@<새-퍼블릭-IP>
```

> 같은 인스턴스를 중지/시작만 한 경우엔 호스트 키가 그대로라 새 IP 에서 지문 확인만 한 번 더 뜬다 (정상).

---

## 인스턴스를 중지/시작한 뒤 반드시 같이 갱신할 것

| 대상 | 내용 |
|---|---|
| Cloudflare DNS | `ddotg.dev` A 레코드(Name `@`) → 새 IP, Proxied 유지 |
| GitHub Secret | `EC2_HOST` → 새 IP (안 바꾸면 배포가 옛 IP 로 감) |
| 보안그룹 | 내 공인 IP 가 바뀌었다면 22/3000/9090 소스도 갱신 (`curl -s ifconfig.me`) |
| known_hosts | 새 IP 지문 수락 (오류 3) |

---

## 접속 후 상태 한 번에 보기

```bash
cat /var/log/dummytalk-cloud-init.log      # 부팅 셋업 결과 (arch/docker/sysctl/swap)
sudo docker ps --format 'table {{.Names}}\t{{.Status}}'
free -m                                    # Swap used 가 0 이어야 회차 유효
```
