package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    /*String getNextLink(String sql) throws SQLException;*/

    boolean isLinkProcessed(String link) throws SQLException;

    void insertIntoNews(String url, String title, String content) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    void insertProcessedLink(String link);

    void insertLinkToBeProcessed(String href);
}
