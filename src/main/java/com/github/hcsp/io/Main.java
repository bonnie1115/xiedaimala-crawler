package com.github.hcsp.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "root";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {

        String jdbcUrl = "jdbc:h2:file:/Users/jiangrui/java/my-git/xiedaimala-crawler/news";
        Connection connection = DriverManager.getConnection(jdbcUrl, USER_NAME, PASSWORD);

        while (true) {

            //待处理的链接池---》从数据库加载即将处理的链接的代码
            List<String> linkPool = loadUrlsFromDataBase(connection, "select link from links_to_be_processed");

            //已经处理的链接池---》从数据库加载已经处理链接的代码  这里注意返回的是List
            //Set<String> processLinks =new HashSet<>(loadUrlsFromDataBase(connection,"select link from links_already_processed"));
            //ArrayList从尾部删除更有效率  remove会返回刚刚删除的元素，所以这里直接remove
            //每次处理完后，更新数据库
            String link = linkPool.remove(linkPool.size() - 1);
            //从待处理的池子中捞一个链接出来处理，处理完之后删除此链接
            insertLinkIntoDataBase(connection, link, "delete from LINKS_TO_BE_PROCESSED where link = ? ");

            //询问数据库，当前链接是不是已经被处理过了
            if (!isLinkProcessed(connection, link)) {
                //只关心news.cn的排除掉passport的
                //if (link.contains("news.sina.cn") || "https://sina.cn".equals(link)&&!link.contains("passprt.sina.cn")) {
                if (isInterestingInk(link)) {
                    Document doc = httpGetAndParseHtml(link);
                    ArrayList<Element> links = doc.select("a");
                    parseUrlFromPageAndStoreIntoDataBase(connection, links);
                    //假如这是一个新闻的详情页面，则存入数据库，否则就什么都不做
                    storeIntoDataBaseIfItIsNewsPage(doc);
                    insertLinkIntoDataBase(connection, link, "insert into LINKS_ALREADY_PROCESSED values (?) ");

                }
            }


        }

    }

    private static void parseUrlFromPageAndStoreIntoDataBase(Connection connection, ArrayList<Element> links) throws SQLException {
        for (Element aTag : links) {
//                    linkPool.add(aTag.attr("href")); //丢到链接池/**/
            String href = aTag.attr("href");
            insertLinkIntoDataBase(connection, href, "insert into LINKS_TO_BE_PROCESSED values (?) ");

        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void insertLinkIntoDataBase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> loadUrlsFromDataBase(Connection connection, String sql) throws SQLException {
        List<String> results = new ArrayList<>();
        ;
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return results;

    }


    private static void storeIntoDataBaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        //这是我们感兴趣的，我们只处理新浪站内的链接
        CloseableHttpClient httpclient = HttpClients.createDefault();
        System.out.println(link);
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println(link);
        }

        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);  //String 格式的html

            return Jsoup.parse(html);
        }

    }

    private static boolean isInterestingInk(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link) && isNotNewsZtPage(link);

    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isNotNewsZtPage(String link) {
        return !link.contains("news.sina.cn\\/news_zt\\/");
    }
}
