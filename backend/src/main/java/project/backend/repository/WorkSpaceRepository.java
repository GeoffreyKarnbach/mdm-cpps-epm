package project.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.backend.entity.ExampleEntity;
import project.backend.entity.User;
import project.backend.entity.WorkSpace;

import java.util.List;

@Repository
public interface WorkSpaceRepository extends JpaRepository<WorkSpace, Long> {

    @Query("SELECT w.users FROM WorkSpace w WHERE w.id = :workSpaceId")
    List<User> findByWorkSpaceId(@Param("workSpaceId") long workSpaceId);
}
