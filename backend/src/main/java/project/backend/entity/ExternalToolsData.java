package project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_tools_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalToolsData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_external_tools_data")
    @SequenceGenerator(name = "seq_external_tools_data", allocationSize = 5)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "root_group_id")
    private Long rootGroupId;

    @Column(name = "creator_user_id")
    private Long creatorUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    private Project project;
}
