package students.batch.security;

import auth.entity.Role;
import auth.repository.BatchRepository;
import auth.repository.UserRepository;
import auth.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("batchSecurityService")
@RequiredArgsConstructor
public class BatchSecurityService {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public boolean isAdmin(Integer batchId, String email) {
        return batchRepository.findById(batchId)
                .flatMap(batch -> userRepository.findByEmail(email)
                        .map(user -> userRoleRepository.existsByUserIdAndInstituteIdAndRole(
                                user.getId(), batch.getInstitute().getId(), Role.INSTITUTE_ADMIN)))
                .orElse(false);
    }
}
