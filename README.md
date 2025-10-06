# ✨ Project Introduction

![mainLogo.png](/assets/ProjectMainLogo.png)

- DummyTalk은 버튼 클릭만으로 OpenAI API를 통해 무작위 잡학 지식을 얻을 수 있는 작은 사이드 프로젝트입니다.
- 단순한 지식 전달에 그치지 않고, 사용자 정보 기반 개인화 또는 메타픽션적인 다크 심리 요소를 섞어 실험적인 경험을 제공합니다.
- 목표: "정보 전달" 그 이상의 "예측 불가능한 재미" & 개발을 통한 프로젝트 지식 습득과 런칭

---

## 🛠️ 기술 스택 (Tech Stack)

### 🛠 Tech Stack

- **FrontEnd**  
  ![Cursor](https://img.shields.io/badge/Cursor-000000?style=for-the-badge&logo=cursor&logoColor=white) ![Gemini](https://img.shields.io/badge/Gemini-4285F4?style=for-the-badge&logo=googlebard&logoColor=white) ![ChatGPT](https://img.shields.io/badge/ChatGPT-74AA9C?style=for-the-badge&logo=openai&logoColor=white)

- **Backend**  
  ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white) ![Spring AI](https://img.shields.io/badge/Spring%20AI-412991?style=for-the-badge&logo=openai&logoColor=white)

- **DB / Infra**  
  ![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)   ![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)    ![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)


  - 수정사항: 프리티어 무료 토큰이 빨리 나가는 관계로 EC2 내 도커를 통한 DB 배포로 변경...

- **빌드 & 배포**  
  ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white) ![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white) ![EC2](https://img.shields.io/badge/AWS%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white) ![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)


---

## 🚀 주요 기능 (Features)

1. **랜덤 잡학 지식 제공**
    - 버튼 클릭 → OpenAI API 호출 → 무작위 지식 반환
2. **확률적 개인화**
    - 일부 요청은 사용자 이름/정보를 바탕으로 지식이 변형
3. **메타픽션 모드**
    - 사용자의 심리에 접근하거나, 사용자를 이야기 속 등장인물처럼 다루는 "실험적 대화" 제공

---

## 📸 실행 화면 (Demo / Screenshot)

> (아직은 개발 중이라 😉)
>

---

## 📂 프로젝트 구조 (Project Structure)

```bash
DummyTalk_BE/
 ┣ .github/
 ┃ ┣ ISSUE_TEMPLATE/
 ┃ ┗ workflows/
 ┣ src/
 ┃ ┣ main/
 ┃ ┃ ┣ java/
 ┃ ┃ ┃ ┗ DummyTalk/DummyTalk_BE/
 ┃ ┃ ┃   ┣ domain/
 ┃ ┃ ┃   ┗ global/
 ┃ ┃ ┃     ┣ email/
 ┃ ┃ ┃     ┗ security/
 ┃ ┃ ┗ resources/
 ┃ ┃   ┣ static/
 ┃ ┃   ┗ templates/
 ┗ build.gradle

```

---

## 🎯 프로젝트 의의 (Why this project?)

- 재미 요소, 예측 불가능성, 메타픽션을 결합한 새로운 형태의 "가벼운 대화형 서비스"
- 단순한 API 호출 프로젝트가 아니라,

  **"AI가 주는 정보"를 어떻게 사용자 경험으로 변환할 수 있는가?** 를 실험하는 공간

- 재미 뿐만 아니라 “실제 런칭”을 위한 1인 개발 프로젝트 진행으로 다양한 지식 습득!
    - 동시성 제어
        - 유로 서비스 쿠폰에 대한 돌발 퀴즈로 동시성 문제 발생 및 제어
    - 대규모 트래픽 처리
        - 위 서비스와 더불어, 특정 API에 대한 트래픽 집중 처리
        - 트래픽 관리를 위한 클라우드 서비스 학습
    - 배포 및 운영 자동화
        - Github action, Workflow 파일 학습
        - Docker-Compose 컨테이너 및 볼륨 관리 학습
    - 보안 (JWT, Spring Security 등)
        - 액세스 토큰, 리프레쉬 토큰 관리 학습

---

## 📌 향후 계획 (To-do / Roadmap)

- [ ]  잡학 지식 카테고리 확장
- [ ]  채팅 및 돌발 퀴즈 서비스 도입
- [ ]  사용자 정보 기반 추천 알고리즘 강화
- [ ]  다크 모드 + 심리학적 대화 강화
- [ ]  웹/앱 프론트엔드 연동
- [ ]  JMeter / K6 를 통한 실시간 성능 테스트
