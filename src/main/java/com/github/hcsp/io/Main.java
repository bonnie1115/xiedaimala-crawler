package com.github.hcsp.io;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Main {
    public static void main(String[] args) throws IOException {


//    String link = "http://sina.cn";

        //待处理的链接池
        List<String> linkPool = new ArrayList<>();
        //已经处理的链接池
        Set<String> processLinks = new HashSet<>();

        linkPool.add("https://sina.cn");


        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            //ArrayList从尾部删除更有效率  remove会返回刚刚删除的元素，所以这里直接remove
            String link = linkPool.remove(linkPool.size() - 1);

            if (processLinks.contains(link)) {
                continue;
            }

            //if (link.contains("news.sina.cn") || "https://sina.cn".equals(link)&&!link.contains("passprt.sina.cn")) {
            if (isInterestingInk(link)) {
                Document doc = httpGetAndParseHtml(link);

                ArrayList<Element> links = doc.select("a");

                for (Element aTag : links) {
                    linkPool.add(aTag.attr("href")); //丢到链接池
                }

                //假如这是一个新闻的详情页面，则存入数据库，否则就什么都不做
                storeIntoDataBaseIfItIsNewsPage(doc);
                processLinks.add(link);
            }

        }

    }

    private static void storeIntoDataBaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).text();
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
        return isNewsPage(link) || isIndexPage(link) && isNotLoginPage(link);

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
}