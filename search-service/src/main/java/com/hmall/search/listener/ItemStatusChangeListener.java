package com.hmall.search.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.ItemDocDTO;
import com.hmall.common.constants.ItemStatusChangeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemStatusChangeListener {

    private final ItemClient itemClient;

    private final RestHighLevelClient client;


    /**
     * 上架商品监听
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = ItemStatusChangeConstants.ITEM_UP_QUEUE),
            exchange = @Exchange(value = ItemStatusChangeConstants.ITEM_TOPIC,type = ExchangeTypes.TOPIC),
            key = ItemStatusChangeConstants.ITEM_UP_KEY
    ))
    public void itemUpListener(Long id){
        ItemDTO itemDTO = itemClient.queryItemById(id);
        ItemDocDTO itemDocDTO = BeanUtil.toBean(itemDTO, ItemDocDTO.class);
        //准备请求对象
        IndexRequest request = new IndexRequest("items").id(itemDocDTO.getId()+"");
        //准备数据
        request.source(JSONUtil.toJsonStr(itemDocDTO), XContentType.JSON);
        //创建文档
        try {
            client.index(request, RequestOptions.DEFAULT);
            System.out.println("同步商品"+id+"成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 下架商品监听
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = ItemStatusChangeConstants.ITEM_DOWN_QUEUE),
            exchange = @Exchange(value = ItemStatusChangeConstants.ITEM_TOPIC,type = ExchangeTypes.TOPIC),
            key = ItemStatusChangeConstants.ITEM_DOWN_KEY
    ))
    public void itemDownListener(Long id){
        DeleteRequest request = new DeleteRequest("items").id(id+"");
        try {
            client.delete(request, RequestOptions.DEFAULT);
            System.out.println("删除商品"+id+"成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
