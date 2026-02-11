# MediaProcessingService 코드 흐름 분석

## 1. 클래스 위치 및 구조

**파일 경로**: `src/main/java/com/triptyche/backend/domain/media/service/MediaProcessingService.java`

**현재 활성화된 의존성**:
- `S3UploadService` (전역 S3 업로드 서비스)
- `MediaFileRepository` (미디어 파일 리포지토리)

**주석 처리된 의존성** (Presigned URL 리팩토링 대기 중):
- `Executor cpuBoundTaskExecutor` (CPU 바운드 작업용 스레드 풀)
- `Executor ioBoundTaskExecutor` (I/O 바운드 작업용 스레드 풀)
- `MetadataExtractorService` (EXIF 메타데이터 추출 서비스)
- `PinPointService` (핀포인트 생성/조회 서비스)

---

## 2. 클래스 다이어그램 (의존성 관계)

```
┌─────────────────────────────────┐
│     TripController              │
│  (DELETE /v1/trips/{tripKey})   │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   TripManagementService         │
│   - deleteTrip()                │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   MediaProcessingService        │
│   - deleteMediaFilesByTrip()    │
└────────┬────────────────────┬───┘
         │                    │
         ▼                    ▼
┌──────────────────┐   ┌──────────────────┐
│ MediaFileRepo    │   │ S3UploadService  │
│ - findAllByTrip()│   │ - deleteFiles()  │
│ - deleteAll()    │   └──────────────────┘
└──────────────────┘

EventPublisher → TripDeletedEvent → NotificationEventListener
```

---

## 3. 메서드별 시퀀스 흐름

### 활성 메서드: `deleteMediaFilesByTrip(Trip trip)`

**호출 경로**:
```
TripController.deleteTrip()
  → TripManagementService.deleteTrip()
    → MediaProcessingService.deleteMediaFilesByTrip()
```

**상세 실행 흐름**:
```
TripController.deleteTrip(userEmail, tripKey)
  ↓
TripManagementService.deleteTrip(userEmail, tripId)
  ├─ 1. validateAccessibleTrip() - 접근 권한 검증
  ├─ 2. eventPublisher.publishEvent(TripDeletedEvent) - 이벤트 발행
  │    └─ NotificationEventListener.onTripDeleted()
  │         └─ 승인된 공유자들에게 알림 전송 (WebSocket)
  ├─ 3. shareRepository.deleteAllByTrip(trip) - 공유 정보 삭제
  ├─ 4. mediaProcessingService.deleteMediaFilesByTrip(trip)
  │    ├─ 4-1. mediaFileRepository.findAllByTrip(trip) - 미디어 파일 조회
  │    ├─ 4-2. mediaKeys 추출 (stream)
  │    ├─ 4-3. s3UploadService.deleteFiles(mediaKeys) - S3에서 벌크 삭제
  │    │      └─ S3Client.deleteObjects() - AWS SDK 호출
  │    └─ 4-4. mediaFileRepository.deleteAll(mediaFiles) - DB에서 삭제
  └─ 5. tripRepository.delete(trip) - 여행 정보 삭제 (Cascade로 PinPoint도 삭제)
```

### 주석 처리된 메서드: `processFileUpload(Trip, List<MultipartFile>)`

**의도된 흐름** (현재 비활성화, Presigned URL 리팩토링 시 재작성 예정):
```
processFileUpload(trip, files)
  ├─ 각 파일별 CompletableFuture 생성
  │   └─ processSingleFileUpload(trip, file)
  │       ├─ metadataFuture: MetadataExtractorService.extractMetadata(file) @ cpuBoundExecutor
  │       └─ uploadFuture: S3UploadService.uploadFile(file) @ ioBoundExecutor
  │       └─ thenCombine: 두 결과 결합 → MediaProcessResult
  └─ CompletableFuture.allOf() 대기
      └─ thenApplyAsync @ ioBoundExecutor
          ├─ PinPointService.findOrCreatePinPoint()
          ├─ MediaFile 엔티티 생성
          └─ mediaFileRepository.saveAll(mediaFiles) - 벌크 저장
```

---

## 4. 트랜잭션 경계

### `deleteMediaFilesByTrip()`
- **트랜잭션 애노테이션**: `jakarta.transaction.Transactional` (잘못된 import)
- **트랜잭션 범위**:
  - DB 조회 (`findAllByTrip`)
  - S3 I/O 작업 (트랜잭션 내부에서 실행됨)
  - DB 삭제 (`deleteAll`)

### 호출자 `TripManagementService.deleteTrip()`
- **트랜잭션 애노테이션**: `@Transactional`
- 전체 삭제 프로세스가 하나의 트랜잭션으로 묶임
- 중첩 트랜잭션 발생: `TripManagementService` (외부) → `MediaProcessingService` (내부)

---

## 5. 이벤트 발행/구독 관계

### 발행: `TripManagementService.deleteTrip()`
```java
eventPublisher.publishEvent(new TripDeletedEvent(trip))
```

### 구독: `NotificationEventListener.onTripDeleted()`
1. `getApprovedShareRecipientIds()` - 승인된 공유자 목록 조회
2. 각 공유자에게 Notification 엔티티 DB 저장 + WebSocket 전송

