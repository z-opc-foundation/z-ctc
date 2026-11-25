package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.DictCategoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典分类 Mapper。
 *
 * @author zifang
 * @since 1.0.8
 */
@Mapper
public interface DictCategoryMapper extends BaseMapper<DictCategoryDO> {

    /**
     * 全局分类（租户为空）。
     *
     * @return 全局分类列表
     */
    @Select("SELECT * FROM z_ctc_dict_category WHERE tenant_code = '' ORDER BY sort_order, id ASC")
    List<DictCategoryDO> selectGlobal();

    /**
     * 指定租户下的分类。
     *
     * @param tenantCode 租户编码
     * @return 分类列表
     */
    @Select("SELECT * FROM z_ctc_dict_category WHERE tenant_code = #{tenantCode} ORDER BY sort_order, id ASC")
    List<DictCategoryDO> selectByTenant(@Param("tenantCode") String tenantCode);

    /**
     * 全部可见分类（全局 + 租户）。
     *
     * @return 分类列表
     */
    default List<DictCategoryDO> selectAll() {
        return selectGlobal();
    }

    /**
     * 按作用域取单个分类。
     *
     * ⚠ 不能用 selectOne：库里可能存在同 (catCode, tenant) 的重复行
     * （2026-11-25 实测：前端提示崩溃期间用户连点 21 次，建出 21 行同编码分类）
     * ⇒ selectOne 直接抛 TooManyResultsException ⇒ 条目列表 500。
     * 这里改为「取 id 最小的一行」，重复行由数据清理/查重兜底，不让读路径炸。
     *
     * @param tenantCode 租户编码（空=全局）
     * @param domainCode 域编码
     * @param catCode    分类编码
     * @return 分类，不存在返回 null
     */
    default DictCategoryDO selectByScope(String tenantCode, String domainCode, String catCode) {
        LambdaQueryWrapper<DictCategoryDO> qw = new LambdaQueryWrapper<DictCategoryDO>()
                .eq(DictCategoryDO::getCatCode, catCode)
                .and(w -> {
                    if (tenantCode == null || tenantCode.isEmpty()) {
                        w.eq(DictCategoryDO::getTenantCode, "");
                    } else {
                        w.eq(DictCategoryDO::getTenantCode, tenantCode);
                    }
                })
                .orderByAsc(DictCategoryDO::getId)
                .last("LIMIT 1");
        List<DictCategoryDO> rows = selectList(qw);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 统计子分类数。
     *
     * @param catCode 父分类编码
     * @return 子分类数
     */
    @Select("SELECT COUNT(*) FROM z_ctc_dict_category WHERE parent_code = #{catCode}")
    long countChildren(@Param("catCode") String catCode);
}
