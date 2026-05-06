package students.institute.security;

import auth.entity.Role;
import auth.repository.UserRepository;
import auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("instituteSecurityService")
@RequiredArgsConstructor
public class InstituteSecurityService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public boolean isMember(Integer instituteId, String email) {
        return userRepository.findByEmail(email)
                .map(user -> userRoleRepository.existsByUserIdAndInstituteId(user.getId(), instituteId))
                .orElse(false);
    }

    public boolean isAdmin(Integer instituteId, String email) {
        return userRepository.findByEmail(email)
                .map(user -> userRoleRepository.existsByUserIdAndInstituteIdAndRole(
                        user.getId(), instituteId, Role.INSTITUTE_ADMIN))
                .orElse(false);
    }
}
