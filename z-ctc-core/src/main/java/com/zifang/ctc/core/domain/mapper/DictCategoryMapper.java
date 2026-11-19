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
                });
        return selectOne(qw);
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
