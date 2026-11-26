package com.zifang.ctc.core.service;

import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import com.zifang.ctc.core.domain.entity.DictEntryDO;

import java.util.List;
import java.util.Map;

/**
 * 灵活字典服务：分类与条目的统一出入口。
 *
 * <p>作用域三级：GLOBAL（全局）/ TENANT（租户）/ DOMAIN（域）。
 * 分类可成树（parent_code），条目按分类的 entry_shape 决定 flat 还是 tree。</p>
 *
 * @author zifang
 * @since 1.0.8
 */
public interface DictService {

    /**
     * 可见分类列表（全局 + 当前租户）。
     *
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 分类列表
     */
    List<DictCategoryDO> listVisibleCategories(String tenantCode, String domainCode);

    /**
     * 按主键取分类。
     *
     * @param id 主键
     * @return 分类，不存在返回 null
     */
    DictCategoryDO findCategoryById(Long id);

    /**
     * 新建分类。
     *
     * @param category 分类
     * @param operator 操作人
     * @return 新分类主键
     */
    Long createCategory(DictCategoryDO category, String operator);

    /**
     * 改分类（打补丁语义，null 字段不覆盖）。
     *
     * @param id       分类主键
     * @param patch    补丁
     * @param operator 操作人
     * @return 是否改动
     */
    boolean updateCategory(Long id, DictCategoryDO patch, String operator);

    /**
     * 删分类。
     *
     * @param id 分类主键
     * @return 是否删除
     */
    boolean deleteCategory(Long id);

    /**
     * 列条目。
     *
     * @param category   分类编码
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 条目列表
     */
    List<DictEntryDO> listEntries(String category, String tenantCode, String domainCode);

    /**
     * 按主键取条目。
     *
     * @param id 主键
     * @return 条目，不存在返回 null
     */
    DictEntryDO findEntryById(Long id);

    /**
     * 新建条目。
     *
     * @param entry    条目
     * @param operator 操作人
     * @return 新条目主键
     */
    Long createEntry(DictEntryDO entry, String operator);

    /**
     * 改条目（打补丁语义）。
     *
     * @param id       条目主键
     * @param patch    补丁
     * @param operator 操作人
     * @return 是否改动
     */
    boolean updateEntry(Long id, DictEntryDO patch, String operator);

    /**
     * 删条目。
     *
     * @param id 条目主键
     * @return 是否删除
     */
    boolean deleteEntry(Long id);

    /**
     * 按分类 + 键查条目（只返回当前生效的）。
     *
     * @param category   分类编码
     * @param key        条目键
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 条目，不存在或未生效返回 null
     */
    DictEntryDO lookup(String category, String key, String tenantCode, String domainCode);

    /**
     * 批量查多个分类的条目。
     *
     * @param categories 分类编码列表
     * @param tenantCode 租户编码
     * @param domainCode 域编码
     * @return 分类编码 → 条目列表
     */
    Map<String, List<DictEntryDO>> resolve(List<String> categories, String tenantCode, String domainCode);
}
