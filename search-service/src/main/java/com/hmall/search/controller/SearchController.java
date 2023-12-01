package com.hmall.search.controller;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.ItemDocDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {


    private final ISearchService searchService;


    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDocDTO> search(ItemPageQuery query) {
        return searchService.search(query);

    }




}
