package com.complain.community.dao;

import com.complain.community.entity.Comment;

import java.util.List;

public interface CommentMapper {
    List<Comment> selectCommentsByEntity(int entityType,int entityId,int offset,int limit);
    int selectCountByEntity(int entityType,int entityId);

    int insertComment(Comment comment);

}
