package project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gitlab_template")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabPrIssueTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gitlab_template")
    @SequenceGenerator(name = "seq_gitlab_template", allocationSize = 5)
    @EqualsAndHashCode.Exclude
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "is_pr_template")
    private Boolean isPrTemplate;

    @Column(name = "name")
    private String name;

    @Column(name = "content", length = 10000)
    private String content;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Project project;
}
