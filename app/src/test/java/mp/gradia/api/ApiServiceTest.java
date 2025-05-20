package mp.gradia.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import mp.gradia.api.models.AuthResponse;
import mp.gradia.api.models.StudySession;
import mp.gradia.api.models.Subject;
import mp.gradia.api.models.SubjectsApiResponse;
import mp.gradia.api.models.UserInfo;
import mp.gradia.api.models.StudySessionsApiResponse;
import retrofit2.Response;

public class ApiServiceTest {

    private ApiService apiService;
    private String authToken;
    private String testSubjectId; // Used to store ID for single subject test, cleaned up in its own test
    private String testSessionId; // Used to store ID for single session test, cleaned up in its own test

    @Before
    public void setUp() throws IOException {
        apiService = RetrofitClient.getApiService();
        assertNotNull("ApiService should not be null", apiService);

        Response<AuthResponse> tempUserResponse = apiService.getTempUser().execute();
        String errorBody = tempUserResponse.errorBody() != null ? tempUserResponse.errorBody().string()
                : "Error body is null";
        assertTrue("Failed to create temp user: " + errorBody, tempUserResponse.isSuccessful());
        assertNotNull("AuthResponse should not be null", tempUserResponse.body());
        authToken = "Bearer " + tempUserResponse.body().getAccess_token();
        assertNotNull("Auth token should not be null", authToken);
        System.out.println("Temporary user created successfully. Token: " + authToken);
    }

    @Test
    public void testGetCurrentUser() throws IOException {
        assertNotNull("Auth token is null, make sure setUp ran correctly.", authToken);
        Response<UserInfo> response = apiService.getCurrentUser(authToken).execute();
        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Error body is null";
        assertTrue("Failed to get current user: " + errorBody, response.isSuccessful());
        assertNotNull("UserInfo should not be null", response.body());
        System.out.println("Current User: " + response.body().toString()); // 실제 UserInfo의 toString() 구현에 따라 출력内容 달라짐
    }

    @Test
    public void testSubjectWorkflow() throws IOException {
        assertNotNull("Auth token is null for subject workflow.", authToken);

        // 1. Create Subject
        Subject newSubject = new Subject();
        String subjectName = "Test Subject Workflow " + System.currentTimeMillis(); // Unique name
        newSubject.setName(subjectName);
        newSubject.setCredit(3);
        newSubject.setType(1); // 예시 타입, 실제 유효한 값으로 설정 필요
        // newSubject.setColor("#FF0000"); // 필요시 설정

        Response<Subject> createResponse = apiService.createSubject(authToken, newSubject).execute();
        String createError = createResponse.errorBody() != null ? createResponse.errorBody().string()
                : "Create subject error body is null";
        assertTrue("Failed to create subject: " + createError + " (Code: " + createResponse.code() + ")",
                createResponse.isSuccessful());
        assertNotNull("Created subject response body should not be null", createResponse.body());
        String createdSubjectId = createResponse.body().getId();
        assertNotNull("Created subject ID should not be null", createdSubjectId);
        assertEquals("Subject name mismatch after creation", subjectName, createResponse.body().getName());
        System.out
                .println("Created Subject: " + createResponse.body().getId() + " - " + createResponse.body().getName());

        // 2. Get Subject
        Response<Subject> getResponse = apiService.getSubject(authToken, createdSubjectId).execute();
        String getError = getResponse.errorBody() != null ? getResponse.errorBody().string()
                : "Get subject error body is null";
        assertTrue("Failed to get subject: " + getError + " (Code: " + getResponse.code() + ")",
                getResponse.isSuccessful());
        assertNotNull("Fetched subject should not be null", getResponse.body());
        assertEquals("Fetched subject ID mismatch", createdSubjectId, getResponse.body().getId());
        System.out.println("Fetched Subject: " + getResponse.body().getId() + " - " + getResponse.body().getName());

        // 3. Get All Subjects
        Response<SubjectsApiResponse> getAllResponse = apiService.getSubjects(authToken).execute();
        String getAllError = getAllResponse.errorBody() != null ? getAllResponse.errorBody().string()
                : "Get all subjects error body is null";
        assertTrue("Failed to get all subjects: " + getAllError + " (Code: " + getAllResponse.code() + ")",
                getAllResponse.isSuccessful());
        assertNotNull("Subjects API response body should not be null", getAllResponse.body());
        assertNotNull("Subjects list from API response should not be null", getAllResponse.body().getSubjects());
        List<Subject> subjectsList = getAllResponse.body().getSubjects();
        assertTrue("Subjects list should contain the created subject",
                subjectsList.stream().anyMatch(s -> createdSubjectId.equals(s.getId())));
        System.out
                .println("Found " + subjectsList.size() + " subjects. Message: " + getAllResponse.body().getMessage());

        // 4. Update Subject
        Subject subjectToUpdate = getResponse.body();
        String updatedSubjectName = "Updated Subject Workflow " + System.currentTimeMillis();
        subjectToUpdate.setName(updatedSubjectName);
        subjectToUpdate.setCredit(4);

        Response<Subject> updateResponse = apiService.updateSubject(authToken, createdSubjectId, subjectToUpdate)
                .execute();
        String updateError = updateResponse.errorBody() != null ? updateResponse.errorBody().string()
                : "Update subject error body is null";
        assertTrue("Failed to update subject: " + updateError + " (Code: " + updateResponse.code() + ")",
                updateResponse.isSuccessful());
        assertNotNull("Updated subject response body should not be null", updateResponse.body());
        assertEquals("Subject name mismatch after update", updatedSubjectName, updateResponse.body().getName());
        assertEquals("Subject credit mismatch after update", 4, updateResponse.body().getCredit());
        System.out
                .println("Updated Subject: " + updateResponse.body().getId() + " - " + updateResponse.body().getName());

        // 5. Delete Subject
        Response<Void> deleteResponse = apiService.deleteSubject(authToken, createdSubjectId).execute();
        String deleteError = deleteResponse.errorBody() != null ? deleteResponse.errorBody().string()
                : "Delete subject error body is null";
        assertTrue("Failed to delete subject: " + deleteError + " (Code: " + deleteResponse.code() + ")",
                deleteResponse.isSuccessful());
        System.out.println("Subject " + createdSubjectId + " deleted successfully.");

        Response<Subject> getAfterDeleteResponse = apiService.getSubject(authToken, createdSubjectId).execute();
        assertFalse("Subject should have been deleted, but GET was successful.", getAfterDeleteResponse.isSuccessful());
        assertEquals("Expected 404 Not Found after deleting subject", 404, getAfterDeleteResponse.code());
    }

