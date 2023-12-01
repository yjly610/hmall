package com.hmall.search.service;

import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.ItemDocDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.query.ItemPageQuery;

public interface ISearchService {
    /**
     * 分页查询商品数据
     * @param query
     * @return
     */
    PageDTO<ItemDocDTO> search(ItemPageQuery query);
}
