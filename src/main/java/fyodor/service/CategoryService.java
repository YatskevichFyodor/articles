package fyodor.service;

import fyodor.model.Article;
import fyodor.model.Category;
import fyodor.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService implements ICategoryService{
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Category> findUsedCategories() {
        return categoryRepository.findUsedCategories();
    }

    @Override
    public Category findByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public List<Category> findByArticlesIn(List<Article> articles) {
        return categoryRepository.findByArticlesIn(articles);
    }
}