    @Test
    public void testStudySessionWorkflow() throws IOException {
        assertNotNull("Auth token is null for study session workflow.", authToken);

        // A. Create a temporary subject for session tests
        Subject tempSubject = new Subject();
        String tempSubjectName = "Session Test Subject " + System.currentTimeMillis();
        tempSubject.setName(tempSubjectName);
        tempSubject.setCredit(1);
        tempSubject.setType(2); // 예시 타입
        Response<Subject> subjectCreateResponse = apiService.createSubject(authToken, tempSubject).execute();
        String sCreateError = subjectCreateResponse.errorBody() != null ? subjectCreateResponse.errorBody().string()
                : "Temp subject create error body is null";
        assertTrue("Failed to create temp subject for session test: " + sCreateError + " (Code: "
                + subjectCreateResponse.code() + ")", subjectCreateResponse.isSuccessful());
        assertNotNull("Temp subject response body is null", subjectCreateResponse.body());
        String tempSubjectIdForSession = subjectCreateResponse.body().getId();
        assertNotNull("Temp subject ID is null", tempSubjectIdForSession);
        System.out.println("Created Temp Subject for Session Test: ID " + tempSubjectIdForSession);

        // 1. Create Study Session
        StudySession newSession = new StudySession();
        newSession.setSubject_id(tempSubjectIdForSession);

        OffsetDateTime now = OffsetDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        newSession.setDate(now.format(dateFormatter)); // "YYYY-MM-DD"
        newSession.setStart_time(now.minusHours(2).format(dateTimeFormatter)); // "YYYY-MM-DDTHH:MM:SSZ"
        newSession.setEnd_time(now.minusHours(1).format(dateTimeFormatter)); // "YYYY-MM-DDTHH:MM:SSZ"
        newSession.setStudy_time(60);
        // newSession.setRest_time(10);

        Response<StudySession> createResponse = apiService.createStudySession(authToken, newSession).execute();
        String createError = createResponse.errorBody() != null ? createResponse.errorBody().string()
                : "Create session error body is null";
        assertTrue("Failed to create study session: " + createError + " (Code: " + createResponse.code() + ")",
                createResponse.isSuccessful());
        assertNotNull("Created study session response body should not be null", createResponse.body());
        String createdSessionId = createResponse.body().getId();
        assertNotNull("Created session ID should not be null", createdSessionId);
        assertEquals("Session subject ID mismatch", tempSubjectIdForSession, createResponse.body().getSubject_id());
        System.out.println("Created Study Session: " + createResponse.body().getId());

        // 2. Get Study Session
        Response<StudySession> getResponse = apiService.getStudySession(authToken, createdSessionId).execute();
        String getError = getResponse.errorBody() != null ? getResponse.errorBody().string()
                : "Get session error body is null";
        assertTrue("Failed to get study session: " + getError + " (Code: " + getResponse.code() + ")",
                getResponse.isSuccessful());
        assertNotNull("Fetched study session should not be null", getResponse.body());
        assertEquals("Fetched session ID mismatch", createdSessionId, getResponse.body().getId());
        System.out.println("Fetched Study Session: " + getResponse.body().getId());

        // 3. Get All Study Sessions for the subject
        Response<StudySessionsApiResponse> getAllResponse = apiService
                .getStudySessions(authToken, tempSubjectIdForSession).execute();
        String getAllError = getAllResponse.errorBody() != null ? getAllResponse.errorBody().string()
                : "Get all sessions error body is null";
        assertTrue("Failed to get all study sessions: " + getAllError + " (Code: " + getAllResponse.code() + ")",
                getAllResponse.isSuccessful());
        assertNotNull("Study sessions API response body should not be null", getAllResponse.body());
        assertNotNull("Sessions list from API response should not be null", getAllResponse.body().getSessions());
        List<StudySession> sessionsList = getAllResponse.body().getSessions();
        assertTrue("Session list should contain the created session for subject " + tempSubjectIdForSession,
                sessionsList.stream().anyMatch(s -> createdSessionId.equals(s.getId())));
        System.out.println("Found " + sessionsList.size() + " sessions for subject " + tempSubjectIdForSession
                + ". Message: " + getAllResponse.body().getMessage());

        // 4. Update Study Session
        StudySession sessionToUpdate = getResponse.body();
        sessionToUpdate.setEnd_time(now.plusMinutes(30).format(dateTimeFormatter));
        sessionToUpdate.setStudy_time(90);
        // sessionToUpdate.setRest_time(15); // 휴식 시간 변경 또는 추가

        Response<StudySession> updateResponse = apiService
                .updateStudySession(authToken, createdSessionId, sessionToUpdate).execute();
        String updateError = updateResponse.errorBody() != null ? updateResponse.errorBody().string()
                : "Update session error body is null";
        assertTrue("Failed to update study session: " + updateError + " (Code: " + updateResponse.code() + ")",
                updateResponse.isSuccessful());
        assertNotNull("Updated study session response body should not be null", updateResponse.body());
        // assertEquals("Updated session notes.", updateResponse.body().getNotes()); //
        // 'notes' 필드 없음
        System.out.println("Updated Study Session: " + updateResponse.body().getId());

        // 5. Delete Study Session
        Response<Void> deleteResponse = apiService.deleteStudySession(authToken, createdSessionId).execute();
        String deleteError = deleteResponse.errorBody() != null ? deleteResponse.errorBody().string()
                : "Delete session error body is null";
        assertTrue("Failed to delete study session: " + deleteError + " (Code: " + deleteResponse.code() + ")",
                deleteResponse.isSuccessful());
        System.out.println("Study session " + createdSessionId + " deleted successfully.");

        Response<StudySession> getAfterDeleteResponse = apiService.getStudySession(authToken, createdSessionId)
                .execute();
        assertFalse("Session should have been deleted, but GET was successful.", getAfterDeleteResponse.isSuccessful());
        assertEquals("Expected 404 Not Found after deleting session", 404, getAfterDeleteResponse.code());

        // B. Clean up the temporary subject
        Response<Void> deleteTempSubjectResponse = apiService.deleteSubject(authToken, tempSubjectIdForSession)
                .execute();
        String sDeleteError = deleteTempSubjectResponse.errorBody() != null
                ? deleteTempSubjectResponse.errorBody().string()
                : "Temp subject delete error body is null";
        assertTrue(
                "Failed to delete temp subject: " + sDeleteError + " (Code: " + deleteTempSubjectResponse.code() + ")",
                deleteTempSubjectResponse.isSuccessful());
        System.out.println("Temp Subject ID " + tempSubjectIdForSession + " for session test deleted successfully.");
    }

    @After
    public void tearDown() throws IOException {
        if (authToken != null) {
            System.out.println("Attempting to delete temporary user in tearDown...");
            Response<Void> deleteTempUserResponse = apiService.deleteTempUser(authToken).execute();
            if (!deleteTempUserResponse.isSuccessful()) {
                String errorBody = deleteTempUserResponse.errorBody() != null
                        ? deleteTempUserResponse.errorBody().string()
                        : "Error body is null or already closed";
                // 서버에서 이미 삭제되었거나 (404), 다른 이유로 실패할 수 있음
                System.err.println("Failed to delete temp user in tearDown: " + errorBody + " (Code: "
                        + deleteTempUserResponse.code() + ")");
            } else {
                System.out.println("Temp user deleted successfully in tearDown.");
            }
            authToken = null; // Prevent reuse
        }
    }
}