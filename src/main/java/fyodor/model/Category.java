package fyodor.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "category")
@Data
@EqualsAndHashCode(exclude={"articles", "parentCategory", "subcategories"})
@ToString(exclude={"articles", "parentCategory", "subcategories"})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "category")
    private Set<Article> articles;

    @ManyToOne
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Category parentCategory;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "parentCategory")
    private Set<Category> subcategories;
}
