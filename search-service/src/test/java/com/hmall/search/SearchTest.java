package com.hmall.search;


import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDocDTO;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SearchTest {

    private RestHighLevelClient client;
    @BeforeEach
    public void testclient(){
        System.out.println("创建资源");
        client = new RestHighLevelClient(
                RestClient.builder(
                        HttpHost.create("http://192.168.200.146:9200")));
    }




    @Test
    public void test() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().query(QueryBuilders.matchAllQuery());
        //获取搜索结果集
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //解析结果集获取数据
        printRespnse(search);
    }

    //模糊搜索
    @Test
    public void MustSearchTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        //获取搜索结果集
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //解析结果集获取数据
        printRespnse(search);
    }


    //精确查询
    @Test
    public void termSearchTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().query(QueryBuilders.termQuery("brand","德亚"));
        //获取搜索结果集
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //解析结果集获取数据
        printRespnse(search);
    }

    //精确范围查询
    @Test
    public void rangeSearchTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().query(QueryBuilders.rangeQuery("price").gte(90000).lte(159900));
        //获取搜索结果
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //结果处理
        printRespnse(search);
    }


    //精确范围查询
    @Test
    public void boolSearchTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("name","智能手机"));
        boolQuery.filter(QueryBuilders.termQuery("brand","华为"));
        boolQuery.filter(QueryBuilders.rangeQuery("price").gte(90000).lte(159900));

        request.source().query(boolQuery);
        //获取搜索结果
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //结果处理
        printRespnse(search);
    }


    //分组
    @Test
    public void aggsTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().size(0);
        request.source().aggregation(AggregationBuilders.terms("test_aggs")
                .field("brand").size(20));
        //获取搜索结果
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //结果处理
        printRespnse(search);

        //分组结果处理
        Aggregations aggregations = search.getAggregations();
        //Terms需要手写
        Terms terms = aggregations.get("test_aggs");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            String brand = bucket.getKeyAsString();
            System.out.println(brand);
        }
    }



    //分页排序查询
    @Test
    public void pageOderSearchTest() throws IOException {
        //构建搜索条件
        SearchRequest request = new SearchRequest("items");
        //准备搜索数据
        request.source().query(QueryBuilders.matchQuery("name","荣耀手机"));
        request.source().from(0).size(50);
        request.source().sort("price", SortOrder.DESC);
        request.source().highlighter(new HighlightBuilder().field("name"));
        //获取搜索结果
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        //结果处理
        printRespnse(search);
    }


    //结果处理抽取
    public void printRespnse(SearchResponse search ){
        //获取第一层hits
        SearchHits hits = search.getHits();
        //获取total
        long total = hits.getTotalHits().value;
        System.out.println("查询总条数："+total);
        //获取第二层hits
        SearchHit[] oldData = hits.getHits();
        for (SearchHit old : oldData) {
            String sourceAsString = old.getSourceAsString();
            ItemDocDTO itemDocDTO = JSONUtil.toBean(sourceAsString, ItemDocDTO.class);
            Map<String, HighlightField> highlightFields = old.getHighlightFields();
            if (highlightFields != null){
                HighlightField nameHight = highlightFields.get("name");
                if (nameHight != null){
                    String name = nameHight.getFragments()[0].toString();
                    itemDocDTO.setName(name);
                }
            }
            System.out.println(itemDocDTO);
        }
//        List<ItemDocDTO> collect = Arrays.stream(oldData).map(old -> old.getSourceAsString()).map(o -> JSONUtil.toBean(o, ItemDocDTO.class)).collect(Collectors.toList());
//        System.out.println(collect);
    }

    @AfterEach
    public void testclose() throws IOException {
        System.out.println("销毁资源");
        client.close();
    }

}