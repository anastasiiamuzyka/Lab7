package pk.pn.pasir_parkovskyi_nikita.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_groups") // 'groups' is a reserved keyword in SQL, use safe name
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Group name

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner; // Group owner (can invite and remove other users)

    private LocalDateTime createdAt; // Date and time the group was created

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships; // List of group memberships (users in the group)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Membership> getMemberships() { return memberships; }
    public void setMemberships(List<Membership> memberships) { this.memberships = memberships; }

    @Transient
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
        // ownerId is computed from owner.getId() for GraphQL/Java,
        // but is NOT a separate column in the database
    }
}
