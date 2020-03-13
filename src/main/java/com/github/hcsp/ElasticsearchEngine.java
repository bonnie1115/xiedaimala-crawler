package com.github.hcsp;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import javax.naming.directory.SearchResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ElasticsearchEngine {
    public static void main(String[] args) throws IOException {
       while(true)  {
            System.out.println("please input a word");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String keyword = bufferedReader.readLine();
            search(keyword);
        }
    }

    private static void search(String keyword) throws IOException {
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("localhost", 9200, "http")))){

            SearchRequest searchResult = new SearchRequest("news");
            //根据title和content匹配查找
            //source 就是一个document 就是一个文档里的所有内容
            searchResult.source(new SearchSourceBuilder().query(new MultiMatchQueryBuilder(keyword,"title","content")));
            SearchResponse result = client.search(searchResult, RequestOptions.DEFAULT);

            result.getHits().forEach(hit-> System.out.println(hit.getSourceAsString()));
        }
    }
}
