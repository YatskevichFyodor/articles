package fyodor.controller;


import fyodor.model.Article;
import fyodor.model.Category;
import fyodor.service.CategoryService;
import fyodor.service.IArticleService;
import fyodor.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;

@Controller
public class WebController {

	@Autowired
	private IUserService userService;

	@Autowired
    private IArticleService articleService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private LocaleResolver localeResolver;

	@GetMapping(value = { "/", "home" })
	public String home(HttpServletRequest request, Model model) {
		String messageValue = messageSource.getMessage("message.welcome", null, localeResolver.resolveLocale(request));
		model.addAttribute("message", messageValue);
		model.addAttribute("articlesMatrix", getArticlesMatrix());

		return "index";
	}

	public List<List<Article>> getArticlesMatrix() {
		List<Article> simpleArticlesList = articleService.findAll();
		List<List<Article>> articlesMatrix = new LinkedList();
		Iterator<Article> iterator = simpleArticlesList.iterator();

		int i = 0;
		List<Article> innerList = new LinkedList<>();
		while (iterator.hasNext()) {
			if (i % 4 == 0) {
				innerList = new LinkedList<>();
				articlesMatrix.add(innerList);
			}
			Article article = iterator.next();
			innerList.add(article);
			i++;
		}
		return articlesMatrix;
	}

	@GetMapping("/profile")
	public String userProfile(HttpServletRequest request, Model model, Principal principal) {
		String username = principal.getName();
		List<Category> listOfCategories = categoryService.findByArticlesIn(articleService.
				findByAuthor(userService.findByUsernameIgnoreCase((username))));
		Set<Category> setOfCategories = new HashSet<Category>(listOfCategories);
		model.addAttribute("user", userService.findByUsernameIgnoreCase(principal.getName()));
		model.addAttribute("setOfCategories", setOfCategories);

		return "profile";
	}

	@GetMapping("/ajax-test")
	public String ajaxTest(HttpServletRequest request) {
		return "ajax";
	}


	@GetMapping("add-article")
	public String addArticle(HttpServletRequest request, Model model) {
		model.addAttribute("listOfCategories", categoryService.findAll());
		//model.addAttribute("article", new Article());

		return "add-article";
	}

    @GetMapping("article/{articleName}")
    public String article(HttpServletRequest request, Model model, @PathVariable String articleName) {
	    Article article = articleService.findByTitle(articleName);
        model.addAttribute("article", article);
        return "article";
    }

    @PostMapping("/findArticlesByCategory")
    @ResponseBody
    public Set<String> findArticlesByCategory(@RequestBody String id, Principal principal) {
        List<Article> articles = articleService.findByCategoryAndAuthor(categoryService.findById(Long.valueOf(id)),
                userService.findByUsernameIgnoreCase(principal.getName()));
        Set<String> titles = new HashSet<>();
        for (Article article: articles) {
            titles.add(article.getTitle());
        }
        return titles;
    }

    @PostMapping("/add-article")
    @ResponseBody
    public Boolean addArticle(@RequestBody String json, Principal principal) {
        articleService.save(json, principal);

        return true;
    }
}