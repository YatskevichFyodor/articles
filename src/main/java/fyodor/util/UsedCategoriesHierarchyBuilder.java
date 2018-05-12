package fyodor.util;


import fyodor.events.*;
import fyodor.model.Article;
import fyodor.model.Category;
import fyodor.repository.CategoryRepository;
import fyodor.service.ICategoryService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(100)
@Data
public class UsedCategoriesHierarchyBuilder {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ICategoryService categoryService;

    private final Category hierarchy = new Category();
    private Set<Category> usedCategories;
    private Set<Category> usedCategoriesAndParentsSet; // used on building stage
    private List<Category> usedCategoriesAndParentsList; // used after building stage

    @PostConstruct
    private void build() {
        usedCategories = new HashSet<>(categoryRepository.findUsedCategories());
        usedCategoriesAndParentsSet = new HashSet<>(usedCategories);
        for (Category category : usedCategories) {
            insertParentCategories(category);
        }

        Set<Category> rootCategories = new HashSet<>();
        for (Category category : usedCategoriesAndParentsSet) {
            if (category.getParentCategory() == null)
                rootCategories.add(category);
        }

        hierarchy.setSubcategories(rootCategories);

        for (Category rootCategory: hierarchy.getSubcategories()) {
            rootCategory.setParentCategory(hierarchy);
        }

        usedCategoriesAndParentsList = new ArrayList<>();
        usedCategoriesAndParentsList.addAll(rootCategories);

        for (Category category : hierarchy.getSubcategories()) {
            filterSubcategories(category);
        }
    }

    private void insertParentCategories(Category c) {
        Category parentCategory = c.getParentCategory();
        if (c.getParentCategory() != null) {
            usedCategoriesAndParentsSet.add(parentCategory);
            insertParentCategories(parentCategory);
        }
    }

    private void filterSubcategories(Category primaryCategory) {
        Set<Category> subcategories = primaryCategory.getSubcategories();
        Set<Category> notUsedSubcategories = new HashSet<>();
        for (Category subcategory : subcategories) {
            // don't replace the code below in method, because it uses usedCategoriesAndParentsSet - not List
            // so it's not usable at runtime
            boolean isAmongUsedCategoriesAndParents = false;
            for (Category usedCategoryOrParent : usedCategoriesAndParentsSet) {
                if (usedCategoryOrParent.equals(subcategory)) {
                    isAmongUsedCategoriesAndParents = true;
                    break;
                }
            }
            if (!isAmongUsedCategoriesAndParents) {
                notUsedSubcategories.add(subcategory);
            }
        }
        subcategories.removeAll(notUsedSubcategories);

        usedCategoriesAndParentsList.addAll(subcategories);

        for (Category subcategory : subcategories) {
            filterSubcategories(subcategory);
        }
    }

    public Category getSubhierarchy(Category primaryCategory) {
        for (Category subcategory : usedCategoriesAndParentsList) {
            if (subcategory.equals(primaryCategory))
                return subcategory;
        }

        throw new RuntimeException("Subhierarchy wasn't found");
    }

    private boolean categoryIsAmongUsedCategories(Category category) {
        for (Category usedCategory : usedCategories) {
            if (usedCategory.equals(category))
                return true;
        }
        return false;
    }

    private boolean categoryIsAmongUsedCategoriesAndParentCategories(Category category) {
        for (Category usedCategory : usedCategoriesAndParentsList) {
            if (usedCategory.equals(category))
                return true;
        }
        return false;
    }

    @EventListener
    private void handleArticleAddedEvent(ArticleAddedEvent articleAddedEvent) {
        processArticleAddedEvent(articleAddedEvent.getArticle());
    }

    @EventListener
    private void handleArticleDeletedEvent(ArticleDeletedEvent articleDeletedEvent) {
        processArticleDeletedEvent(articleDeletedEvent.getArticle());
    }

    @EventListener
    private void handleArticleEditedEvent(ArticleEditedEvent articleEditedEvent) {
        processArticleDeletedEvent(articleEditedEvent.getArticle());
        processArticleAddedEvent(articleEditedEvent.getArticle());
    }

    @EventListener
    private void handleCategoryDeletedEvent(CategoryDeletedEvent categoryDeletedEvent) {
        Category deletedCategory = categoryDeletedEvent.getCategory();

        if (!categoryIsAmongUsedCategories(deletedCategory)) return;

        usedCategories.remove(deletedCategory);
    }

    private void processArticleAddedEvent(Article createdArticle) {
        Category createdArticleCategory = createdArticle.getCategory();
        if (categoryIsAmongUsedCategories(createdArticleCategory)) return;

        usedCategories.add(createdArticleCategory);
//        usedCategoriesAndParentsList.add(createdArticleCategory);

        attachBrachToTree(createdArticleCategory);
    }

    private void attachBrachToTree(Category c) {
        usedCategoriesAndParentsList.add(c);
        Category parentCategory = c.getParentCategory();

        if (parentCategory == null) {
            hierarchy.getSubcategories().add(c);
            return;
        }

        for (Category usedCategoryOrParentCategory: usedCategoriesAndParentsList) {
            if (usedCategoryOrParentCategory.equals(parentCategory))
                usedCategoryOrParentCategory.getSubcategories().add(c);
        }

        Set<Category> cSet = new HashSet<>();
        cSet.add(c);
        parentCategory.setSubcategories(cSet);

        attachBrachToTree(parentCategory);
    }

    private void processArticleDeletedEvent(Article deletedArticle) {
        Category deletedArticleCategory = deletedArticle.getCategory();
        Category subhierarchy = getSubhierarchy(deletedArticleCategory);

        Category actualCategory = categoryService.findById(deletedArticleCategory.getId());
        Set<Article> articlesWithDeletedArticleCategory = actualCategory.getArticles();
        articlesWithDeletedArticleCategory.remove(deletedArticle);
        if (articlesWithDeletedArticleCategory.size() != 0) return;
        usedCategories.remove(subhierarchy);

        if (isParentCategory(subhierarchy))
            return;
        usedCategoriesAndParentsList.remove(subhierarchy);

        cutOffBranchFromTree(subhierarchy);
    }

    private void cutOffBranchFromTree(Category c) { // if category comes here then it's not among used categories - obviously
        Category parentCategory = c.getParentCategory();

        Set<Category> subcategories = parentCategory.getSubcategories();
        if (subcategories.size() > 1) {
            subcategories.remove(c);
            return;
        }

        usedCategories.remove(parentCategory);
        usedCategoriesAndParentsList.remove(parentCategory);

        if (parentCategory.getParentCategory() == null) return;

        cutOffBranchFromTree(parentCategory);
    }

    private boolean isParentCategory(Category category) {
        if (category.getSubcategories().size() == 0) return false;
        return true;
    }
}