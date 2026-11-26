package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.DomainDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 域 Mapper.
 */
public interface DomainMapper extends BaseMapper<DomainDO> {

    /**
     * 拉取租户下的所有域, 按 sort_order, id 排序.
     */
    @Select("SELECT * FROM z_ctc_ac_domain WHERE tenant_code = #{tenantCode} AND status = 1 ORDER BY id ASC")
    List<DomainDO> selectByTenant(@Param("tenantCode") String tenantCode);

    /**
     * 全量无分页 (管理页用).
     */
    @Select("SELECT * FROM z_ctc_ac_domain ORDER BY tenant_code, id ASC")
    List<DomainDO> selectAll();

    default DomainDO selectByDomainCode(String tenantCode, String domainCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DomainDO>()
                .eq("tenant_code", tenantCode)
                .eq("domain_code", domainCode)
                .last("LIMIT 1"));
    }
}
