package project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gitlab_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabProject {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gitlab_project")
    @SequenceGenerator(name = "seq_gitlab_project", allocationSize = 5)
    @EqualsAndHashCode.Exclude
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "gitlab_id")
    private Long gitlabId;

    @OneToOne
    @JoinColumn(name = "project_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Project project;
}
