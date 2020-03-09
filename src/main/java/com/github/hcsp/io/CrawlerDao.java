package com.github.hcsp.io;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

    void updateDataBase(String link, String sql) throws SQLException;

    void insertIntoNews(String url, String title, String content) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

}
