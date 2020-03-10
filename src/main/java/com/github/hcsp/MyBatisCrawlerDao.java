package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    MyBatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = session.selectOne("com.github.hcsp.MyMapper.selectNextAvailableLink");
            if (link != null) {
                session.delete("com.github.hcsp.MyMapper.deleteLinksFromLinksToBeProcessed", link);
            }
            return link;
        }
    }


    @Override
    public void insertIntoNews(String url, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertNews", new News(title, url, content));
        }
    }


    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = (Integer) session.selectOne("com.github.hcsp.MyMapper.countLink", link);
            return count != 0;
        }

    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String, Object> param = new HashMap();
        param.put("tableName", "links_already_processed");
        param.put("link", link);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLink", param);

        }

    }

    @Override
    public void insertLinkToBeProcessed(String href) {
        Map<String, Object> param = new HashMap();
        param.put("tableName", "links_to_be_processed");
        param.put("link", href);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.MyMapper.insertLink", param);

        }

    }

}