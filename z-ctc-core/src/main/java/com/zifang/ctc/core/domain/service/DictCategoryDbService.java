package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;

import java.util.List;

/**
 * 字典分类数据库服务。
 *
 * @author zifang
 * @since 1.0.8
 */
public interface DictCategoryDbService {

    /**
     * 全局分类。
     *
     * @return 分类列表
     */
    List<DictCategoryDO> selectGlobal();

    /**
     * 指定租户下的分类。
     *
     * @param tenantCode 租户编码
     * @return 分类列表
     */
    List<DictCategoryDO> selectByTenant(String tenantCode);

    /**
     * 全部可见分类。
     *
     * @return 分类列表
     */
    List<DictCategoryDO> selectAll();

    /**
     * 按主键取分类。
     *
     * @param id 主键
     * @return 分类，不存在返回 null
     */
    DictCategoryDO selectById(Long id);

    /**
     * 按作用域取分类。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @param catCode    分类编码
     * @return 分类，不存在返回 null
     */
    DictCategoryDO selectByScope(String tenantCode, String domainCode, String catCode);

    /**
     * 统计子分类数。
     *
     * @param catCode 父分类编码
     * @return 子分类数
     */
    long countChildren(String catCode);

    /**
     * 新增分类。
     *
     * @param entity 分类
     * @return 影响行数
     */
    int insert(DictCategoryDO entity);

    /**
     * 更新分类。
     *
     * @param entity 分类
     * @return 影响行数
     */
    int updateById(DictCategoryDO entity);

    /**
     * 删分类。
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);
}
