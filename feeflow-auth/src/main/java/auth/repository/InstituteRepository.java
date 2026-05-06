package auth.repository;

import auth.entity.Institute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstituteRepository extends JpaRepository<Institute, Integer> {

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT ur.institute FROM UserRole ur WHERE ur.user.id = :userId AND ur.institute IS NOT NULL")
    List<Institute> findDistinctInstitutesByUserId(@Param("userId") Integer userId);
}
