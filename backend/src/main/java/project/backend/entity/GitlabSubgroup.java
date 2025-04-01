package project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gitlab_subgroup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitlabSubgroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_gitlab_subgroup")
    @SequenceGenerator(name = "seq_gitlab_subgroup", allocationSize = 5)
    @EqualsAndHashCode.Exclude
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "gitlab_id")
    private Long gitlabId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workspace_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private WorkSpace workspace;

    @Column(name = "is_reviewer_group")
    private Boolean isReviewerGroup;


}
