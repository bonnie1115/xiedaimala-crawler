package com.github.hcsp;

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
import java.util.stream.Collectors;


public class Crawler {
    CrawlerDao dao = new MyBatisCrawlerDao();

    public void run() throws SQLException, IOException {

        String link;
        //从数据库中加载一个链接，能加载到才开始循环
        while ((link = dao.getNextLinkThenDelete()) != null) {

            //询问数据库，当前链接是不是已经被处理过了
            if (!dao.isLinkProcessed(link)) {
                //只关心news.cn的排除掉passport的
                //if (link.contains("news.sina.cn") || "https://sina.cn".equals(link)&&!link.contains("passprt.sina.cn")) {
                if (isInterestingInk(link)) {
                    Document doc = httpGetAndParseHtml(link);
                    ArrayList<Element> links = doc.select("a");
                    parseUrlFromPageAndStoreIntoDataBase(links);
                    //假如这是一个新闻的详情页面，则存入数据库，否则就什么都不做
                    storeIntoDataBaseIfItIsNewsPage(doc, link);
                    dao.insertProcessedLink(link);
                   // dao.updateDataBase(link, "insert into LINKS_ALREADY_PROCESSED values (?) ");

                }
            }


        }
    }


    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {

        new Crawler().run();

    }

    public void parseUrlFromPageAndStoreIntoDataBase(ArrayList<Element> links) throws SQLException {

        for (Element aTag : links) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
                System.out.println(href);
            }

            if (!href.toLowerCase().startsWith("javascript")) {
                dao.insertLinkToBeProcessed(href);
            }

        }
    }


    public void storeIntoDataBaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                ArrayList<Element> paragraphs = articleTag.select("p");  //拿到文章的每个段落
                String content = paragraphs.stream().map(Element::text).collect(Collectors.joining("\n"));  //每个段落以换行符分割，得到新闻的内容
                dao.insertIntoNews(link, title, content);

            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        //这是我们感兴趣的，我们只处理新浪站内的链接
        CloseableHttpClient httpclient = HttpClients.createDefault();
        System.out.println(link);


        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);  //String 格式的html

            return Jsoup.parse(html);
        }

    }

    public boolean isInterestingInk(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link) && isNotNewsZtPage(link);

    }

    public boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    public boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    public boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    public boolean isNotNewsZtPage(String link) {
        return !link.contains("news.sina.cn\\/news_zt\\/");
    }
}

