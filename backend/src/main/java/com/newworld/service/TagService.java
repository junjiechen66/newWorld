package com.newworld.service;

import com.newworld.entity.Tag;

import java.util.List;

public interface TagService {

    /**
     * 获取标签列表
     */
    List<Tag> getList(Long userId);

    /**
     * 新建标签
     */
    Tag create(Tag tag);

    /**
     * 删除标签
     */
    void delete(Long id);
}
