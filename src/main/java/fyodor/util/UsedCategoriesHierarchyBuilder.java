package fyodor.util;


import fyodor.events.ArticleAddedEvent;
import fyodor.events.ArticleDeletedEvent;
import fyodor.events.CategoryDeletedEvent;
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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
//@DependsOn(value = {"categoryRepository", "categoryService"})
@Data
public class UsedCategoriesHierarchyBuilder {

    @Autowired
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private CategoryRepository categoryRepository;

    @Autowired
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ICategoryService categoryService;

    @Setter(AccessLevel.NONE)
    private final Category hierarchy = new Category();

    @Setter(AccessLevel.NONE)
    private Set<Category> usedCategories;

    @Setter(AccessLevel.NONE)
    private Set<Category> usedCategoriesAndParentsSet; // used on building stage

    @Setter(AccessLevel.NONE)
    private List<Category> usedCategoriesAndParentsList; // used after building stage

    @PostConstruct
    private void build() {
        usedCategories = new HashSet<>(categoryRepository.findUsedCategories());
        usedCategoriesAndParentsSet = new HashSet<>(usedCategories);
        for (Category category: usedCategories) {
            insertParentCategories(category);
        }

        Set<Category> rootCategories = new HashSet<>();
        for (Category category: usedCategoriesAndParentsSet) {
            if (category.getParentCategory() == null)
                rootCategories.add(category);
        }

        hierarchy.setSubcategories(rootCategories);

        usedCategoriesAndParentsList = new ArrayList<>();
        usedCategoriesAndParentsList.addAll(rootCategories);

        for (Category category: hierarchy.getSubcategories()) {
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
        for (Category subcategory: usedCategoriesAndParentsList) {
            if (subcategory.equals(primaryCategory))
                return subcategory;
        }

        throw new RuntimeException("Subhierarchy wasn't found");
    }

    @EventListener
    private void handleArticleAddedEvent(ArticleAddedEvent articleAddedEvent) {
        Article createdArticle = articleAddedEvent.getArticle();
        Category createdArticleCategory = createdArticle.getCategory();
        if (categoryIsAmongUsedCategories(createdArticleCategory)) return;

        usedCategories.add(createdArticleCategory);

        attachBrachToTree(createdArticleCategory);
    }

    private void attachBrachToTree(Category c) {
        Category parentCategory = c.getParentCategory();

        Set<Category> cSet = new HashSet<>();
        cSet.add(c);
        parentCategory.setSubcategories(cSet);

        usedCategoriesAndParentsList.add(c);

        if (parentCategory == null) {
            hierarchy.getSubcategories().add(c);
            return;
        }

        for (Category usedCategory : usedCategories) {
            if (usedCategory.equals(parentCategory)) {
                usedCategory.getSubcategories().add(c);
                return;
            }
        }

        attachBrachToTree(parentCategory);
    }

    @EventListener
    private void handleArticleDeletedEvent(ArticleDeletedEvent articleDeletedEvent) {
        Article deletedArticle = articleDeletedEvent.getArticle();
        Category deletedArticleCategory = deletedArticle.getCategory();

        if (!categoryIsAmongUsedCategories(deletedArticleCategory)) return;

        usedCategories.remove(deletedArticleCategory);

        cutOffBranchFromTree(deletedArticleCategory);
    }

    private void cutOffBranchFromTree(Category c) {
        if (categoryIsAmongUsedCategories(c))
            return;
        Category parentCategory = c.getParentCategory();
        if (parentCategory == null) {
            hierarchy.getSubcategories().remove(c);
            return;
        } else {
            cutOffBranchFromTree(parentCategory);
        }
    }

    @EventListener
    private void handleCategoryDeletedEvent(CategoryDeletedEvent categoryDeletedEvent) {
        Category deletedCategory = categoryDeletedEvent.getCategory();

        if (!categoryIsAmongUsedCategories(deletedCategory)) return;

        usedCategories.remove(deletedCategory);
    }

    private boolean categoryIsAmongUsedCategories(Category category) {
        for (Category usedCategory: usedCategories) {
            if (usedCategory.equals(category))
                return true;
        }
        return false;
    }
}