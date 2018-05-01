package fyodor.service;

import fyodor.dto.ArticleDto;
import fyodor.events.ArticleCreatedEvent;
import fyodor.model.Article;
import fyodor.model.Category;
import fyodor.model.Image;
import fyodor.model.User;
import fyodor.repository.ArticleRepository;
import fyodor.util.UsedCategoriesHierarchyBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Service
public class ArticleService implements IArticleService {
    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IImageService imageService;

    private ApplicationEventPublisher publisher;

    @Autowired
    private UsedCategoriesHierarchyBuilder usedCategoriesHierarchyBuilder;

    @Override
    public Article save(String json, Principal author) {
        JsonParser springParser = JsonParserFactory.getJsonParser();
        Map<String, Object> data = springParser.parseMap(json);

        Article article = new Article();
        User user = userService.findByUsernameIgnoreCase(author.getName());
        article.setTitle((String)data.get("title"));
        article.setContent((String)data.get("content"));
        article.setCategory(categoryService.findById(Long.valueOf((String)data.get("categoryId"))));
        article.setAuthor(user);

        String imageCode = (String)data.get("picture");
        Image image = imageService.save(imageCode);
        article.setImage(image);

        return articleRepository.save(article);
    }

    @Override
    public Article save(ArticleDto articleDto, Principal author) {
        Article article = new Article();
        User user = userService.findByUsernameIgnoreCase(author.getName());
        article.setTitle(articleDto.getTitle());
        article.setContent(articleDto.getContent());
        article.setCategory(categoryService.findById(articleDto.getCategoryId()));
        article.setAuthor(user);
        String imageCode = articleDto.getImageData();
        Image image = imageService.save(imageCode);
        article.setImage(image);

        Article savedArticle = articleRepository.save(article);

        this.publisher.publishEvent(new ArticleCreatedEvent(savedArticle));

        return savedArticle;
    }

    @Override
    public Article save(Article article) {
        return articleRepository.save(article);
    }

    @Override
    public Article findById(Long id) {
        return articleRepository.findById(id);
    }

    @Override
    public Article findByTitle(String title) {
        return articleRepository.findByTitle(title);
    }

    @Override
    public List<Article> findByCategoryId(Long id) {
        return articleRepository.findByCategoryId(id);
    }

    @Override
    public List<Article> findByCategoryIdHierarchically(Long id) {
        List<Category> categories = categoryService.findCategoriesAndSubcategoriesById(id);
        return articleRepository.findArticlesByCategoryIn(categories);
    }

    @Override
    public List<Article> findByCategoryAndAuthor(Category category, User author) {
        return articleRepository.findByCategoryAndAuthor(category, author);
    }

    @Override
    public List<Article> findByAuthor(User author) {
        return articleRepository.findByAuthor(author);
    }

    @Override
    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    @Override
    public List<Article> findAllWithOrder(Long orderId) {
        if (orderId == 1)
            return articleRepository.findAllMostPopular();
        if (orderId == 2)
            return articleRepository.findAllLastAdded();
        if (orderId == 3)
            return articleRepository.findAllFirstAdded();
        throw new RuntimeException("Unexpected order index");
    }

    @Override
    public Article findByTitleIgnoreCase(String title) {
        return articleRepository.findByTitleIgnoreCase(title);
    }
}
