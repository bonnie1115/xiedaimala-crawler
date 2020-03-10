package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

@SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
public class JdbcCrawlerDao implements CrawlerDao {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";
    String jdbcUrl = "jdbc:h2:file:/Users/jiangrui/java/my-git/xiedaimala-crawler/news";
    Connection connection;

    public JdbcCrawlerDao() {
        try {
            connection = DriverManager.getConnection(jdbcUrl, USER_NAME, PASSWORD);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private  String getNextLink(String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null; //没找到则return null

    }

    public boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where link = ? ")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
            return false;

        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }

    }


    public void updateDataBase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }


    public void insertIntoNews(String url, String title, String content) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into NEWS (title,url,content,CREATED_AT,MODIFIED_AT) values(?,?,?,now(),now())")) {
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, url);
            preparedStatement.setString(3, content);
            preparedStatement.executeUpdate();
        }
    }

    public String getNextLinkThenDelete() throws SQLException {
        //先从数据库拿出来一个链接（拿出来并从数据库中删除，防止拿到的链接是同一个），准备处理
        String link = getNextLink("select link from links_to_be_processed limit 1");
        if (link != null) {
            //从待处理的池子中捞一个链接出来处理，处理完之后删除此链接
            updateDataBase(link, "delete from LINKS_TO_BE_PROCESSED where link = ? ");
        }

        return link;


    }

    @Override
    public void insertProcessedLink(String link) {

    }

    @Override
    public void insertLinkToBeProcessed(String href) {

    }



    /*public String selectLinksFromAlreadyProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select LINK from LINKS_ALREADY_PROCESSED where link = ? ")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
            return false;

        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }*/
}

