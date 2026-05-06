package students.institute.service.impl;

import auth.entity.Institute;
import auth.entity.Role;
import auth.entity.User;
import auth.entity.UserRole;
import auth.repository.InstituteRepository;
import auth.repository.UserRepository;
import auth.repository.UserRoleRepository;
import common.dto.institute.CreateInstituteRequest;
import common.dto.institute.InstituteResponse;
import common.dto.institute.UpdateInstituteRequest;
import common.exception.BadRequestException;
import common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstituteServiceImpl Unit Tests")
class InstituteServiceImplTest {

    @Mock
    private InstituteRepository instituteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private InstituteServiceImpl instituteService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private static final String TEST_EMAIL = "admin@test.com";
    private static final String INSTITUTE_EMAIL = "institute@test.com";
    private static final Integer INSTITUTE_ID = 1;
    private static final Integer USER_ID = 10;

    private User testUser;
    private Institute testInstitute;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .name("Admin User")
                .email(TEST_EMAIL)
                .phone("9876543210")
                .status("ACTIVE")
                .build();

        testInstitute = Institute.builder()
                .id(INSTITUTE_ID)
                .name("Test Institute")
                .city("Bangalore")
                .state("Karnataka")
                .phone("9876543210")
                .email(INSTITUTE_EMAIL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void mockSecurityContext() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        SecurityContextHolder.setContext(securityContext);
    }

    private CreateInstituteRequest buildCreateRequest() {
        return CreateInstituteRequest.builder()
                .name("Test Institute")
                .city("Bangalore")
                .state("Karnataka")
                .phone("9876543210")
                .email(INSTITUTE_EMAIL)
                .build();
    }

    // ─── createInstitute ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createInstitute - success: creates institute and assigns INSTITUTE_ADMIN role")
    void createInstitute_success() {
        mockSecurityContext();
        when(instituteRepository.existsByEmail(INSTITUTE_EMAIL)).thenReturn(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(instituteRepository.save(any(Institute.class))).thenReturn(testInstitute);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(UserRole.builder().build());

        InstituteResponse response = instituteService.createInstitute(buildCreateRequest());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(INSTITUTE_ID);
        assertThat(response.getName()).isEqualTo("Test Institute");
        assertThat(response.getCity()).isEqualTo("Bangalore");
        assertThat(response.getState()).isEqualTo("Karnataka");
        assertThat(response.getPhone()).isEqualTo("9876543210");
        assertThat(response.getEmail()).isEqualTo(INSTITUTE_EMAIL);

        ArgumentCaptor<Institute> instituteCaptor = ArgumentCaptor.forClass(Institute.class);
        verify(instituteRepository).save(instituteCaptor.capture());
        assertThat(instituteCaptor.getValue().getName()).isEqualTo("Test Institute");

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole()).isEqualTo(Role.INSTITUTE_ADMIN);
        assertThat(roleCaptor.getValue().getUser()).isEqualTo(testUser);
        assertThat(roleCaptor.getValue().getInstitute()).isEqualTo(testInstitute);
    }

