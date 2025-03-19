package com.complain.community;


import com.complain.community.dao.DiscussPostMapper;
import com.complain.community.elasticsearch.DiscussPostRepository;
import com.complain.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SpringBootTest
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void testInsert() {
        discussPostRepository.save(discussMapper.selectDiscussPostById(241));
        discussPostRepository.save(discussMapper.selectDiscussPostById(242));
        discussPostRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100,0));
        discussPostRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100,0));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        discussPostRepository.save(post);
    }

    @Test
    public void testDelete() {
        //  discussPostRepository.deleteById(231);
        discussPostRepository.deleteAll();
    }

    @Test
    public void testSearchByRepository() {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        //elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
        // 底层获取得到了高亮显示的值, 但是没有返回.

        // 使用 ElasticsearchRestTemplate 执行查询
        SearchHits<DiscussPost> searchHits = elasticsearchRestTemplate.search(searchQuery, DiscussPost.class);

        // 输出总命中数
        System.out.println("Total Hits: " + searchHits.getTotalHits());

        // 遍历结果
        for (SearchHit<DiscussPost> hit : searchHits) {
            DiscussPost post = hit.getContent();
            System.out.println("用户：" + post.getUserId());
            System.out.println("顶置：" + post.getType());
            System.out.println("精华：" + post.getScore());
            System.out.println("时间：" + post.getCreateTime());
            System.out.println("标题：" + post.getTitle());
        }
    }
    @Test
    public void testSearchByTemplate() {
        // 构建查询条件
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        // 使用 ElasticsearchRestTemplate 执行查询
        SearchHits<DiscussPost> searchHits = elasticsearchRestTemplate.search(searchQuery, DiscussPost.class);

        // 遍历结果并处理高亮
        List<DiscussPost> resultList = new ArrayList<>();

        for (SearchHit<DiscussPost> hit : searchHits) {
            DiscussPost post = hit.getContent();
            // 处理高亮字段，覆盖到post中的title和content
            Map<String, List<String>> highlightFields = extractHighlightFields(hit);
            if (highlightFields.containsKey("title")) {
                post.setTitle(highlightFields.get("title").get(0));
            }
            if (highlightFields.containsKey("content")) {
                post.setContent(highlightFields.get("content").get(0));
            }

            resultList.add(post);
        }

        // 输出分页信息
        // 所有符合条件的数据条目总数
        System.out.println("Total Elements: " + searchHits.getTotalHits());
        // 当前页号
        System.out.println("Page Number: " + searchQuery.getPageable().getPageNumber());

        System.out.println("Page Size: " + searchQuery.getPageable().getPageSize());

        // 输出每条记录
        for (DiscussPost post : resultList) {
            System.out.println(post);
        }
    }

    /**
     * 提取高亮字段并转换为 Map
     *
     * @param hit 搜索结果中的单个命中项
     * @return 包含高亮字段的 Map
     */
    private Map<String, List<String>> extractHighlightFields(SearchHit<DiscussPost> hit) {
        Map<String, List<String>> highlightFields = new HashMap<>();
        if (hit.getHighlightFields() != null) {
            /*
            * 直接访问键值对 ：
            entrySet() 返回的是一个包含所有键值对的集合（Set<Map.Entry<K, V>>）。
            每个 Map.Entry 对象同时包含键和值，因此可以一次性获取两者。
            性能更优 ：
            只需要一次迭代即可获取所有的键和值。
            避免了多次调用 map.get(key)，减少了查找操作的开销。
            * */

            for (Map.Entry<String, List<String>> entry : hit.getHighlightFields().entrySet()) {
                highlightFields.put(entry.getKey(), entry.getValue());
            }
        }
        return highlightFields;
    }



}
