package fyodor.repository;

import fyodor.model.Article;
import fyodor.model.Category;
import fyodor.model.Image;
import fyodor.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArticleDao {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private Connection connect;

    public ArticleDao() {
        try {
            connect = ConnectorDB.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Article covertResultSetToArticle(ResultSet rs) throws SQLException {
        Article article = new Article();
        article.setId(rs.getInt(1));
        article.setContent(rs.getString(2));
        article.setTimestamp(rs.getDate(3));
        article.setTitle(rs.getString(4));
        User author = userRepository.findById(rs.getLong(5));
        article.setAuthor(author);
        Category category = categoryRepository.findById(rs.getLong(6));
        article.setCategory(category);
        Image image = imageRepository.findById(rs.getLong(7));
        article.setImage(image);
        article.setPopularity(rs.getLong(8));

        return article;
    }

    public List<Article> getArticlesByCategoriesSortedByPopularity(List<Category> categories) throws SQLException {
        StringBuilder statement = new StringBuilder("" +
                "SELECT *\n" +
                "FROM article a\n" +
                "WHERE\n" +
                "  a.category_id in (");
        for (Category category: categories) {
            statement.append(category.getId());
            statement.append(", ");
        }
        statement.delete(statement.length() - 2, statement.length());
        statement.append(")\n" +
                "ORDER BY popularity DESC;");

        PreparedStatement ps = connect.prepareStatement(statement.toString());
        ResultSet rs = ps.executeQuery();

        List<Article> result = new ArrayList<>();
        while (rs.next()) {
            result.add(covertResultSetToArticle(rs));
        }

        if (ps != null)
            ps.close();

        return result;
    }

    public List<Article> getArticlesByCategoriesSortedByDateDesc(List<Category> categories) throws SQLException {
        StringBuilder statement = new StringBuilder("" +
                "SELECT *\n" +
                "FROM article a\n" +
                "WHERE\n" +
                "  a.category_id in (");
        for (Category category: categories) {
            statement.append(category.getId());
            statement.append(", ");
        }
        statement.delete(statement.length() - 2, statement.length());
        statement.append(")\n" +
                "ORDER BY timestamp DESC;");

        PreparedStatement ps = connect.prepareStatement(statement.toString());
        ResultSet rs = ps.executeQuery();

        List<Article> result = new ArrayList<>();
        while (rs.next()) {
            result.add(covertResultSetToArticle(rs));
        }

        if (ps != null)
            ps.close();

        return result;
    }

    public List<Article> getArticlesByCategoriesSortedByDateAsc(List<Category> categories) throws SQLException {
        StringBuilder statement = new StringBuilder("" +
                "SELECT *\n" +
                "FROM article a\n" +
                "WHERE\n" +
                "  a.category_id in (");
        for (Category category: categories) {
            statement.append(category.getId());
            statement.append(", ");
        }
        statement.delete(statement.length() - 2, statement.length());
        statement.append(")\n" +
                "ORDER BY timestamp ASC;");

        PreparedStatement ps = connect.prepareStatement(statement.toString());
        ResultSet rs = ps.executeQuery();

        List<Article> result = new ArrayList<>();
        while (rs.next()) {
            result.add(covertResultSetToArticle(rs));
        }

        if (ps != null)
            ps.close();

        return result;
    }
}
