package fyodor.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class UserAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, columnDefinition = "boolean default 1")
    private boolean enabled = true;
}
