package com.github.hcsp;

import org.apache.http.HttpHost;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElasticsearchDataGenerator {
    public static void main(String[] args) throws IOException {
        SqlSessionFactory sqlSessionFactory;

        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<News> newsFromMySQL = getNewsFromMySQL(sqlSessionFactory);
        for (int i = 0; i < 2; i++) {
            new Thread(() -> writeSingleThread(newsFromMySQL)).start();
        }


    }

    private static void writeSingleThread(List<News> newsFromMySQL) {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))) {

            //单线程写入2000*100=20_0000的数据
            for (int i = 0; i < 100; i++) {
                //ES的批处理
                BulkRequest bulkRequest = new BulkRequest();
                for (News news : newsFromMySQL) {

                    IndexRequest request = new IndexRequest("news");  //类似需要操作哪张表
                    Map<String, Object> map = new HashMap<>();
                    map.put("url", news.getUrl());
                    map.put("content", news.getContent().length() > 10 ? news.getContent().substring(0, 10) : news.getContent());
                    map.put("title", news.getTitle());
                    map.put("create_at", news.getCreatedAt());
                    map.put("modified_at", news.getModifiedAt());
                    request.source(map, XContentType.JSON);

                    /*IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                    System.out.println(response.status().getStatus());*/

                    bulkRequest.add(request);
                }
                BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
                System.out.println("Current thread:" + Thread.currentThread().getName() + "finishes " + i + ":" + bulkResponse.status().getStatus());


            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<News> getNewsFromMySQL(SqlSessionFactory sqlSessionFactory) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            //捞取2000条种子数据
            return session.selectList("com.github.hcsp.MockDataMapper.selectNewsForES");
        }
    }
}
