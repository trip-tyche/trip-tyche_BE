# MediaProcessingService 코드 흐름 분석

## 1. 클래스 개요

| 항목 | 내용 |
|------|------|
| **위치** | `src/main/java/com/fivefeeling/memory/domain/media/service/MediaProcessingService.java` |
| **역할** | 미디어 파일의 일괄 삭제 처리. 원래는 파일 업로드와 비동기 처리를 담당했으나, 현재는 Trip 삭제 시 관련 미디어 파일을 S3와 DB에서 일괄 삭제하는 단일 기능만 활성화 |
| **어노테이션** | `@Service`, `@Slf4j`, `@RequiredArgsConstructor` |

## 2. 의존성

### 활성화된 의존성

| 빈 | 역할 |
|----|------|
| **S3UploadService** (line 22) | S3 버킷에서 미디어 파일 삭제. AWS SDK 기반 |
| **MediaFileRepository** (line 25) | MediaFile 엔티티 CRUD. Trip에 속한 모든 미디어 파일 조회 및 삭제 |

### 주석 처리된 의존성 (미사용)

| 빈 | 역할 |
|----|------|
| `cpuBoundTaskExecutor` (line 20) | CPU 집약적 작업용 Executor (메타데이터 추출) |
| `ioBoundTaskExecutor` (line 21) | I/O 집약적 작업용 Executor (S3 업로드) |
| `MetadataExtractorService` (line 23) | EXIF 메타데이터 추출 서비스 |
| `PinPointService` (line 24) | 위치 기반 핀포인트 생성/조회 |

## 3. 진입점 (Controller → Service)

### 활성화된 진입점

```
DELETE /v1/trips/{tripKey}

TripController.deleteTrip(userEmail, tripKey)      [TripController.java:115-124]
  └─ TripManagementService.deleteTrip(userEmail, tripId)  [line 104-118]
       └─ MediaProcessingService.deleteMediaFilesByTrip(trip)  [line 113]
```

### 비활성화된 진입점 (주석 처리)

```
POST /api/trips/{tripId}/upload    [FileUploadController.java 전체 주석 처리]

FileUploadController.uploadFiles(tripId, files)
  └─ MediaProcessingService.processFileUpload(trip, files)
```

- **비활성화 이유**: Presigned URL 방식으로 전환 (클라이언트가 직접 S3에 업로드)

## 4. 메서드별 실행 흐름

### 4.1 `deleteMediaFilesByTrip(Trip trip)` (line 101-113) — 활성

**트랜잭션**: `@Transactional` (jakarta.transaction — 문제 있음, 아래 참고)

```
Step 1: MediaFile 조회
  mediaFileRepository.findAllByTrip(trip)
  → Trip에 속한 모든 MediaFile 엔티티 조회

Step 2: MediaKey 추출
  mediaFiles.stream().map(MediaFile::getMediaKey).collect(Collectors.toList())
  → S3 객체 삭제를 위한 키 목록 생성

Step 3: S3 파일 삭제
  if (!mediaKeys.isEmpty())
    s3UploadService.deleteFiles(mediaKeys)
    → AWS SDK DeleteObjectsRequest로 일괄 삭제 (최대 1000개)

Step 4: DB 레코드 삭제
  mediaFileRepository.deleteAll(mediaFiles)
  → JPA deleteAll() — 각 엔티티마다 개별 DELETE 쿼리 실행 (N번)
```

### 4.2 `processFileUpload(Trip, List<MultipartFile>)` (line 27-74) — 주석 처리

**원래 설계된 흐름**:
1. 각 파일마다 `processSingleFileUpload()` 호출
2. `CompletableFuture`로 병렬 처리:
   - CPU 작업: MetadataExtractorService로 EXIF 추출 (cpuBoundTaskExecutor)
   - I/O 작업: S3UploadService로 파일 업로드 (ioBoundTaskExecutor)
3. 두 작업 완료 후 결과 결합 → MediaProcessResult 생성
4. 모든 파일 처리 완료 후 PinPoint 생성 및 MediaFile 배치 저장

### 4.3 `processSingleFileUpload(Trip, MultipartFile)` (line 76-99) — 주석 처리

**원래 설계된 흐름**:
1. `CompletableFuture.supplyAsync()`로 병렬 처리 시작
2. 메타데이터 추출과 S3 업로드 동시 실행
3. `thenCombine()`으로 결과 결합
4. 예외 발생 시 `exceptionally()`로 null 반환

**현재 대체 방식**:
- `PresignedURLController`: POST `/v1/trips/{tripKey}/presigned-url` → 클라이언트에게 Presigned PUT URL 제공
- `MediaMetadataService.processAndSaveMetadataBatch()`: 클라이언트 업로드 완료 후 메타데이터 서버 전송 → MediaFile 엔티티 생성/저장

## 5. 트랜잭션 경계

### 잘못된 트랜잭션 어노테이션 사용

```java
// 현재 (line 7)
import jakarta.transaction.Transactional;

// 올바른 사용 (CLAUDE.md 필수)
import org.springframework.transaction.annotation.Transactional;
```

