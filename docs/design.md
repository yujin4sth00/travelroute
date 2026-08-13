# 여행 동선 최적화 웹 서비스 - 시스템 설계 (v2)

> 코드 작성 없이 구조와 로드맵만 다루는 설계 문서. Phase 0 스캐폴딩은 이 문서의 로드맵을 따른다.

---

## 1. 전체 시스템 구조

```
┌─────────────────────────────┐
│   Frontend (React + Vite)    │
│   - Kakao Map JS SDK 직접 로드 │
│   - 나머지는 전부 백엔드 API 호출 │
└───────────────┬──────────────┘
                │ REST API (JSON)
┌───────────────▼──────────────┐
│   Backend (Java Spring Boot)  │
│  - 장소/여행/날짜/배치 CRUD      │
│  - 동선 최적화 로직 (Haversine)  │
│  - Kakao Local API 프록시       │
│  - Kakao 도보 경로 API 프록시+캐시 │
│  - (추후) 카카오 로그인/인증        │
└───────────────┬──────────────┘
                │ JPA
┌───────────────▼──────────────┐
│         MySQL (Docker)        │
└────────────────────────────────┘

[Docker Compose로 backend + mysql (+frontend) 묶어서 로컬/배포 환경 통일]
```

### 책임 분리 원칙

| 영역 | 누가 담당 | 이유 |
|---|---|---|
| 장소 검색(Kakao Local API) | **백엔드가 프록시** | REST API 키를 프론트에 노출하지 않기 위함 |
| 지도 표시(Kakao Map SDK) | **프론트가 직접** | 지도 SDK는 브라우저에서 직접 그려야 함. JavaScript 키는 카카오 콘솔의 "사용 도메인" 등록으로 도용을 막음 |
| 도보 경로 조회(Kakao Walking API) | **백엔드가 프록시 + 캐시** | 쿼터 관리와 캐싱을 위해 반드시 백엔드를 거쳐야 함 |
| 동선 순서 계산(Haversine, NN+2-opt) | **백엔드 순수 로직** | 외부 API 전혀 안 씀. 비용 0원 |

---

## 2. 주요 도메인

- **Place**: 검색·저장한 장소 (이름, 주소, 좌표, 카테고리, 메모, kakao_place_id). 출발지/최종 목적지도 Place의 일종.
- **Trip**: 여행 하나 (이름, 시작일, 종료일)
- **TripDay**: 여행의 하루 (며칠째, 날짜, 그날의 출발지/도착지 참조)
- **TripDayPlace**: 특정 날짜에 배치된 장소 + 방문 순서(`visit_order`) + 수동 고정 여부(`is_locked`)
- **RouteCache**: 두 장소 간 실제 도보 경로 결과 캐시 (거리, 소요시간, 경로 좌표)
- **User** *(추후)*: 카카오 로그인 연동 시점에 추가

---

## 3. DB 구조 초안 (MySQL)

```sql
CREATE TABLE places (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    category VARCHAR(50),
    memo TEXT,
    kakao_place_id VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    title VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE trip_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    day_number INT NOT NULL,
    date DATE,
    start_place_id BIGINT NULL,
    end_place_id BIGINT NULL,
    FOREIGN KEY (trip_id) REFERENCES trips(id),
    FOREIGN KEY (start_place_id) REFERENCES places(id),
    FOREIGN KEY (end_place_id) REFERENCES places(id)
);

CREATE TABLE trip_day_places (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_day_id BIGINT NOT NULL,
    place_id BIGINT NOT NULL,
    visit_order INT NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trip_day_id) REFERENCES trip_days(id),
    FOREIGN KEY (place_id) REFERENCES places(id)
);

CREATE TABLE route_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    origin_place_id BIGINT NOT NULL,
    destination_place_id BIGINT NOT NULL,
    distance_m INT,
    duration_sec INT,
    path_json JSON,
    source VARCHAR(20) DEFAULT 'KAKAO_WALK',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_route (origin_place_id, destination_place_id)
);

-- 추후 인증 붙을 때
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kakao_id VARCHAR(50) UNIQUE NOT NULL,
    nickname VARCHAR(50),
    profile_image VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

**설계 포인트**
- `route_cache`는 `(origin_place_id, destination_place_id)` 유니크 제약으로 동일 구간 재요청 시 API 호출 없이 캐시 반환.
- `trip_days.start_place_id`/`end_place_id`는 `places` 레코드를 참조.
- `user_id`는 Phase 7 전까지 nullable.

---

## 4. API 구조 초안

```
[장소]
GET    /api/places/search?query=       Kakao Local API 프록시 (검색만)
POST   /api/places                     검색 결과 저장
GET    /api/places                     저장된 장소 목록 조회
DELETE /api/places/{id}                삭제

