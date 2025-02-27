package com.complain.community;




import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import com.complain.community.dao.DiscussPostMapper;
import com.complain.community.elasticsearch.DiscussPostRepository;
import com.complain.community.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



import co.elastic.clients.elasticsearch.core.search.HighlightField;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;


@SpringBootTest(classes = CommunityApplication.class)
public class ElasticsearchTests {

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;


    @Test
    public void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    public void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100));
    }

    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人,使劲灌水.");
        discussRepository.save(post);
    }

    @Test
    public void testDelete() {
        // discussRepository.deleteById(231);
        discussRepository.deleteAll();
    }

    @Test
    public void testSearchByRepository() {


    }



}
