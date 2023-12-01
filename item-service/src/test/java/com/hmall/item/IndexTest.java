package com.hmall.item;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.api.dto.ItemDocDTO;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.junit.jupiter.api.Test;
import cn.hutool.core.bean.BeanUtil;
import com.hmall.item.domain.po.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.elasticsearch.client.RestClient;
import com.hmall.item.service.IItemService;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class IndexTest {
    private RestHighLevelClient client;

    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\":{\n" +
            "        \"type\": \"long\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_smart\"\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"stock\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Autowired
    private IItemService iItemService;


    @BeforeEach
    public void testclient(){
        System.out.println("创建资源");
        client = new RestHighLevelClient(
                RestClient.builder(
                HttpHost.create("http://192.168.200.146:9200")));
    }

    @Test
    public void testClient(){
        System.out.println(client);
    }

    /**
     * 创建索引
     */
    @Test
    public void createIndex() throws IOException {
        // 1.创建Request对象
        CreateIndexRequest request = new CreateIndexRequest("items");
        // 2.准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3.发送请求
        client.indices().create(request, RequestOptions.DEFAULT);

    }


    /**
     * 判断索引随否存在
     */
    @Test
    public void existsIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest();
        request.indices("items");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }


    /**
     *创建/全量更新 文档
     */
    @Test
    public void createDocumentTest() throws IOException {
        //准备数据
        Item item = iItemService.getById(317578L);
        ItemDocDTO itemDocDTO = BeanUtil.toBean(item, ItemDocDTO.class);
        //准备请求对象
        IndexRequest request = new IndexRequest("items").id(itemDocDTO.getId()+"");
        //准备数据
        request.source(JSONUtil.toJsonStr(itemDocDTO),XContentType.JSON);
        //创建文档
        client.index(request,RequestOptions.DEFAULT);
    }

    /**
     * 查询文档
     */
    @Test
    public void getDocumentTest() throws IOException {
        GetRequest request = new GetRequest("items").id("317578");
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        System.out.println(response);
    }

    /**
     * 删除文档
     */
    @Test
    public void deleteDocumentTest() throws IOException {
        DeleteRequest request = new DeleteRequest("items").id("317578");
       client.delete(request, RequestOptions.DEFAULT);
    }

    /**
     * 局部更新文档
     */
    @Test
    public void updateDocumentTest() throws IOException {
        UpdateRequest request = new UpdateRequest("items","317578");
        //准备更新数据
        request.doc("price",990);
        client.update(request,RequestOptions.DEFAULT);
    }


    /**
     * 批量插入数据
     * @throws IOException
     */
    @Test
    public void bulkDocumentTest() throws IOException {
        //准备数据
        Item item = iItemService.getById(317578L);
        BulkRequest request = new BulkRequest("items");
        request.add(new IndexRequest("items").id(item.getId()+"").source(JSONUtil.toJsonStr(BeanUtil.toBean(item,ItemDocDTO.class)),XContentType.JSON));
        client.bulk(request,RequestOptions.DEFAULT);
    }

    /**
     * 批量插入数据
     */
    @Test
    public void bulkAllDocumentTest() throws IOException {
        long pageNum = 1;
        long pageSize = 1000;
        while (true) {
            //准备数据
            Page<Item> page = iItemService.lambdaQuery().eq(Item::getStatus, 1).page(Page.of(pageNum, pageSize));
            //获取分页数据
            List<Item> records = page.getRecords();

            if (ObjectUtil.isEmpty(records)){
                return;
            }
            BulkRequest request = new BulkRequest("items");
            for (Item item : records) {
                request.add(new IndexRequest("items").id(item.getId()+"")
                        .source(JSONUtil.toJsonStr(BeanUtil.toBean(item,ItemDocDTO.class)),XContentType.JSON));
            }
            client.bulk(request,RequestOptions.DEFAULT);
            pageNum++;
        }
    }


    @AfterEach
    public void testclose() throws IOException {
        System.out.println("销毁资源");
        client.close();
    }
}
