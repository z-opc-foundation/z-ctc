package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.RoleResourceDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RoleResourceMapper extends BaseMapper<RoleResourceDO> {

    default List<RoleResourceDO> selectByRoleId(Long roleId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RoleResourceDO>()
                .eq("role_id", roleId));
    }

    @Select("SELECT resource_id FROM z_ctc_spc_role_resource WHERE role_id IN (${roleIds})")
    List<Long> selectResourceIdsByRoleIds(@Param("roleIds") String roleIdsCsv);
}