    @Test
    @DisplayName("createInstitute - throws BadRequestException when email already exists")
    void createInstitute_duplicateEmail_throwsException() {
        mockSecurityContext();
        when(instituteRepository.existsByEmail(INSTITUTE_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> instituteService.createInstitute(buildCreateRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(INSTITUTE_EMAIL);

        verify(userRepository, never()).findByEmail(any());
        verify(instituteRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createInstitute - throws ResourceNotFoundException when user not found")
    void createInstitute_userNotFound_throwsException() {
        mockSecurityContext();
        when(instituteRepository.existsByEmail(INSTITUTE_EMAIL)).thenReturn(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instituteService.createInstitute(buildCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(TEST_EMAIL);

        verify(instituteRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    // ─── getMyInstitutes ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyInstitutes - success: returns all distinct institutes via JOIN FETCH query")
    void getMyInstitutes_success() {
        mockSecurityContext();
        Institute anotherInstitute = Institute.builder()
                .id(2).name("Another Institute").city("Mumbai").state("Maharashtra")
                .phone("9123456789").email("another@test.com")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(instituteRepository.findDistinctInstitutesByUserId(USER_ID))
                .thenReturn(List.of(testInstitute, anotherInstitute));

        List<InstituteResponse> responses = instituteService.getMyInstitutes();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(InstituteResponse::getName)
                .containsExactlyInAnyOrder("Test Institute", "Another Institute");
        verify(userRoleRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("getMyInstitutes - returns empty list when user has no institute associations")
    void getMyInstitutes_emptyResult_returnsEmptyList() {
        mockSecurityContext();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(instituteRepository.findDistinctInstitutesByUserId(USER_ID)).thenReturn(List.of());

        List<InstituteResponse> responses = instituteService.getMyInstitutes();

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("getMyInstitutes - throws ResourceNotFoundException when user not found")
    void getMyInstitutes_userNotFound_throwsException() {
        mockSecurityContext();
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instituteService.getMyInstitutes())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(TEST_EMAIL);

        verify(instituteRepository, never()).findDistinctInstitutesByUserId(any());
    }

    // ─── getInstituteById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getInstituteById - success: returns all mapped fields")
    void getInstituteById_success() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));

        InstituteResponse response = instituteService.getInstituteById(INSTITUTE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(INSTITUTE_ID);
        assertThat(response.getName()).isEqualTo("Test Institute");
        assertThat(response.getCity()).isEqualTo("Bangalore");
        assertThat(response.getState()).isEqualTo("Karnataka");
        assertThat(response.getPhone()).isEqualTo("9876543210");
        assertThat(response.getEmail()).isEqualTo(INSTITUTE_EMAIL);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getInstituteById - throws ResourceNotFoundException when not found")
    void getInstituteById_notFound_throwsException() {
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instituteService.getInstituteById(INSTITUTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));
    }

    // ─── updateInstitute ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateInstitute - success: updates all provided fields")
    void updateInstitute_success_allFields() {
        UpdateInstituteRequest request = UpdateInstituteRequest.builder()
                .name("Updated Name").city("Chennai")
                .state("Tamil Nadu").phone("9000000000").email("updated@test.com")
                .build();

        Institute updatedInstitute = Institute.builder()
                .id(INSTITUTE_ID).name("Updated Name").city("Chennai")
                .state("Tamil Nadu").phone("9000000000").email("updated@test.com")
                .createdAt(testInstitute.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(instituteRepository.save(any(Institute.class))).thenReturn(updatedInstitute);

        InstituteResponse response = instituteService.updateInstitute(INSTITUTE_ID, request);

        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getCity()).isEqualTo("Chennai");
        assertThat(response.getState()).isEqualTo("Tamil Nadu");
        assertThat(response.getPhone()).isEqualTo("9000000000");
        assertThat(response.getEmail()).isEqualTo("updated@test.com");
        verify(instituteRepository).save(testInstitute);
    }

    @Test
    @DisplayName("updateInstitute - partial update: only name provided, other fields untouched")
    void updateInstitute_partialUpdate_onlyNameChanged() {
        UpdateInstituteRequest request = UpdateInstituteRequest.builder().name("Only Name Updated").build();
        Institute afterSave = Institute.builder()
                .id(INSTITUTE_ID).name("Only Name Updated").city("Bangalore")
                .state("Karnataka").phone("9876543210").email(INSTITUTE_EMAIL)
                .createdAt(testInstitute.getCreatedAt()).updatedAt(LocalDateTime.now())
                .build();

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(instituteRepository.save(any(Institute.class))).thenReturn(afterSave);

        InstituteResponse response = instituteService.updateInstitute(INSTITUTE_ID, request);

        assertThat(response.getName()).isEqualTo("Only Name Updated");
        assertThat(response.getCity()).isEqualTo("Bangalore");
        assertThat(response.getState()).isEqualTo("Karnataka");
    }

    @Test
    @DisplayName("updateInstitute - throws ResourceNotFoundException when institute not found")
    void updateInstitute_notFound_throwsException() {
        UpdateInstituteRequest request = UpdateInstituteRequest.builder().name("Updated").build();
        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> instituteService.updateInstitute(INSTITUTE_ID, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(INSTITUTE_ID));

        verify(instituteRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateInstitute - no-op when all request fields are null")
    void updateInstitute_allNullFields_noChanges() {
        UpdateInstituteRequest request = new UpdateInstituteRequest();

        when(instituteRepository.findById(INSTITUTE_ID)).thenReturn(Optional.of(testInstitute));
        when(instituteRepository.save(any(Institute.class))).thenReturn(testInstitute);

        InstituteResponse response = instituteService.updateInstitute(INSTITUTE_ID, request);

        assertThat(response.getName()).isEqualTo("Test Institute");
        assertThat(response.getCity()).isEqualTo("Bangalore");
        verify(instituteRepository).save(testInstitute);
    }
}
