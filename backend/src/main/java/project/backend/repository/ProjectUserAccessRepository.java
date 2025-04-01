package project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.backend.entity.ExampleEntity;
import project.backend.entity.ProjectUserAccess;

import java.util.List;

@Repository
public interface ProjectUserAccessRepository extends JpaRepository<ProjectUserAccess, Long> {

    @Query("SELECT pua FROM ProjectUserAccess pua WHERE pua.user.id = :userId")
    List<ProjectUserAccess> findByUserId(@Param("userId") Long userId);

    @Query("SELECT pua FROM ProjectUserAccess pua WHERE pua.project.id = :projectId AND pua.user.id = :userId")
    ProjectUserAccess findByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT COUNT(pua) > 0 FROM ProjectUserAccess pua WHERE pua.user.id = :userId AND pua.project.id = :projectId")
    boolean hasAccessToProject(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Query("SELECT COUNT(pua) > 0 FROM ProjectUserAccess pua WHERE pua.user.id = :userId AND pua.project.id = :projectId AND pua.isAdmin = true")
    boolean hasAdminAccessToProject(@Param("userId") Long userId, @Param("projectId") Long projectId);
}
