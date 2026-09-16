package com.zifang.ctc.core.domain.service.impl;

import com.zifang.ctc.core.domain.entity.MenuDO;
import com.zifang.ctc.core.domain.mapper.MenuMapper;
import com.zifang.ctc.core.domain.service.MenuDbService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * MenuDbService 默认实现：直接转发到 {@link MenuMapper}，不加任何业务语义。
 *
 * @author zifang
 * @since 1.0.0
 * @see MenuDbService
 */
@Service
public class MenuDbServiceImpl implements MenuDbService {

    private final MenuMapper menuMapper;

    /**
     * 构造函数。
     *
     * @param menuMapper 菜单数据访问对象
     */
    public MenuDbServiceImpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 插入菜单记录。
     *
     * @param entity 菜单实体对象
     * @return 插入成功的记录数
     */
    @Override
    public int insert(MenuDO entity) {
        return menuMapper.insert(entity);
    }

    /**
     * 根据主键更新菜单记录。
     *
     * @param entity 菜单实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    public int updateById(MenuDO entity) {
        return menuMapper.updateById(entity);
    }

    /**
     * 根据主键删除菜单记录。
     *
     * @param id 菜单主键
     * @return 删除成功的记录数
     */
    @Override
    public int deleteById(Long id) {
        return menuMapper.deleteById(id);
    }

    /**
     * 批量根据主键删除菜单记录。
     *
     * @param ids 菜单主键列表
     * @return 删除成功的记录数
     */
    @Override
    public int deleteBatchIds(Collection<Long> ids) {
        return menuMapper.deleteBatchIds(ids);
    }

    /**
     * 根据主键查询菜单记录。
     *
     * @param id 菜单主键
     * @return 菜单实体对象，不存在则返回 null
     */
    @Override
    public MenuDO selectById(Long id) {
        return menuMapper.selectById(id);
    }

    /**
     * 根据应用编码和菜单编码查询菜单记录。
     *
     * @param appCode  应用编码
     * @param menuCode 菜单编码
     * @return 菜单实体对象，不存在则返回 null
     */
    @Override
    public MenuDO selectByMenuCode(String appCode, String menuCode) {
        return menuMapper.selectByMenuCode(appCode, menuCode);
    }

    /**
     * 根据应用编码查询菜单列表。
     *
     * @param appCode 应用编码
     * @return 菜单实体对象列表
     */
    @Override
    public List<MenuDO> selectByAppCode(String appCode) {
        return menuMapper.selectByAppCode(appCode);
    }
}
