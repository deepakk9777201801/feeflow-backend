package auth.repository;

import auth.entity.Role;
import auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUserId(Integer userId);

    boolean existsByUserIdAndInstituteId(Integer userId, Integer instituteId);

    boolean existsByUserIdAndInstituteIdAndRole(Integer userId, Integer instituteId, Role role);
}
