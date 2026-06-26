package com.newworld.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.newworld.entity.ShareMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ShareMemberMapper extends BaseMapper<ShareMember> {

    /**
     * 查询某资源的共享成员列表（含用户昵称/头像）
     */
    List<Map<String, Object>> selectMembersWithUser(@Param("resourceType") String resourceType,
                                                     @Param("resourceId") Long resourceId);

    /**
     * 查询"我的 + 被共享的"任务列表
     */
    List<Map<String, Object>> selectTasksWithShare(@Param("userId") Long userId,
                                                    @Param("isNote") Boolean isNote,
                                                    @Param("projectId") Long projectId,
                                                    @Param("status") String status,
                                                    @Param("priority") String priority,
                                                    @Param("keyword") String keyword);
}
