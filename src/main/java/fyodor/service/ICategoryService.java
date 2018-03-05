package fyodor.service;

import fyodor.model.Article;
import fyodor.model.Category;

import java.util.List;

public interface ICategoryService {
    List<Category> findAll();

    Category findByName(String name);

    Category findById(Long id);

    List<Category> findByArticlesIn(List<Article> article);
}
