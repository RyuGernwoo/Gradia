package mp.gradia.api;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mp.gradia.api.models.EvaluationRatio;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.StudySessionsApiResponse;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.TargetStudyTime;
import mp.gradia.database.AppDatabase;
import mp.gradia.database.dao.StudySessionDao;
import mp.gradia.database.dao.SubjectDao;
import mp.gradia.database.entity.StudySessionEntity;
import mp.gradia.database.entity.SubjectEntity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CloudSyncManagerTest {

    @Mock
    private Context mockContext;

    @Mock
    private ApiService mockApiService;

    @Mock
    private AuthManager mockAuthManager;

    @Mock
    private AppDatabase mockDatabase;

    @Mock
    private SubjectDao mockSubjectDao;

    @Mock
    private StudySessionDao mockStudySessionDao;

    @Mock
    private Call<SubjectsApiResponse> mockSubjectsCall;

    @Mock
    private Call<StudySessionsApiResponse> mockSessionsCall;

    @Mock
    private Call<Subject> mockSubjectCall;

    @Mock
    private Call<StudySession> mockSessionCall;

    private CloudSyncManager cloudSyncManager;

    private final String AUTH_HEADER = "Bearer mockToken";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // 조회용 DB 모킹
        when(mockDatabase.subjectDao()).thenReturn(mockSubjectDao);
        when(mockDatabase.studySessionDao()).thenReturn(mockStudySessionDao);

        // RxJava 스케줄러 설정 (테스트에서 메인 스레드 문제 없애기 위함)
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
        RxAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
        // AndroidSchedulers.mainThread() 문제 해결을 위한 추가 설정
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());

        // Mockito 셋업: RetrofitClient 대신 mock API 서비스 사용
        RetrofitClient.setMockApiServiceForTesting(mockApiService);

        // AuthManager 설정
        when(mockAuthManager.isLoggedIn()).thenReturn(true);
        when(mockAuthManager.getAuthHeader()).thenReturn(AUTH_HEADER);

        // 의존성 모킹
        AuthManager.setInstanceForTesting(mockAuthManager);
        AppDatabase.setInstanceForTesting(mockDatabase);

        // CloudSyncManager 인스턴스 생성
        cloudSyncManager = new CloudSyncManager(mockContext);
    }

    @After
    public void tearDown() {
        // 리소스 해제
        cloudSyncManager.dispose();

        // 스케줄러 리셋
        RxJavaPlugins.reset();
        RxAndroidPlugins.reset();

        // 싱글톤 리셋
        RetrofitClient.resetMockForTesting();
        AuthManager.resetInstanceForTesting();
        AppDatabase.resetInstanceForTesting();
    }

    /**
     * 로그인되지 않은 상태에서 서버 업로드 시도 시 에러 콜백이 호출되는지 테스트합니다.
     */
    @Test
    public void testUploadToServer_WhenNotLoggedIn_ShouldReturnErrorCallback() {
        // Given: 로그인되지 않은 상태
        when(mockAuthManager.isLoggedIn()).thenReturn(false);

        // When: 서버 업로드 시도
        final boolean[] errorCalled = { false };
        final String[] errorMessage = { null };

        cloudSyncManager.uploadToServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                fail("성공 콜백이 호출되면 안됩니다");
            }

            @Override
            public void onError(String message) {
                errorCalled[0] = true;
                errorMessage[0] = message;
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        // Then: 에러 콜백이 호출되어야 함
        assertTrue("에러 콜백이 호출되어야 합니다", errorCalled[0]);
        assertEquals("로그인이 필요합니다.", errorMessage[0]);
    }

    /**
     * 로그인되지 않은 상태에서 서버로부터 다운로드 시도 시 에러 콜백이 호출되는지 테스트합니다.
     */
    @Test
    public void testDownloadFromServer_WhenNotLoggedIn_ShouldReturnErrorCallback() {
        // Given: 로그인되지 않은 상태
        when(mockAuthManager.isLoggedIn()).thenReturn(false);

        // When: 서버에서 다운로드 시도
        final boolean[] errorCalled = { false };
        final String[] errorMessage = { null };

        cloudSyncManager.downloadFromServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                fail("성공 콜백이 호출되면 안됩니다");
            }

            @Override
            public void onError(String message) {
                errorCalled[0] = true;
                errorMessage[0] = message;
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        // Then: 에러 콜백이 호출되어야 함
        assertTrue("에러 콜백이 호출되어야 합니다", errorCalled[0]);
        assertEquals("로그인이 필요합니다.", errorMessage[0]);
    }

    /**
     * 과목 및 학습 세션 데이터가 있을 때 서버 업로드가 성공하는지 테스트합니다.
     * 로컬 DB의 모든 과목과 학습 세션이 성공적으로 서버에 업로드되고,
     * 해당 API 호출이 정확히 이루어지는지 확인합니다.
     */
    @Test
    public void testUploadToServer_WithSubjectsAndSessions_ShouldSucceed() {
        // Given: 로그인 상태 및 DB에 데이터가 존재
        when(mockAuthManager.isLoggedIn()).thenReturn(true);

        // 과목 모킹
        List<SubjectEntity> mockSubjects = createMockSubjectEntities();
        doReturn(Flowable.just(mockSubjects)).when(mockSubjectDao).getAll();

        // 이 테스트는 업로드에 관한 것이므로 clearAll 메소드 호출은 필요하지 않음
        // doReturn(Completable.complete()).when(mockSubjectDao).clearAll(); - 삭제

        // API 호출 모킹
        when(mockApiService.updateSubject(eq(AUTH_HEADER), anyString(), any(Subject.class)))
                .thenReturn(mockSubjectCall);
        when(mockApiService.createSubject(eq(AUTH_HEADER), any(Subject.class)))
                .thenReturn(mockSubjectCall);
        mockApiCallsForSubjectUpload();

        // 학습 세션 모킹
        List<StudySessionEntity> mockSessions = createMockStudySessionEntities();
        doReturn(Flowable.just(mockSessions)).when(mockStudySessionDao).getAll();

        // 이 테스트는 업로드에 관한 것이므로 clearAll 메소드 호출은 필요하지 않음
        // doReturn(Completable.complete()).when(mockStudySessionDao).clearAll(); - 삭제

        // API 호출 모킹
        when(mockApiService.updateStudySession(eq(AUTH_HEADER), anyString(), any(StudySession.class)))
                .thenReturn(mockSessionCall);
        when(mockApiService.createStudySession(eq(AUTH_HEADER), any(StudySession.class)))
                .thenReturn(mockSessionCall);
        mockApiCallsForSessionUpload();

        // When: 서버 업로드 시도
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] successCalled = { false };
        final boolean[] errorCalled = { false };
        final String[] errorMessage = { null };

        cloudSyncManager.uploadToServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorCalled[0] = true;
                errorMessage[0] = message;
                latch.countDown();
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("테스트 타임아웃");
        }

        // Then: 성공 콜백이 호출되어야 함
        assertTrue("성공 콜백이 호출되어야 합니다", successCalled[0]);
        assertFalse("에러 콜백이 호출되면 안됩니다", errorCalled[0]);

        // API 메서드가 호출되었는지 확인
        verify(mockApiService).getSubjects(AUTH_HEADER); // 서버 과목 조회 확인
        verify(mockApiService).getStudySessions(eq(AUTH_HEADER), isNull()); // 서버 세션 조회 확인

        // 과목 및 세션이 생성/업데이트되었는지 확인
        verify(mockSubjectDao, atLeastOnce()).update(any(SubjectEntity.class)); // 과목 업데이트 확인
        verify(mockStudySessionDao, atLeastOnce()).update(any(StudySessionEntity.class)); // 세션 업데이트 확인
    }

    /**
     * 서버에 데이터가 존재할 때, 서버로부터 데이터를 성공적으로 다운로드하는지 테스트합니다.
     * 서버로부터 과목 및 학습 세션 데이터를 가져와 로컬 DB에 올바르게 저장하는지 확인합니다.
     */
    @Test
    public void testDownloadFromServer_WithServerData_ShouldSucceed() {
        // Given: 로그인 상태 및 서버에 데이터가 존재
        when(mockAuthManager.isLoggedIn()).thenReturn(true);

        // 과목 DB 작업 모킹
        doReturn(Completable.complete()).when(mockSubjectDao).clearAll();
        doReturn(Completable.complete()).when(mockSubjectDao).insert(any(SubjectEntity[].class));

        // 학습 세션 DB 작업 모킹
        doReturn(Completable.complete()).when(mockStudySessionDao).clearAll();
        doReturn(Completable.complete()).when(mockStudySessionDao).insert(any(StudySessionEntity[].class));

        // API 응답 모킹 - 테스트 데이터가 포함된 응답 생성
        mockApiResponsesForDownload();

        // When: 서버에서 다운로드 시도
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] successCalled = { false };
        final boolean[] errorCalled = { false };
        final String[] errorMessage = { null };

        cloudSyncManager.downloadFromServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                errorCalled[0] = true;
                errorMessage[0] = message;
                latch.countDown();
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("테스트 타임아웃");
        }

        // Then: 성공 콜백이 호출되어야 함
        assertTrue("성공 콜백이 호출되어야 합니다", successCalled[0]);
        assertFalse("에러 콜백이 호출되면 안됩니다", errorCalled[0]);

        // API 메서드 호출 확인
        verify(mockApiService).getSubjects(AUTH_HEADER);
        verify(mockApiService).getStudySessions(eq(AUTH_HEADER), isNull());

        // DB 작업 확인
        verify(mockSubjectDao).clearAll(); // 기존 과목 데이터 삭제
        verify(mockSubjectDao).insert(any(SubjectEntity[].class)); // 새 과목 데이터 삽입
        verify(mockStudySessionDao).clearAll(); // 기존 세션 데이터 삭제
        verify(mockStudySessionDao).insert(any(StudySessionEntity[].class)); // 새 세션 데이터 삽입
    }

    /**
     * 로컬 DB의 SubjectEntity 모델이 API 통신을 위한 Subject 모델로 올바르게 변환되는지 테스트합니다.
     */
    @Test
    public void testModelConversion_SubjectEntityToApiSubject() {
        // Given: SubjectEntity 준비
        SubjectEntity localSubject = createSampleSubjectEntity();

        // When: convertToApiSubject 메서드 호출
        Subject apiSubject = invokeConvertToApiSubject(localSubject);

        // Then: 변환 결과 검증
        assertNotNull("API Subject는 null이 아니어야 합니다", apiSubject);
        assertEquals("1", apiSubject.getId());
        assertEquals("테스트 과목", apiSubject.getName());
        assertEquals(3, apiSubject.getCredit());
        assertEquals(1, apiSubject.getType());
        assertEquals(4, (int) apiSubject.getDifficulty());
        assertEquals("2023-06-15", apiSubject.getMid_term_schedule());
        assertEquals("2023-06-30", apiSubject.getFinal_term_schedule());
        assertEquals("#FF5733", apiSubject.getColor());

        // 평가 비율 검증
        assertNotNull("평가 비율은 null이 아니어야 합니다", apiSubject.getEvaluation_ratio());
        assertEquals(30, apiSubject.getEvaluation_ratio().getMid_term_ratio());
        assertEquals(40, apiSubject.getEvaluation_ratio().getFinal_term_ratio());
        assertEquals(10, apiSubject.getEvaluation_ratio().getQuiz_ratio());
        assertEquals(15, apiSubject.getEvaluation_ratio().getAssignment_ratio());
        assertEquals(5, apiSubject.getEvaluation_ratio().getAttendance_ratio());

        // 목표 공부 시간 검증
        assertNotNull("목표 공부 시간은 null이 아니어야 합니다", apiSubject.getTarget_study_time());
        assertEquals(60, apiSubject.getTarget_study_time().getDaily_target_study_time());
        assertEquals(300, apiSubject.getTarget_study_time().getWeekly_target_study_time());
        assertEquals(1200, apiSubject.getTarget_study_time().getMonthly_target_study_time());
    }

    /**
     * 로컬 DB의 StudySessionEntity 모델이 API 통신을 위한 StudySession 모델로 올바르게 변환되는지 테스트합니다.
     */
    @Test
    public void testModelConversion_StudySessionEntityToApiStudySession() {
        // Given: StudySessionEntity 준비
        StudySessionEntity localSession = createSampleStudySessionEntity();

        // When: convertToApiStudySession 메서드 호출 (리플렉션 이용)
        StudySession apiSession = invokeConvertToApiStudySession(localSession);

        // Then: 변환 결과 검증
        assertNotNull("API StudySession은 null이 아니어야 합니다", apiSession);
        assertEquals("1", apiSession.getId());
        assertEquals("2", apiSession.getSubject_id());
        assertEquals("2023-05-20", apiSession.getDate());
        assertEquals(120, (int) apiSession.getStudy_time());
        assertEquals("09:00:00", apiSession.getStart_time());
        assertEquals("11:00:00", apiSession.getEnd_time());
        assertEquals(15, (int) apiSession.getRest_time());
    }

    /**
     * 로컬과 서버 모두에 존재하는 데이터의 업데이트 시간을 비교하여
     * 로컬 데이터가 더 최신일 때 서버 데이터를 업데이트하는지 테스트합니다.
     */
    @Test
    public void testUploadToServer_WhenLocalDataIsNewer_ShouldUpdateServer() {
        // Given: 로그인 상태 및 로컬과 서버 모두에 데이터가 존재
        when(mockAuthManager.isLoggedIn()).thenReturn(true);

        // 서버에 있는 데이터보다 최신 시간을 가진 로컬 데이터 생성
        List<SubjectEntity> mockSubjects = new ArrayList<>();
        SubjectEntity newerSubject = createSampleSubjectEntity();
        newerSubject.setServerId("1"); // 서버와 동일한 ID를 가지지만
        newerSubject.setUpdatedAt(java.time.LocalDateTime.now()); // 현재 시간으로 업데이트(서버보다 최신)
        mockSubjects.add(newerSubject);

        doReturn(Flowable.just(mockSubjects)).when(mockSubjectDao).getAll();

        // API 호출 모킹 - 서버에 있는 과목 데이터 반환(더 오래된 업데이트 시간)
        when(mockApiService.getSubjects(AUTH_HEADER)).thenReturn(mockSubjectsCall);

        doAnswer(invocation -> {
            Callback<SubjectsApiResponse> callback = invocation.getArgument(0);

            SubjectsApiResponse response = new SubjectsApiResponse();
            response.setMessage("Success");

            List<Subject> subjects = new ArrayList<>();
            Subject olderSubject = new Subject();
            olderSubject.setId("1");
            olderSubject.setName("테스트 과목");
            olderSubject.setUpdated_at("2023-01-01T00:00:00Z"); // 오래된 업데이트 시간

            subjects.add(olderSubject);
            response.setSubjects(subjects);

            callback.onResponse(mockSubjectsCall, Response.success(response));
            return null;
        }).when(mockSubjectsCall).enqueue(any());

        // 과목 업데이트 API 모킹
        when(mockApiService.updateSubject(eq(AUTH_HEADER), eq("1"), any(Subject.class)))
                .thenReturn(mockSubjectCall);

        doAnswer(invocation -> {
            Callback<Subject> callback = invocation.getArgument(0);
            Subject updatedSubject = new Subject();
            updatedSubject.setId("1");
            updatedSubject.setUpdated_at(
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME));

            callback.onResponse(mockSubjectCall, Response.success(updatedSubject));
            return null;
        }).when(mockSubjectCall).enqueue(any());

        // 학습 세션 모킹 - 빈 세션 목록
        doReturn(Flowable.just(new ArrayList<>())).when(mockStudySessionDao).getAll();
        when(mockApiService.getStudySessions(eq(AUTH_HEADER), isNull())).thenReturn(mockSessionsCall);

        doAnswer(invocation -> {
            Callback<StudySessionsApiResponse> callback = invocation.getArgument(0);
            StudySessionsApiResponse response = new StudySessionsApiResponse();
            response.setMessage("Success");
            response.setSessions(new ArrayList<>());

            callback.onResponse(mockSessionsCall, Response.success(response));
            return null;
        }).when(mockSessionsCall).enqueue(any());

        // When: 서버 업로드 시도
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] successCalled = { false };

        cloudSyncManager.uploadToServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                latch.countDown();
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("테스트 타임아웃");
        }

        // Then: 성공 콜백이 호출되고 서버 업데이트 API가 호출되어야 함
        assertTrue("성공 콜백이 호출되어야 합니다", successCalled[0]);
        verify(mockApiService).updateSubject(eq(AUTH_HEADER), eq("1"), any(Subject.class));
    }

    /**
     * 로컬과 서버 모두에 존재하는 데이터의 업데이트 시간을 비교하여
     * 서버 데이터가 더 최신일 때 로컬 데이터가 업데이트되지 않는지 테스트합니다.
     */
    @Test
    public void testUploadToServer_WhenServerDataIsNewer_ShouldNotUpdateServer() {
        // Given: 로그인 상태 및 로컬과 서버 모두에 데이터가 존재
        when(mockAuthManager.isLoggedIn()).thenReturn(true);

        // 서버에 있는 데이터보다 오래된 시간을 가진 로컬 데이터 생성
        List<SubjectEntity> mockSubjects = new ArrayList<>();
        SubjectEntity olderSubject = createSampleSubjectEntity();
        olderSubject.setServerId("1"); // 서버와 동일한 ID
        olderSubject.setUpdatedAt(java.time.LocalDateTime.of(2023, 1, 1, 0, 0)); // 오래된 업데이트 시간
        mockSubjects.add(olderSubject);

        doReturn(Flowable.just(mockSubjects)).when(mockSubjectDao).getAll();

        // API 호출 모킹 - 서버에 있는 과목 데이터 반환(더 최신 업데이트 시간)
        when(mockApiService.getSubjects(AUTH_HEADER)).thenReturn(mockSubjectsCall);

        doAnswer(invocation -> {
            Callback<SubjectsApiResponse> callback = invocation.getArgument(0);

            SubjectsApiResponse response = new SubjectsApiResponse();
            response.setMessage("Success");

            List<Subject> subjects = new ArrayList<>();
            Subject newerSubject = new Subject();
            newerSubject.setId("1");
            newerSubject.setName("테스트 과목");
            newerSubject.setUpdated_at(
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)); // 최신 업데이트
                                                                                                             // 시간

            subjects.add(newerSubject);
            response.setSubjects(subjects);

            callback.onResponse(mockSubjectsCall, Response.success(response));
            return null;
        }).when(mockSubjectsCall).enqueue(any());

        // 학습 세션 모킹 - 빈 세션 목록
        doReturn(Flowable.just(new ArrayList<>())).when(mockStudySessionDao).getAll();
        when(mockApiService.getStudySessions(eq(AUTH_HEADER), isNull())).thenReturn(mockSessionsCall);

        doAnswer(invocation -> {
            Callback<StudySessionsApiResponse> callback = invocation.getArgument(0);
            StudySessionsApiResponse response = new StudySessionsApiResponse();
            response.setMessage("Success");
            response.setSessions(new ArrayList<>());

            callback.onResponse(mockSessionsCall, Response.success(response));
            return null;
        }).when(mockSessionsCall).enqueue(any());

        // When: 서버 업로드 시도
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] successCalled = { false };

        cloudSyncManager.uploadToServer(new CloudSyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String message) {
                latch.countDown();
            }

            @Override
            public void onProgress(int progress, int total) {
                // 진행 상황 무시
            }
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail("테스트 타임아웃");
        }

        // Then: 성공 콜백이 호출되고, 서버 업데이트 API가 호출되지 않아야 함
        assertTrue("성공 콜백이 호출되어야 합니다", successCalled[0]);
        verify(mockApiService, never()).updateSubject(eq(AUTH_HEADER), eq("1"), any(Subject.class));
    }

    // Helper 메서드 - 테스트용 과목 엔티티 생성
    private List<SubjectEntity> createMockSubjectEntities() {
        List<SubjectEntity> subjects = new ArrayList<>();
        subjects.add(createSampleSubjectEntity());

        SubjectEntity subject2 = new SubjectEntity("수학", 3, "#3355FF", 2, "2023-06-20", "2023-07-10",
                new mp.gradia.database.entity.EvaluationRatio(35, 35, 10, 15, 5),
                new mp.gradia.database.entity.TargetStudyTime(90, 400, 1600));
        subject2.setSubjectId(2);
        subjects.add(subject2);

        return subjects;
    }

    // Helper 메서드 - 테스트용 학습 세션 엔티티 생성
    private List<StudySessionEntity> createMockStudySessionEntities() {
        List<StudySessionEntity> sessions = new ArrayList<>();
        sessions.add(createSampleStudySessionEntity());

        StudySessionEntity session2 = new StudySessionEntity(
                2,
                LocalDate.of(2023, 5, 21),
                90,
                LocalTime.of(14, 0),
                LocalTime.of(15, 30),
                10);
        session2.setSessionId(2);
        sessions.add(session2);

        return sessions;
    }

    // Helper 메서드 - 테스트용 샘플 과목 엔티티 생성
    private SubjectEntity createSampleSubjectEntity() {
        mp.gradia.database.entity.EvaluationRatio ratio = new mp.gradia.database.entity.EvaluationRatio(
                30, 40, 10, 15, 5);

        mp.gradia.database.entity.TargetStudyTime time = new mp.gradia.database.entity.TargetStudyTime(
                60, 300, 1200);

        SubjectEntity subject = new SubjectEntity(
                "테스트 과목", 3, "#FF5733", 1, "2023-06-15", "2023-06-30", ratio, time);
        subject.setSubjectId(1);
        subject.setDifficulty(4);

        return subject;
    }

    // Helper 메서드 - 테스트용 샘플 학습 세션 엔티티 생성
    private StudySessionEntity createSampleStudySessionEntity() {
        StudySessionEntity session = new StudySessionEntity(
                2,
                LocalDate.of(2023, 5, 20),
                120,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                15);
        session.setSessionId(1);
        session.setServerId("1");

        return session;
    }

    // Helper 메서드 - 과목 업로드 API 호출 모킹
    private void mockApiCallsForSubjectUpload() {
        // 서버로부터 과목 조회 응답 모킹 추가
        when(mockApiService.getSubjects(AUTH_HEADER)).thenReturn(mockSubjectsCall);

        doAnswer(invocation -> {
            Callback<SubjectsApiResponse> callback = invocation.getArgument(0);

            SubjectsApiResponse response = new SubjectsApiResponse();
            response.setMessage("Success");
            response.setSubjects(new ArrayList<>()); // 빈 과목 목록 반환

            callback.onResponse(mockSubjectsCall, Response.success(response));
            return null;
        }).when(mockSubjectsCall).enqueue(any());

        // 과목 생성/업데이트 응답 모킹
        doAnswer(invocation -> {
            Callback<Subject> callback = invocation.getArgument(0);

            Subject responseSubject = new Subject();
            responseSubject.setId("1");
            responseSubject.setCreated_at("2023-05-20T09:00:00Z");
            responseSubject.setUpdated_at("2023-05-20T09:00:00Z");

            callback.onResponse(mockSubjectCall, Response.success(responseSubject));
            return null;
        }).when(mockSubjectCall).enqueue(any());
    }

    // Helper 메서드 - 학습 세션 업로드 API 호출 모킹
    private void mockApiCallsForSessionUpload() {
        // 서버로부터 세션 조회 응답 모킹 추가
        when(mockApiService.getStudySessions(eq(AUTH_HEADER), isNull())).thenReturn(mockSessionsCall);

        doAnswer(invocation -> {
            Callback<StudySessionsApiResponse> callback = invocation.getArgument(0);

            StudySessionsApiResponse response = new StudySessionsApiResponse();
            response.setMessage("Success");
            response.setSessions(new ArrayList<>()); // 빈 세션 목록 반환

            callback.onResponse(mockSessionsCall, Response.success(response));
            return null;
        }).when(mockSessionsCall).enqueue(any());

        // 세션 생성/업데이트 응답 모킹
        doAnswer(invocation -> {
            Callback<StudySession> callback = invocation.getArgument(0);

            StudySession responseSession = new StudySession();
            responseSession.setId("1");
            responseSession.setCreated_at("2023-05-20T09:00:00Z");
            responseSession.setUpdated_at("2023-05-20T09:00:00Z");

            callback.onResponse(mockSessionCall, Response.success(responseSession));
            return null;
        }).when(mockSessionCall).enqueue(any());
    }

    // Helper 메서드 - 다운로드 API 응답 모킹
    private void mockApiResponsesForDownload() {
        // 과목 조회 응답 모킹
        when(mockApiService.getSubjects(AUTH_HEADER)).thenReturn(mockSubjectsCall);

        doAnswer(invocation -> {
            Callback<SubjectsApiResponse> callback = invocation.getArgument(0);

            SubjectsApiResponse response = new SubjectsApiResponse();
            response.setMessage("Success");

            List<Subject> subjects = new ArrayList<>();

            Subject subject1 = new Subject();
            subject1.setId("1");
            subject1.setName("API 과목 1");
            subject1.setCredit(3);
            subject1.setType(1);
            subject1.setColor("#FF0000");
            subject1.setCreated_at("2023-05-20T09:00:00Z");
            subject1.setUpdated_at("2023-05-20T09:00:00Z");

            EvaluationRatio ratio1 = new EvaluationRatio();
            ratio1.setMid_term_ratio(30);
            ratio1.setFinal_term_ratio(40);
            ratio1.setQuiz_ratio(10);
            ratio1.setAssignment_ratio(15);
            ratio1.setAttendance_ratio(5);
            subject1.setEvaluation_ratio(ratio1);

            TargetStudyTime time1 = new TargetStudyTime();
            time1.setDaily_target_study_time(60);
            time1.setWeekly_target_study_time(300);
            time1.setMonthly_target_study_time(1200);
            subject1.setTarget_study_time(time1);

            subjects.add(subject1);
            response.setSubjects(subjects);

            callback.onResponse(mockSubjectsCall, Response.success(response));
            return null;
        }).when(mockSubjectsCall).enqueue(any());

        // 학습 세션 조회 응답 모킹
        when(mockApiService.getStudySessions(eq(AUTH_HEADER), isNull())).thenReturn(mockSessionsCall);

        doAnswer(invocation -> {
            Callback<StudySessionsApiResponse> callback = invocation.getArgument(0);

            StudySessionsApiResponse response = new StudySessionsApiResponse();
            response.setMessage("Success");

            List<StudySession> sessions = new ArrayList<>();

            StudySession session1 = new StudySession();
            session1.setId("1");
            session1.setSubject_id("1");
            session1.setDate("2023-05-20");
            session1.setStudy_time(120);
            session1.setStart_time("09:00:00");
            session1.setEnd_time("11:00:00");
            session1.setRest_time(15);
            session1.setCreated_at("2023-05-20T09:00:00Z");
            session1.setUpdated_at("2023-05-20T09:00:00Z");

            sessions.add(session1);
            response.setSessions(sessions);

            callback.onResponse(mockSessionsCall, Response.success(response));
            return null;
        }).when(mockSessionsCall).enqueue(any());
    }

    // Helper 메서드 - private 메서드 호출을 위한 리플렉션 유틸리티
    private Subject invokeConvertToApiSubject(SubjectEntity localSubject) {
        try {
            java.lang.reflect.Method method = CloudSyncManager.class.getDeclaredMethod(
                    "convertToApiSubject", SubjectEntity.class);
            method.setAccessible(true);
            return (Subject) method.invoke(cloudSyncManager, localSubject);
        } catch (Exception e) {
            e.printStackTrace();
            fail("리플렉션을 통한 메서드 호출 실패: " + e.getMessage());
            return null;
        }
    }

    private StudySession invokeConvertToApiStudySession(StudySessionEntity localSession) {
        try {
            java.lang.reflect.Method method = CloudSyncManager.class.getDeclaredMethod(
                    "convertToApiStudySession", StudySessionEntity.class);
            method.setAccessible(true);
            return (StudySession) method.invoke(cloudSyncManager, localSession);
        } catch (Exception e) {
            e.printStackTrace();
            fail("리플렉션을 통한 메서드 호출 실패: " + e.getMessage());
            return null;
        }
    }
}
