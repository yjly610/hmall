package com.hmall.api.client;

import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.api.fallback.ItemClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "item-service",fallbackFactory = ItemClientFallbackFactory.class)
public interface ItemClient {
    @GetMapping("/items")
    public List<ItemDTO> queryItemByIds(@RequestParam("ids") Iterable<Long> ids);

    //批量减库存
    @PutMapping("/items/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items);

    /**
     * 根据id查询商品
     * @param id
     * @return
     */
    @GetMapping("/items/{id}")
    public ItemDTO queryItemById(@PathVariable("id") Long id);
}
