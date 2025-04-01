package project.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_users")
    @SequenceGenerator(name = "seq_users", allocationSize = 5)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "gitlabID", nullable = false)
    private Long gitlabId;

    @Column(name = "gitlabUsername", nullable = false)
    private String gitlabUsername;

    @Column(name = "email")
    @EqualsAndHashCode.Exclude
    private String email;

    @Column(name = "avatarUrl", nullable = false)
    private String avatarUrl;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    private Set<ProjectUserAccess> projectUserAccesses;


    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<WorkSpace> workSpaces;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "expiration_time")
    private Integer expirationTime;
}
