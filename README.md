# Uni_Core
대학생들을 위한 코어 기능들만 모은 웹 프로젝트



## 프로젝트 사용 스택

**Spring**

- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Data Redis
- ~~Spring REST Docs?~~
- Spring AI
    - openAI 사용

**ETC**

- Lombok
- MySQL
- Docker Compose Support?
- Java Email Sender
- Swagger
- Notion
- Figma

## 프로젝트 구조

- 도메인형 패키지 구조
    - 프로젝트 구조
        - domain
            - user
            - notice
            - lecture
        - global
            - apiResponse
            - security
            - …

---

## Convention

### In-Code

1. 네이밍 규칙
   변수, 메서드, 파라미터: 카멜 케이스(camelCase) 사용 
   - 예시: userList, findCourseByUserId, updateStudentCount


2. 클래스, 인터페이스: 파스칼 케이스(PascalCase) 사용 
   - 예시: CourseController, UserService, CourseRepository


3. 상수: 모두 대문자로 표기하며, 단어 사이는 언더스코어(_)로 구분합니다. 
    - 예시: MAX_STUDENT_COUNT, DEFAULT_PAGE_SIZE


4. 리스트 변수: 변수명 끝에 List를 붙여 명확하게 구분합니다.
   - 예시: userList, courseList, studentList


5. 주석 (Comments)
   - 클래스 및 메서드: 각 클래스와 공개(public) 메서드에는 Javadoc 스타일의 주석을 달아 어떤 역할을 하는지, 어떤 파라미터를 받는지, 어떤 값을 반환하는지 명시합니다.


6. 코드: 복잡한 로직이나 특별한 의도가 있는 코드에는 인라인 주석(//)을 추가하여 설명합니다.


### Commit
feat: 새로운 기능 추가

fix: 버그 수정

docs: 문서 수정 (README.md, 주석 등)

style: 코드 서식, 세미콜론 누락 등 (코드 동작에 영향을 주지 않는 변경)

refactor: 코드 리팩토링 (기능 변경 없이 코드를 개선)

test: 테스트 코드 추가 또는 수정

chore: 빌드, 패키지 관리 등 잡다한 변경

---

