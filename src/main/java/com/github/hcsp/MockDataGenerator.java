package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {


    private static void mockData(SqlSessionFactory sqlSessionFactory, int howMany) {

           //处理方式：批处理ExecutorType.BATCH
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {

            List<News> currentNews = session.selectList("com.github.hcsp.MockDataMapper.selectNews");
            System.out.println("拿到多有的新闻");
            int count = howMany - currentNews.size();

            Random random = new Random();
            while (count-- > 0) {
                try {
                    int index = random.nextInt(currentNews.size());
                    News newsToBeInserted = new News(currentNews.get(index));

                    //为了后续索引，将时间改下之后再set回去
                    Instant currentTime = newsToBeInserted.getCreatedAt();
                    currentTime = currentTime.minusMillis(random.nextInt(3600 * 24 * 365));
                    newsToBeInserted.setCreatedAt(currentTime);
                    newsToBeInserted.setModifiedAt(currentTime);
                    session.insert("com.github.hcsp.MockDataMapper.insertNews",newsToBeInserted);
                    System.out.println("Left:" + count);
                    if(count%2000==0){
                        session.flushStatements();
                    }

                    session.commit();  //为了保证数据的原子性
                } catch (Exception e) {
                    session.rollback();  //为了保证数据的原子性
                    throw new RuntimeException(e);

                }
            }
        }


    }


    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory;

        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        mockData(sqlSessionFactory, 50_0000);


    }
}