[여행/날짜]
POST   /api/trips                              여행 생성 → trip_days 자동 생성
GET    /api/trips/{tripId}                     여행 상세 (일차 목록 포함)
PATCH  /api/trips/{tripId}/days/{dayId}        start_place_id / end_place_id 지정

[일정 배치]
POST   /api/trips/{tripId}/days/{dayId}/places         장소 추가
DELETE /api/trips/{tripId}/days/{dayId}/places/{id}    제거
PATCH  /api/trips/{tripId}/days/{dayId}/places/reorder  수동 순서 변경 (is_locked=true)

[동선 최적화 - 외부 API 호출 없음]
POST   /api/trips/{tripId}/days/{dayId}/optimize
       → Haversine 기반 NN+2-opt, visit_order 갱신

[실제 경로 - Kakao 도보 API 호출 지점, 캐시 우선]
GET    /api/trips/{tripId}/days/{dayId}/route
       → 인접 장소쌍마다 route_cache 우선 조회, 없을 때만 Kakao 호출 후 캐시 저장

[추후]
POST   /api/auth/kakao                 카카오 로그인
```

`optimize`(계산)와 `route`(실제 경로 조회)를 분리하여, 순서 재계산은 무료(0원)이고 확정 시에만 실제 API를 호출.

---

## 5. 자동 동선 알고리즘 설계 방향

**Stage A — 순서 계산 (외부 API 0회)**
1. 장소 좌표 + 출발지/도착지 좌표 확보
2. Haversine 직선거리 행렬 계산
3. Open-path 최적화: Nearest Neighbor 초기해 → 2-opt 개선 (양끝 고정, `is_locked` 장소는 순서 고정)
4. 결과: 정렬된 순서 (직선거리 기준 근사치)

**Stage B — 실제 경로 확정 (외부 API 최소 호출)**
1. 확정된 순서의 인접 쌍(N-1개)만 대상
2. `route_cache` 우선 조회, 없는 구간만 Kakao 도보 경로 API 호출 후 캐시 저장
3. 실제 도보 거리/시간/폴리라인을 지도에 반영

**다박 여행 자동 분배 (추후)**: 전체 장소를 일수만큼 지리적으로 그룹핑(그리드/K-means, API 호출 없음) 후 그룹별로 Stage A/B 적용.

**사용자 수정 구조**: `trip_day_places.is_locked`로 관리. 잠긴 장소는 재정렬 대상에서 제외되어 완전 자동/완전 수동/혼합 모드를 하나의 구조로 커버.

---

## 6. 단계별 개발 로드맵

```
Phase 0: 프로젝트 뼈대 + Docker 환경                    ✅ 완료
Phase 1: 장소(Place) 도메인                             ✅ 완료
Phase 2: 여행(Trip)/날짜(TripDay)/배치(TripDayPlace) CRUD ✅ 완료
Phase 3: 동선 최적화 로직 (Stage A, Haversine)           ✅ 완료
Phase 4: Kakao 도보 경로 API 연동 + 캐싱 (Stage B)        ✅ 완료
Phase 5: 프론트엔드 지도 시각화                          ✅ 완료
Phase 6: 다박 여행 자동 분배                             ✅ 완료
Phase 7: Kakao 로그인 연동                              ✅ 완료
Phase 8: 배포 및 확장                                   보류 (실제 호스팅 비용 발생 — 필요 시 진행)
```

각 Phase의 상세 지시문은 프로젝트 히스토리(대화 로그)에 남겨진 원본 설계 프롬프트를 참고한다.

Phase 8(배포)은 의도적으로 보류했다. 로컬 `docker compose up`으로 전체 스택(MySQL + 백엔드)이
완전히 동작하는 상태이며, 프론트는 `npm run dev`로 별도 실행한다. 실제 클라우드/서버 배포는
비용이 발생하므로 필요해질 때 별도로 진행한다.

---

## 확장성 관련 메모

- `route_cache`가 쌓일수록 API 호출 자체가 줄어드는 구조라, 사용자가 늘어도 쿼터 부담이 선형으로 커지지 않음.
- 백엔드는 무상태(stateless)로 설계(세션 대신 JWT 예정)해서 수평 확장이 쉬움. 상태는 전부 MySQL에.
- 캐시 테이블이 매우 커지면 Redis 도입을 고려할 수 있지만, 초기 단계에선 MySQL 테이블 하나로 충분.
