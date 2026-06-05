package com.newworld.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.newworld.entity.Tag;
import com.newworld.mapper.TagMapper;
import com.newworld.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Override
    public List<Tag> getList(Long userId) {
        return tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().eq(Tag::getUserId, userId));
    }

    @Override
    public Tag create(Tag tag) {
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public void delete(Long id) {
        tagMapper.deleteById(id);
    }
}
