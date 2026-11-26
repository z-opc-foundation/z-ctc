package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.ResourceDO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ResourceMapper extends BaseMapper<ResourceDO> {

    /**
     * 按 resource_code + 可选 app_code + 可选 tenant_code 查资源.
     * <p>
     * appCode 不为空时强制加 {@code app_code = #{appCode}} 过滤, 业务侧声明「我要 z-team 范围的」即可消除跨 app 同名歧义.
     *
     * @param resourceCode 资源编码 (必填)
     * @param appCode      应用编码; null/空表示不过滤 (与旧签名语义一致)
     * @param tenantCode   租户编码; null 表示不过滤
     */
    default ResourceDO selectByResourceCode(String resourceCode, String appCode, String tenantCode) {
        return selectOne(new QueryWrapper<ResourceDO>()
                .eq("resource_code", resourceCode)
                .eq(appCode != null && !appCode.isEmpty(), "app_code", appCode)
                .eq(tenantCode != null, "tenant_code", tenantCode)
                .last("LIMIT 1"));
    }

    /**
     * 按 resource_code + 可选 tenant_code 查资源. 多条同名 (例如 GLOBAL_ADMIN 在 z-opc / z-team 各一条) 时由 LIMIT 1 决定命中行, 不保证顺序.
     * <p>
     * 业务侧已知自己所属 app 时, 请改用 {@link #selectByResourceCode(String, String, String)} 带 appCode 过滤,
     * 避免跨 app 同名资源命中歧义 (FEATURE-ZTEAM-AUTH-UNIFY 段 4).
     */
    default ResourceDO selectByResourceCode(String resourceCode, String tenantCode) {
        return selectByResourceCode(resourceCode, null, tenantCode);
    }

    /**
     * FEATURE015: DISTINCT 出已注册的 app_code 列表
     */
    @Select("SELECT DISTINCT app_code FROM z_ctc_spc_resource WHERE app_code IS NOT NULL AND app_code <> '' ORDER BY app_code")
    List<String> selectDistinctApps();
}
