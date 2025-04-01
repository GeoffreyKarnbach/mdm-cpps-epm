package project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.backend.entity.ExampleEntity;
import project.backend.entity.GitlabProject;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitlabProjectRepository extends JpaRepository<GitlabProject, Long> {

    @Query("SELECT p FROM GitlabProject p WHERE p.project.id = :projectId")
    List<GitlabProject> findByProjectId(@Param("projectId") Long projectId);
}
