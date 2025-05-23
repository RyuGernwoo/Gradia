# Subject 클라우드 동기화 기능

## 개요
기존 CloudSyncManager에서 Subject(과목) 관련 클라우드 동기화 기능을 SubjectRepository로 이동하여, 과목이 추가/수정/삭제될 때 로컬 DB와 서버가 자동으로 동기화되도록 구현했습니다.

## 주요 특징

### 1. 자동 동기화
- **추가**: 과목 추가 시 로컬 DB 저장 후 서버에도 자동 업로드
- **수정**: 과목 수정 시 로컬 DB 업데이트 후 서버에도 자동 반영
- **삭제**: 서버에서 먼저 삭제 후 로컬 DB에서 삭제

### 2. 오프라인 지원
- 로그인되지 않은 상태에서는 로컬 DB만 업데이트
- 서버 ID가 없는 과목의 경우 로컬 DB만 업데이트
- 네트워크 오류 시에도 로컬 작업은 계속 진행

### 3. 사용자 피드백
- 성공/실패 상태를 Toast 메시지로 표시
- 버튼 비활성화로 중복 요청 방지
- 구체적인 오류 메시지 제공

## 구현된 파일들

### SubjectRepository.java
- 클라우드 동기화 로직 구현
- `CloudSyncCallback` 인터페이스 제공
- 기존 메서드와 콜백 지원 메서드 모두 제공

### SubjectViewModel.java
- Repository의 콜백 메서드들을 ViewModel 레벨에서 노출
- 리소스 해제를 위한 `onCleared()` 메서드 추가

### SubjectAddFragment.java
- 과목 추가/수정 시 클라우드 동기화 콜백 사용
- 성공 시 자동으로 이전 화면으로 이동
- 실패 시 오류 메시지 표시 및 버튼 재활성화

### SubjectDeleteDialog.java
- 과목 삭제 시 클라우드 동기화 콜백 사용
- 성공 시 과목 리스트로 이동
- 실패 시 오류 메시지 표시

## 사용법

### 기본 사용 (콜백 없음)
```java
// 기존과 동일하게 사용 가능
viewModel.insert(subject);
viewModel.update(subject);
viewModel.delete(subject);
```

### 콜백을 통한 결과 처리
```java
SubjectRepository.CloudSyncCallback callback = new SubjectRepository.CloudSyncCallback() {
    @Override
    public void onSuccess() {
        // 성공 처리
        Toast.makeText(context, "성공!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String message) {
        // 오류 처리
        Toast.makeText(context, "오류: " + message, Toast.LENGTH_LONG).show();
    }
};

viewModel.insert(subject, callback);
viewModel.update(subject, callback);
viewModel.delete(subject, callback);
```

## 동기화 과정

### 과목 추가
1. 로컬 DB에 과목 저장
2. 로그인 상태이면 서버에 과목 생성 요청
3. 서버에서 생성된 ID와 타임스탬프를 로컬에 업데이트
4. 성공/실패 콜백 호출

### 과목 수정
1. 로컬 DB에 과목 업데이트 (updatedAt 자동 설정)
2. 로그인 상태이고 서버 ID가 있으면 서버에 업데이트 요청
3. 서버에서 받은 타임스탬프를 로컬에 업데이트
4. 성공/실패 콜백 호출

### 과목 삭제
1. 로그인 상태이고 서버 ID가 있으면 서버에서 먼저 삭제
2. 로컬 DB에서 과목 삭제
3. 성공/실패 콜백 호출

## 주의사항

1. **메모리 누수 방지**: ViewModel의 `onCleared()`에서 자동으로 disposables 해제
2. **UI 스레드**: 모든 콜백은 메인 스레드에서 실행되므로 UI 업데이트 안전
3. **중복 요청 방지**: 요청 중에는 버튼을 비활성화하여 중복 요청 방지
4. **오류 처리**: 네트워크 오류나 서버 오류 시에도 로컬 작업은 계속 진행

## 향후 개선 사항

1. **재시도 로직**: 네트워크 오류 시 자동 재시도 기능
2. **배치 동기화**: 여러 과목을 한 번에 동기화하는 기능
3. **충돌 해결**: 동시 수정 시 충돌 해결 로직
4. **진행률 표시**: 대량 동기화 시 진행률 표시 