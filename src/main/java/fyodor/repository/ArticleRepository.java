package fyodor.repository;

import fyodor.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long>{
    Article findByTitle(String title);
}
