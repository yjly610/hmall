package com.hmall.search.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDocDTO;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import com.hmall.common.domain.PageDTO;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Service;
import org.elasticsearch.client.RequestOptions;
import com.hmall.search.service.ISearchService;
import org.elasticsearch.search.sort.SortOrder;
import com.hmall.search.domain.query.ItemPageQuery;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ISearchServiceImpl implements ISearchService {

    private final RestHighLevelClient client;

    /**
     * 分页查询商品数据
     * @param query
     * @return
     */
    @Override
    public PageDTO<ItemDocDTO> search(ItemPageQuery query) {
        //准备查询对象
        SearchRequest request = new SearchRequest("items");
        //设置搜索条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (ObjectUtil.isNotEmpty(query.getKey())){
            boolQuery.must(QueryBuilders.matchQuery("name",query.getKey()));
        }
        if (ObjectUtil.isNotEmpty(query.getCategory())){
            boolQuery.filter(QueryBuilders.termQuery("category",query.getCategory()));
        }
        if (ObjectUtil.isNotEmpty(query.getBrand())){
            boolQuery.filter(QueryBuilders.termQuery("brand",query.getBrand()));
        }
        //价格
        if(query.getMinPrice() != null && query.getMinPrice() >0){
            boolQuery.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }
        if(query.getMaxPrice() != null && query.getMaxPrice() != 999999){
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }
        request.source().query(boolQuery);

        //排序
        if (ObjectUtil.isNotEmpty(query.getSortBy())){
            request.source().sort(query.getSortBy(),query.getIsAsc() ? SortOrder.ASC:SortOrder.DESC);
        }

        // 分页查询
        Integer from = (query.getPageNo()-1)*query.getPageSize();
        request.source().from(from).size(query.getPageSize());

        //高亮
        request.source().highlighter(new HighlightBuilder().field("name"));
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            PageDTO<ItemDocDTO> pageDTO =  handRespnse(response);
            return pageDTO;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 有问题返回空
        return null;
    }


    //结果处理抽取
    public PageDTO<ItemDocDTO> handRespnse(SearchResponse response ) {
        //获取第一层hits
        SearchHits hits = response.getHits();
        //获取total
        long total = hits.getTotalHits().value;
        System.out.println("查询总条数：" + total);

        List<ItemDocDTO> itemDocDTOS = new ArrayList<>();
        //获取第二层hits
        SearchHit[] oldData = hits.getHits();
        for (SearchHit old : oldData) {
            String sourceAsString = old.getSourceAsString();
            ItemDocDTO itemDocDTO = JSONUtil.toBean(sourceAsString, ItemDocDTO.class);
            Map<String, HighlightField> highlightFields = old.getHighlightFields();
            if (highlightFields != null) {
                HighlightField nameHight = highlightFields.get("name");
                if (nameHight != null) {
                    String name = nameHight.getFragments()[0].toString();
                    itemDocDTO.setName(name);
                }
            }
            itemDocDTOS.add(itemDocDTO);
        }
        long pages = (long) Math.ceil((double) total/itemDocDTOS.size());
        return new PageDTO<ItemDocDTO>(total,pages,itemDocDTOS);
    }
}
