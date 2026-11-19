package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.DictEntryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字典条目 Mapper。
 *
 * @author zifang
 * @since 1.0.8
 */
@Mapper
public interface DictEntryMapper extends BaseMapper<DictEntryDO> {

    /**
     * 某分类下的条目。
     *
     * @param category 分类编码
     * @return 条目列表
     */
    @Select("SELECT * FROM z_ctc_dictionary WHERE category = #{category} ORDER BY sort_order, id ASC")
    List<DictEntryDO> selectByCategory(@Param("category") String category);

    /**
     * 可见条目（全局 + 指定租户）。
     *
     * @param tenantCode 租户编码
     * @return 条目列表
     */
    @Select("SELECT * FROM z_ctc_dictionary WHERE tenant_code = '' OR tenant_code = #{tenantCode} "
            + "ORDER BY category, sort_order, id ASC")
    List<DictEntryDO> selectVisible(@Param("tenantCode") String tenantCode);

    /**
     * 统计分类下条目数。
     *
     * @param category 分类编码
     * @return 条目数
     */
    @Select("SELECT COUNT(*) FROM z_ctc_dictionary WHERE category = #{category}")
    long countByCategory(@Param("category") String category);

    /**
     * 按作用域取单个条目。
     *
     * @param tenantCode 租户编码（空=全局）
     * @param domainCode 域编码
     * @param category   分类编码
     * @param dictKey    条目键
     * @return 条目，不存在返回 null
     */
    default DictEntryDO selectByScope(String tenantCode, String domainCode, String category, String dictKey) {
        LambdaQueryWrapper<DictEntryDO> qw = new LambdaQueryWrapper<DictEntryDO>()
                .eq(DictEntryDO::getCategory, category)
                .eq(DictEntryDO::getDictKey, dictKey)
                .and(w -> {
                    if (tenantCode == null || tenantCode.isEmpty()) {
                        w.eq(DictEntryDO::getTenantCode, "");
                    } else {
                        w.eq(DictEntryDO::getTenantCode, tenantCode);
                    }
                });
        return selectOne(qw);
    }
}
