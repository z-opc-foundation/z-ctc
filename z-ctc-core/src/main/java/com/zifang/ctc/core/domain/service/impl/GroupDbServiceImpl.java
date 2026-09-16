package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.GroupDO;
import com.zifang.ctc.core.domain.mapper.GroupMapper;
import com.zifang.ctc.core.domain.service.GroupDbService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * GroupDbService 默认实现：直接转发到 {@link GroupMapper}，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see GroupDbService
 */
@Service
public class GroupDbServiceImpl implements GroupDbService {

    private final GroupMapper groupMapper;

    /**
     * 构造函数。
     *
     * @param groupMapper 分组数据访问对象
     */
    public GroupDbServiceImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    /**
     * 插入分组记录。
     *
     * @param entity 分组实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(GroupDO entity) {
        return groupMapper.insert(entity);
    }

    /**
     * 根据主键更新分组记录。
     *
     * @param entity 分组实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(GroupDO entity) {
        return groupMapper.updateById(entity);
    }

    /**
     * 根据主键删除分组记录。
     *
     * @param id 分组主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return groupMapper.deleteById(id);
    }

    /**
     * 根据租户编码、域编码和分组编码查询分组记录。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param groupCode  分组编码
     * @return 分组实体对象，不存在则返回 null
     */
    @Override
    public GroupDO selectByGroupCode(String tenantCode, String domainCode, String groupCode) {
        return groupMapper.selectByGroupCode(tenantCode, domainCode, groupCode);
    }

    /**
     * 根据租户编码、域编码和部门编码查询分组列表。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param deptCode   部门编码
     * @return 分组实体对象列表
     */
    @Override
    public List<GroupDO> selectByDeptCode(String tenantCode, String domainCode, String deptCode) {
        return groupMapper.selectByDeptCode(tenantCode, domainCode, deptCode);
    }
}
