package com.scms.core.student;

import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.dto.ChangeStudentPasswordRequest;
import com.scms.core.student.dto.StudentInfoResponse;
import com.scms.core.student.dto.UpdateStudentInfoRequest;
import com.scms.core.student.repository.StudentInfoRepository;
import com.scms.core.student.service.StudentAvatarStorageService;
import com.scms.core.student.service.StudentProfileCompletionService;
import com.scms.core.student.service.StudentService;
import com.scms.core.user.domain.UserEntity;
import com.scms.core.user.domain.UserRole;
import com.scms.core.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentServiceTest {

    private StudentInfoRepository studentInfoRepository;
    private UserRepository userRepository;
    private CurrentUserProvider currentUserProvider;
    private PasswordEncoder passwordEncoder;
    private StudentAvatarStorageService studentAvatarStorageService;
    private StudentProfileCompletionService studentProfileCompletionService;
    private StudentService studentService;
    private UserEntity studentUser;

    @BeforeEach
    void setUp() {
        studentInfoRepository = mock(StudentInfoRepository.class);
        userRepository = mock(UserRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        studentAvatarStorageService = mock(StudentAvatarStorageService.class);
        studentProfileCompletionService = new StudentProfileCompletionService(studentInfoRepository);
        studentService = new StudentService(
                studentInfoRepository,
                userRepository,
                currentUserProvider,
                passwordEncoder,
                studentAvatarStorageService,
                studentProfileCompletionService
        );

        studentUser = new UserEntity();
        studentUser.setUsername("student");
        studentUser.setEmail("student@scms.local");
        studentUser.setPasswordHash("encoded-old");
        studentUser.setRole(UserRole.STUDENT);
        studentUser.setEnabled(true);

        setEntityId(studentUser, 9L);
        when(currentUserProvider.getRequiredUser()).thenReturn(new AuthenticatedUser(9L, "student", UserRole.STUDENT));
        when(userRepository.findById(9L)).thenReturn(Optional.of(studentUser));
    }

    @Test
    void getCurrentInfoShouldFallbackToUserWhenStudentInfoMissing() {
        when(studentInfoRepository.findByUserId(9L)).thenReturn(Optional.empty());

        StudentInfoResponse response = studentService.getCurrentInfo();

        assertEquals("student", response.displayName());
        assertEquals("", response.studentNo());
        assertEquals("", response.grade());
        assertEquals("", response.className());
        assertEquals("", response.phone());
        assertEquals("", response.bio());
        assertEquals(false, response.profileCompleted());
        assertEquals(java.util.List.of("displayName", "studentNo", "grade", "className"), response.missingRequiredFields());
    }

    @Test
    void updateCurrentInfoShouldCreateStudentInfoWhenMissing() {
        when(studentInfoRepository.findByUserId(9L)).thenReturn(Optional.empty());
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentInfoResponse response = studentService.updateCurrentInfo(
                new UpdateStudentInfoRequest(
                        "Student Name",
                        "20260001",
                        "HIGH_1",
                        "3",
                        "13800000000",
                        "Student bio"
                )
        );

        assertEquals("Student Name", response.displayName());
        assertEquals("20260001", response.studentNo());
        assertEquals("HIGH_1", response.grade());
        assertEquals("3", response.className());
        assertEquals("13800000000", response.phone());
        assertEquals("Student bio", response.bio());
        assertEquals(true, response.profileCompleted());
        assertEquals(java.util.List.of(), response.missingRequiredFields());
    }

    @Test
    void getCurrentInfoShouldNormalizeLegacyGradeAndClassName() {
        StudentInfoEntity info = new StudentInfoEntity();
        info.setDisplayName("Legacy Student");
        info.setGrade("高二");
        info.setClassName("5班");
        when(studentInfoRepository.findByUserId(9L)).thenReturn(Optional.of(info));

        StudentInfoResponse response = studentService.getCurrentInfo();

        assertEquals("Legacy Student", response.displayName());
        assertEquals("HIGH_2", response.grade());
        assertEquals("5", response.className());
    }

    @Test
    void updateCurrentInfoShouldNormalizeLegacyCompatibleValuesInsideService() {
        when(studentInfoRepository.findByUserId(9L)).thenReturn(Optional.empty());
        when(studentInfoRepository.save(any(StudentInfoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentInfoResponse response = studentService.updateCurrentInfo(
                new UpdateStudentInfoRequest(
                        "Student Name",
                        "20260001",
                        "高三",
                        "12班",
                        "13800000000",
                        "Student bio"
                )
        );

        assertEquals("HIGH_3", response.grade());
        assertEquals("12", response.className());
    }

    @Test
    void getCurrentInfoShouldClearOutOfRangeClassName() {
        StudentInfoEntity info = new StudentInfoEntity();
        info.setDisplayName("Legacy Student");
        info.setGrade("HIGH_1");
        info.setClassName("31");
        when(studentInfoRepository.findByUserId(9L)).thenReturn(Optional.of(info));

        StudentInfoResponse response = studentService.getCurrentInfo();

        assertEquals("HIGH_1", response.grade());
        assertEquals("", response.className());
    }

    @Test
    void changeCurrentPasswordShouldRejectIncorrectCurrentPassword() {
        when(passwordEncoder.matches("wrong-old", "encoded-old")).thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> studentService.changeCurrentPassword(
                        new ChangeStudentPasswordRequest("wrong-old", "new-password", "new-password")
                )
        );
    }

    @Test
    void changeCurrentPasswordShouldUpdateHashWhenRequestIsValid() {
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studentService.changeCurrentPassword(
                new ChangeStudentPasswordRequest("old-password", "new-password", "new-password")
        );

        assertEquals("encoded-new", studentUser.getPasswordHash());
    }

    private void setEntityId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
