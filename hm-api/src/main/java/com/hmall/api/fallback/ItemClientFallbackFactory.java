package com.hmall.api.fallback;

import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {
    @Override
    public ItemClient create(Throwable throwable) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Iterable<Long> ids) {
                log.error("查询用户失败",throwable);
                return new ArrayList<>();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("查询失败",throwable);
                throw new RuntimeException("库存扣减失败");
            }

            /**
             * 根据id查询商品
             * @param id
             * @return
             */
            @Override
            public ItemDTO queryItemById(Long id) {
                return null;
            }
        };
    }
}
