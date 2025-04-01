package project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;
import project.backend.entity.ExampleEntity;
import project.backend.entity.GitlabSubgroup;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface GitlabSubgroupRepository extends JpaRepository<GitlabSubgroup, Long> {

    @Query("SELECT g FROM GitlabSubgroup g WHERE g.workspace.id = :workspaceId AND g.isReviewerGroup = false")
    Optional<GitlabSubgroup> findNonReviewerSubgroupByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT g FROM GitlabSubgroup g WHERE g.workspace.id = :workspaceId AND g.isReviewerGroup = true")
    Optional<GitlabSubgroup> findReviewerSubgroupByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT g FROM GitlabSubgroup g WHERE g.workspace.id = :workspaceId")
    List<GitlabSubgroup> findGitlabSubgroupByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
