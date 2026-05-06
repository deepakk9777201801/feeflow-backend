package auth.repository;

import auth.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    @Query("SELECT s FROM Student s JOIN FETCH s.institute JOIN FETCH s.batch WHERE s.institute.id = :instituteId")
    List<Student> findByInstituteId(@Param("instituteId") Integer instituteId);

    @Query("SELECT s FROM Student s JOIN FETCH s.institute JOIN FETCH s.batch WHERE s.institute.id = :instituteId AND s.batch.id = :batchId")
    List<Student> findByInstituteIdAndBatchId(@Param("instituteId") Integer instituteId, @Param("batchId") Integer batchId);

    boolean existsByPrimaryPhoneAndInstituteId(String primaryPhone, Integer instituteId);

    @Query("SELECT s.primaryPhone FROM Student s WHERE s.institute.id = :instituteId")
    Set<String> findPrimaryPhonesByInstituteId(@Param("instituteId") Integer instituteId);
}