- `jakarta.transaction.Transactional`은 JTA 전용
- Spring 트랜잭션 관리 기능 (readOnly, propagation, isolation 등) 사용 불가

### 트랜잭션 범위 문제

| 문제 | 설명 |
|------|------|
| **S3 삭제가 트랜잭션 내부** | 네트워크 I/O가 트랜잭션 경계 안에 있어 DB 커넥션 장시간 점유 |
| **원자성 보장 부족** | S3 삭제 성공 + DB 삭제 실패 → 고아 레코드 / S3 삭제 실패 → DB에 레코드 잔존 |
| **N+1 삭제 쿼리** | `deleteAll(mediaFiles)` → 각 엔티티마다 개별 DELETE 실행 |

## 6. 이벤트/비동기 처리

**현재 상태**: 없음

**관련 이벤트**:
- `MediaFileAddedEvent` — MediaMetadataService에서 발행 (MediaProcessingService 미관련)
- `MediaFileDeletedEvent` — MediaMetadataService에서 발행 (MediaProcessingService 미관련)
- `TripDeletedEvent` — TripManagementService에서 발행하지만, MediaProcessingService는 이벤트 리스너가 아닌 직접 호출 방식

## 7. 주석 처리된 코드

### 파일 업로드 처리 로직 (line 27-99, 97줄 — 전체의 84%)

> **CLAUDE.md 지침**: "Presigned URL 리팩토링 시 재작성 예정. 삭제하지 말 것."

**원래 설계 의도**:
- 비동기 병렬 처리: CPU 작업(EXIF 추출)과 I/O 작업(S3 업로드)을 별도 스레드 풀에서 실행
- 멀티파일 동시 처리: `CompletableFuture.allOf()`로 전체 대기
- 배치 저장: 모든 파일 처리 완료 후 `mediaFileRepository.saveAll()` 단일 호출

### 주석 처리된 의존성 (line 19-24)

- `MetadataExtractorService.java`도 전체 주석 처리됨
- `PinPointService`는 MediaMetadataService에서 활성 사용 중

## 8. 개선 포인트

### Critical

| # | 문제 | 위치 | 영향 | 권장 |
|---|------|------|------|------|
| 1 | 잘못된 트랜잭션 어노테이션 | line 7, 101 | Spring 트랜잭션 관리 불가 | `org.springframework.transaction.annotation.Transactional`로 변경 |
| 2 | 트랜잭션 내 외부 I/O 호출 | line 109 | DB 커넥션 장시간 점유, 서비스 장애 전파 | S3 삭제를 트랜잭션 밖으로 분리 |
| 3 | S3-DB 원자성 미보장 | line 108-112 | 부분 실패 시 불일치 상태 | DB 삭제 먼저 → S3 삭제 (실패 시 재시도) |
| 4 | N+1 삭제 쿼리 | line 112 | 파일 수만큼 DELETE 쿼리 실행 | `@Query("DELETE FROM MediaFile m WHERE m.trip = :trip")` 벌크 삭제 |

### Warning

| # | 문제 | 위치 | 권장 |
|---|------|------|------|
| 5 | Entity에 `@Data` 사용 | MediaFile.java:23, Trip.java:31 | `@Getter` + `@NoArgsConstructor(PROTECTED)` + `@Builder`로 변경 |
| 6 | `@AllArgsConstructor` 사용 | MediaFile.java:26, Trip.java:34 | 제거 (`@Builder`만 유지) |
| 7 | `collect(Collectors.toList())` | line 106 | `.toList()` 사용 (Java 16+) |

### Info

| # | 문제 | 위치 | 권장 |
|---|------|------|------|
| 8 | 로깅 누락 | line 101-113 | 삭제 작업 시작/완료 로그 추가 |
| 9 | 예외 처리 누락 | line 108-112 | S3 실패 시에도 DB 삭제 진행하도록 처리 |
| 10 | 대량 주석 처리 코드 | line 19-99 | 리팩토링 완료 후 정리 (현재는 유지) |

## 요약

| 항목 | 내용 |
|------|------|
| **활성 메서드** | `deleteMediaFilesByTrip()` 1개만 활성 |
| **주석 처리** | 97줄 (전체의 84%) — 파일 업로드/비동기 처리 로직 |
| **진입점** | `DELETE /v1/trips/{tripKey}` → TripManagementService → MediaProcessingService |
| **주요 의존성** | S3UploadService, MediaFileRepository |
| **트랜잭션** | `jakarta.transaction.Transactional` (잘못된 패키지) |
| **핵심 문제** | 잘못된 트랜잭션 어노테이션, 트랜잭션 내 외부 I/O, S3-DB 원자성, N+1 삭제 쿼리 |

### 측정 포인트 (Before 리팩토링)

1. `DELETE /v1/trips/{tripKey}` 응답 시간 (MediaFile 10개, 50개, 100개)
2. 실행된 DELETE 쿼리 수 (N+1 확인)
3. DB 커넥션 점유 시간 (S3 API 호출 포함)
4. S3 삭제 실패 시 DB 상태 (불일치 여부)
