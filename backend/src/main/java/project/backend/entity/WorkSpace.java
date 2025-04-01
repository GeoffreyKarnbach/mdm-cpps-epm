package project.backend.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "workspace")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_workspace")
    @SequenceGenerator(name = "seq_workspace", allocationSize = 5)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_common", nullable = false)
    private Boolean isCommon;

    @ManyToMany(fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JoinTable(name = "user_workspace",
        joinColumns = @JoinColumn(name = "workspace_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> users;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    @ToString.Exclude
    private Project project;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    private Set<GitlabSubgroup> gitlabSubgroups;
}
