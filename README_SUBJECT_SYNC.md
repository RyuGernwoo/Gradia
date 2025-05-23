# Subject 클라우드 동기화 시스템

## 개요
과목(Subject) 데이터에 대한 완전한 클라우드 동기화 시스템을 구현했습니다. 이 시스템은 로컬 DB를 우선으로 하여 사용자 경험을 보장하면서도, 백그라운드에서 클라우드와 자동 동기화를 수행합니다.

## 🚀 주요 특징

### 1. 로컬 우선 정책
- **즉시 성공**: 모든 CRUD 작업은 로컬 DB 성공 시 즉시 완료 처리
- **백그라운드 동기화**: 서버 동기화는 별도 스레드에서 비동기 수행
- **오프라인 지원**: 네트워크 없이도 완전한 기능 사용 가능

### 2. 자동 복구 동기화
- **Fragment 시작 시**: SubjectFragment 생성/재개 시 자동 동기화 체크
- **누락 항목 복구**: 이전에 실패한 동기화 항목들 자동 재시도
- **충돌 해결**: 로컬 DB 기준으로 서버 데이터 업데이트

### 3. 견고한 오류 처리
- **부분 실패 허용**: 서버 오류가 발생해도 로컬 작업은 성공
- **자동 재시도**: Fragment 재시작 시 실패한 동기화 재시도
- **상세 로깅**: 모든 동기화 과정에 대한 상세 로그

## 📁 수정된 파일들

### SubjectRepository.java
```java
// 새로운 기능들
- syncLocalToCloud(callback)           // 로컬 기준 동기화
- performLocalToCloudSync()           // 동기화 로직
- performBatchSync()                  // 배치 처리
- isLocalNewer()                      // 타임스탬프 비교
```

### SubjectViewModel.java
```java
// 추가된 메서드
- syncLocalToCloud(callback)          // Repository 동기화 호출
```

### SubjectFragment.java
```java
// 새로운 라이프사이클 처리
- onViewCreated()                     // 초기 동기화
- onResume()                          // 재시작 시 동기화
- performInitialSync()                // 동기화 수행
```

## 🔄 동기화 시나리오

### 시나리오 1: 정상적인 온라인 사용
```
1. 사용자가 과목 추가
2. 로컬 DB에 즉시 저장 → UI 즉시 업데이트
3. 백그라운드에서 서버에 업로드
4. 서버 ID와 타임스탬프를 로컬에 업데이트
```

### 시나리오 2: 네트워크 오류 발생
```
1. 사용자가 과목 추가
2. 로컬 DB에 즉시 저장 → UI 즉시 업데이트
3. 서버 업로드 실패 (serverId = null 상태 유지)
4. 다음번 Fragment 시작 시 자동으로 서버에 업로드 재시도
```

### 시나리오 3: 앱 재시작 후 동기화
```
1. Fragment가 시작됨 (onViewCreated)
2. 로컬 DB에서 과목 목록 조회
3. 서버에서 과목 목록 조회
4. 비교 분석:
   - serverId가 null인 항목 → 서버에 생성
   - 로컬이 더 최신인 항목 → 서버 업데이트
   - 서버에만 있는 항목 → 무시 (로컬 기준)
```

## 💻 사용법

### 기본 사용 (기존과 동일)
```java
// ViewModel에서
viewModel.insert(subject);
viewModel.update(subject);
viewModel.delete(subject);
```

### 콜백을 통한 상세 제어
```java
SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
    @Override
    public void onSuccess() {
        // 로컬 작업 성공 (서버 동기화는 백그라운드에서 진행)
        Toast.makeText(context, "저장 완료!", Toast.LENGTH_SHORT).show();
        // 화면 이동 등의 후속 작업 수행
    }

    @Override
    public void onError(String message) {
        // 로컬 작업도 실패한 경우 (매우 드문 상황)
        Toast.makeText(context, "오류: " + message, Toast.LENGTH_LONG).show();
    }
};

viewModel.insert(subject, callback);
viewModel.update(subject, callback);
viewModel.delete(subject, callback);
```

### 수동 동기화
```java
// 필요시 수동으로 동기화 수행
viewModel.syncLocalToCloud(new SubjectRepository.CloudSyncCallback() {
    @Override
    public void onSuccess() {
        Log.d("Sync", "동기화 완료");
    }

    @Override
    public void onError(String message) {
        Log.w("Sync", "동기화 실패: " + message);
    }
});
```

## 🔍 동기화 상태 확인

### 로그를 통한 모니터링
```
D/SubjectRepository: 로컬 과목 생성 완료: 수학
D/SubjectRepository: 서버 동기화 완료: 수학
```

```
D/SubjectRepository: 로컬 과목 생성 완료: 물리학
W/SubjectRepository: 서버 동기화 실패 (나중에 재시도): 물리학
```

```
D/SubjectFragment: 초기 과목 동기화 시작
D/SubjectRepository: 동기화 필요: 생성 1개, 업데이트 0개
D/SubjectRepository: 과목 생성 동기화 완료: 물리학
```

### 데이터베이스에서 동기화 상태 확인
```java
// serverId가 null인 경우 → 서버 동기화 대기 중
// serverId가 있는 경우 → 서버와 동기화 완료
```

## ⚙️ 고급 설정

### 동기화 주기 조정
현재는 Fragment 시작/재개 시마다 동기화를 수행하지만, 필요에 따라 조정 가능:

```java
// SubjectFragment.java에서
@Override
public void onResume() {
    super.onResume();
    // 더 자주 동기화하거나, 조건부로 동기화 수행 가능
    if (shouldPerformSync()) {
        performInitialSync();
    }
}
```

### 배치 크기 조정
```java
// performBatchSync()에서 한 번에 처리할 최대 항목 수 조정 가능
```

## 🛡️ 오류 처리 전략

### 1. 로컬 DB 오류
- 즉시 사용자에게 알림
- 재시도 옵션 제공

### 2. 네트워크 오류
- 로그에만 기록
- 자동 재시도 (다음 Fragment 시작 시)

### 3. 서버 API 오류
- 로그에 기록
- HTTP 상태 코드별 대응 가능

## 📊 성능 최적화

### 1. 비동기 처리
- 모든 DB 작업: 백그라운드 스레드
- UI 업데이트: 메인 스레드
- 네트워크 요청: IO 스레드

### 2. 메모리 관리
- ViewModel에서 자동 리소스 해제
- CompositeDisposable로 구독 관리

### 3. 네트워크 최적화
- 필요한 경우에만 동기화 수행
- 배치 처리로 여러 항목 한 번에 처리

## 🚨 주의사항

1. **serverId 필드**: 동기화 상태를 나타내는 중요한 필드
2. **타임스탬프**: 충돌 해결의 기준이 되는 중요한 데이터
3. **네트워크 권한**: 앱에 인터넷 권한 필요
4. **로그인 상태**: 클라우드 동기화는 로그인 상태에서만 동작

## 🔮 향후 확장 가능성

1. **실시간 동기화**: WebSocket을 이용한 실시간 업데이트
2. **충돌 해결 UI**: 사용자가 직접 충돌을 해결할 수 있는 인터페이스
3. **동기화 상태 표시**: UI에서 동기화 상태를 시각적으로 표시
4. **선택적 동기화**: 사용자가 동기화할 항목을 선택할 수 있는 기능 