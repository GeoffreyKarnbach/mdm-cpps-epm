package project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.backend.entity.ExampleEntity;
import project.backend.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
