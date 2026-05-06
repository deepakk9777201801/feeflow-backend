package students.student.security;

import auth.entity.Role;
import auth.repository.StudentRepository;
import auth.repository.UserRepository;
import auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("studentSecurityService")
@RequiredArgsConstructor
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public boolean isMember(Integer studentId, String email) {
        return studentRepository.findById(studentId)
                .flatMap(student -> userRepository.findByEmail(email)
                        .map(user -> userRoleRepository.existsByUserIdAndInstituteId(
                                user.getId(), student.getInstitute().getId())))
                .orElse(false);
    }

    public boolean isAdmin(Integer studentId, String email) {
        return studentRepository.findById(studentId)
                .flatMap(student -> userRepository.findByEmail(email)
                        .map(user -> userRoleRepository.existsByUserIdAndInstituteIdAndRole(
                                user.getId(), student.getInstitute().getId(), Role.INSTITUTE_ADMIN)))
                .orElse(false);
    }
}
