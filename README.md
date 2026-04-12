# u-u-g-backend

🗣️ 음어그(U-U-G) 백엔드 서버입니다.

---

## 실행 환경

| 환경 | 설명 | 브랜치 |
|------|------|--------|
| `local` | 로컬 개발 (DB만 Docker, 앱은 직접 실행) | `develop` |
| `dev` | 개발 서버 (앱 + DB 전체 Docker) | `develop` |
| `prod` | 운영 서버 (앱 + DB 전체 Docker) | `master` |

---

## 로컬 개발 (local)

DB만 Docker로 띄우고 앱은 IDE 또는 Gradle로 직접 실행합니다.

**1. 환경변수 파일 준비**

```bash
cp .env.example .env.local
# .env.dev 파일을 열어 DB 접속 정보 입력
```

**2. DB 컨테이너 실행**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.local up -d db
```

**3. 앱 실행**

```bash
export $(grep -v '^#' .env.local | xargs)
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 개발 서버 (dev)

앱과 DB를 모두 Docker로 실행합니다.

**1. 환경변수 파일 준비**

```bash
cp .env.example .env.dev
# .env.dev 파일을 열어 DB 접속 정보 입력
```

**2. 전체 실행**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.dev up -d --build
```

**3. 로그 확인**

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.dev logs -f app
```

---

## 운영 서버 (prod)

**1. 환경변수 파일 준비**

```bash
cp .env.example .env.prod
# .env.prod 파일을 열어 실제 운영 값 입력 (운영 배포 시 비밀번호 등 반드시 확인 필요)
```

**2. 전체 실행**

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

**3. 로그 확인**

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod logs -f app
```

---

## 컨테이너 종료

```bash
# dev
docker compose -f docker-compose.yml -f docker-compose.dev.yml --env-file .env.dev down

# prod
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod down
```

---

## API 문서

앱 실행 후 아래 주소에서 Swagger UI를 확인할 수 있습니다. (dev 환경에서만 활성화)

```
http://localhost:8080/swagger-ui/index.html
```
