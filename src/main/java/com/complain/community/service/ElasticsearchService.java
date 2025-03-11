package com.complain.community.service;


import com.complain.community.elasticsearch.DiscussPostRepository;
import com.complain.community.entity.DiscussPost;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    public void saveDiscussPost(DiscussPost post){
        discussPostRepository.save(post);
    }
    public void deleteDiscussPost(int id) {
        discussPostRepository.deleteById(id);
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        NativeSearchQuery searchQuery= new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        // 执行查询
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
        // 返回分页结果
        return new PageImpl<>(
                resultList,
                searchQuery.getPageable(),
                searchHits.getTotalHits()
        );

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
