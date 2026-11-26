package com.zifang.ctc.core.domain.service;

import com.zifang.ctc.core.domain.entity.MenuDO;

import java.util.Collection;
import java.util.List;

/**
 * 应用菜单 DbService.
 */
public interface MenuDbService {

    int insert(MenuDO entity);

    int updateById(MenuDO entity);

    int deleteById(Long id);

    int deleteBatchIds(Collection<Long> ids);

    MenuDO selectById(Long id);

    MenuDO selectByMenuCode(String appCode, String menuCode);

    List<MenuDO> selectByAppCode(String appCode);
}
