package project.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "project_user_access")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_project_user_access")
    @SequenceGenerator(name = "seq_project_user_access", allocationSize = 5)
    @EqualsAndHashCode.Include
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User user;

    @Column(name = "is_reviewer", nullable = false)
    private Boolean isReviewer;

    @Column(name = "is_admin", nullable = false)
    private Boolean isAdmin;
}