**주의점**:
- 이벤트 리스너는 동기 실행 (기본값) → `deleteTrip()` 트랜잭션 커밋 전 실행
- WebSocket 전송 실패 시 예외 catch하여 로그만 남김 (트랜잭션 롤백 방지)

---

## 6. 외부 시스템 연동

### S3 (AWS SDK v2)
- `S3UploadService.deleteFiles()` → `S3Client.deleteObjects()` (벌크 삭제, 단일 API 호출)
- 예외 시: `S3Exception` → `CustomException(ResultCode.FILE_DELETE_FAILED)`

### Redis
- 현재 MediaProcessingService에서는 직접 사용 안함

---

## 7. 주요 발견 사항 및 개선 포인트

### [Critical] 트랜잭션 내 외부 I/O 작업

```java
@Transactional  // DB 커넥션 점유 시작
public void deleteMediaFilesByTrip(Trip trip) {
    List<MediaFile> mediaFiles = mediaFileRepository.findAllByTrip(trip);
    // ...
    s3UploadService.deleteFiles(mediaKeys); // S3 네트워크 I/O (수백 ms~초)
    mediaFileRepository.deleteAll(mediaFiles);
}
```

- S3 네트워크 지연 동안 DB 커넥션 점유
- 동시 삭제 요청 시 HikariCP 커넥션 풀 고갈 위험

**권장**: S3 삭제를 트랜잭션 외부로 이동
```java
// 1단계: DB만 삭제 (트랜잭션 내)
@Transactional
public List<String> deleteMediaFilesFromDB(Trip trip) {
    List<String> mediaKeys = mediaFileRepository.findMediaKeysByTrip(trip);
    mediaFileRepository.deleteAllByTripBulk(trip);
    return mediaKeys;
}

// 2단계: S3 삭제 (트랜잭션 외부)
public void deleteMediaFilesByTrip(Trip trip) {
    List<String> mediaKeys = deleteMediaFilesFromDB(trip);
    if (!mediaKeys.isEmpty()) {
        s3UploadService.deleteFiles(mediaKeys);
    }
}
```

### [Critical] N+1 삭제 쿼리

`mediaFileRepository.deleteAll(mediaFiles)` → 내부적으로 각 엔티티마다 개별 DELETE 실행

**현재**: N개 파일 삭제 시 N번 DELETE 쿼리
**목표**: 1번 벌크 DELETE 쿼리

```java
@Modifying
@Query("DELETE FROM MediaFile m WHERE m.trip = :trip")
void deleteAllByTrip(@Param("trip") Trip trip);
```

### [Warning] 잘못된 트랜잭션 Import

```java
import jakarta.transaction.Transactional; // JTA 트랜잭션
// 권장:
import org.springframework.transaction.annotation.Transactional;
```

- Spring의 `readOnly`, `propagation` 등 옵션 사용 불가
- CLAUDE.md 컨벤션 위반

### [Info] Entity 컨벤션 위반

`MediaFile`, `Trip` 엔티티에서:
- `@Data` 사용 (CLAUDE.md 금지) → `@Getter`로 변경 필요
- `@AllArgsConstructor` 사용 (CLAUDE.md 금지) → `@Builder`로 대체

---

## 8. 규모 적합성 평가 (DAU 500 / 피크 100 동시접속)

| 현재 선택 | 평가 | 이 규모에서 |
|-----------|------|-------------|
| S3 벌크 삭제 | 적절 | 네트워크 왕복 최소화 |
| JPA `deleteAll()` | 부적절 | N+1 쿼리 발생, 벌크 DELETE 필요 |
| 트랜잭션 내 S3 I/O | 문제 | HikariCP 기본값(10)에서 커넥션 고갈 가능 |
| 동기 이벤트 리스너 | 적절 | 피크 100명에서 비동기 불필요 |

---

## 9. 포트폴리오 스토리 후보

| 순위 | 문제 | Before 측정 | After 목표 | 면접 키워드 |
|------|------|------------|-----------|------------|
| 1 | N+1 삭제 쿼리 | 미디어 N개 삭제 시 쿼리 N+1회 | 벌크 삭제 1회 | JPA 벌크 연산, @Modifying |
| 2 | 트랜잭션 내 S3 I/O | S3 지연(500ms) 동안 커넥션 점유 | 트랜잭션 외부로 분리 | 트랜잭션 경계, 커넥션 풀 관리 |
| 3 | 잘못된 트랜잭션 import | jakarta.transaction 사용 | Spring @Transactional | 선언적 트랜잭션, Spring AOP |

---

## 10. Before 측정 포인트 (리팩토링 전 기록)

1. **쿼리 성능**: `spring.jpa.show-sql: true` 활성화 → 미디어 10개 삭제 시 DELETE 쿼리 수 카운트
2. **응답 시간**: DELETE `/v1/trips/{tripKey}` 응답 시간 (S3 파일 수별: 1개, 10개, 50개)
3. **커넥션 풀**: HikariCP 메트릭 → 삭제 요청 중 active connections 수
4. **동시성**: K6로 동시 10명 삭제 요청 → 커넥션 풀 고갈 여부 확인
