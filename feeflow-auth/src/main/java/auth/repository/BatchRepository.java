package auth.repository;

import auth.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Integer> {

    @Query("SELECT b FROM Batch b JOIN FETCH b.institute WHERE b.institute.id = :instituteId AND b.status <> :status")
    List<Batch> findActiveByInstituteId(@Param("instituteId") Integer instituteId, @Param("status") String status);

    boolean existsByNameAndInstituteId(String name, Integer instituteId);
}
