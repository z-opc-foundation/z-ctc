package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.DictEntryDO;

import java.util.List;

/**
 * 字典条目数据库服务。
 *
 * @author zifang
 * @since 1.0.8
 */
public interface DictEntryDbService {

    /**
     * 某分类下的条目。
     *
     * @param category 分类编码
     * @return 条目列表
     */
    List<DictEntryDO> selectByCategory(String category);

    /**
     * 可见条目。
     *
     * @param tenantCode 租户编码
     * @return 条目列表
     */
    List<DictEntryDO> selectVisible(String tenantCode);

    /**
     * 按主键取条目。
     *
     * @param id 主键
     * @return 条目，不存在返回 null
     */
    DictEntryDO selectById(Long id);

    /**
     * 按作用域取条目。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param category   分类编码
     * @param dictKey    条目键
     * @return 条目，不存在返回 null
     */
    DictEntryDO selectByScope(String tenantCode, String domainCode, String category, String dictKey);

    /**
     * 统计分类下条目数。
     *
     * @param category 分类编码
     * @return 条目数
     */
    long countByCategory(String category);

    /**
     * 新增条目。
     *
     * @param entity 条目
     * @return 影响行数
     */
    int insert(DictEntryDO entity);

    /**
     * 更新条目。
     *
     * @param entity 条目
     * @return 影响行数
     */
    int updateById(DictEntryDO entity);

    /**
     * 删条目。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
