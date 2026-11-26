package com.zifang.ctc.core.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.domain.entity.MenuDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 应用菜单 Mapper.
 * <p>
 * 表: z_ctc_app_menu.
 */
public interface MenuMapper extends BaseMapper<MenuDO> {

    /**
     * 拉取指定 app_code 下的所有菜单行 (扁平), 按 sort_order, id 排序.
     */
    @Select("SELECT * FROM z_ctc_app_menu "
            + "WHERE app_code = #{appCode} AND status = 1 "
            + "ORDER BY sort_order ASC, id ASC")
    List<MenuDO> selectByAppCode(@Param("appCode") String appCode);

    /**
     * 按 menu_code 查 (用于判重 / 更新).
     */
    default MenuDO selectByMenuCode(String appCode, String menuCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MenuDO>()
                .eq("app_code", appCode)
                .eq("menu_code", menuCode)
                .last("LIMIT 1"));
    }
}
