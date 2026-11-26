package com.zifang.ctc.core.domain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.InvitationDO;
import com.zifang.ctc.core.domain.mapper.InvitationMapper;
import com.zifang.ctc.core.domain.service.InvitationDbService;
import com.zifang.z.meta.api.annotation.BizMethod;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * InvitationDbService 默认实现：转发到 {@link InvitationMapper}。
 *
 * @author zifang
 * @since 1.0.0
 * @see InvitationDbService
 */
@Service
public class InvitationDbServiceImpl implements InvitationDbService {

    private final InvitationMapper invitationMapper;

    /**
     * 构造函数。
     *
     * @param invitationMapper 邀请数据访问对象
     */
    public InvitationDbServiceImpl(InvitationMapper invitationMapper) {
        this.invitationMapper = invitationMapper;
    }

    /**
     * 插入邀请记录。
     *
     * @param entity 邀请实体对象
     * @return 插入成功的记录数
     */
    @Override
    @BizMethod(name = "插入邀请记录", module = "z-ctc", description = "插入邀请记录。", operation = BizMethod.Operation.CREATE)
    public int insert(InvitationDO entity) {
        return invitationMapper.insert(entity);
    }

    /**
     * 根据主键更新邀请记录。
     *
     * @param entity 邀请实体对象（必须包含主键）
     * @return 更新成功的记录数
     */
    @Override
    @BizMethod(name = "根据主键更新邀请记录", module = "z-ctc", description = "根据主键更新邀请记录。", operation = BizMethod.Operation.UPDATE)
    public int updateById(InvitationDO entity) {
        return invitationMapper.updateById(entity);
    }

    /**
     * 根据主键删除邀请记录。
     *
     * @param id 邀请主键
     * @return 删除成功的记录数
     */
    @Override
    @BizMethod(name = "根据主键删除邀请记录", module = "z-ctc", description = "根据主键删除邀请记录。", operation = BizMethod.Operation.DELETE)
    public int deleteById(Long id) {
        return invitationMapper.deleteById(id);
    }

    /**
     * 根据主键查询邀请记录。
     *
     * @param id 邀请主键
     * @return 邀请实体对象，不存在则返回 null
     */
    @Override
    @BizMethod(name = "根据主键查询邀请记录", module = "z-ctc", description = "根据主键查询邀请记录。", operation = BizMethod.Operation.QUERY)
    public InvitationDO selectById(Long id) {
        return invitationMapper.selectById(id);
    }

    /**
     * 根据令牌查询邀请记录。
     *
     * @param token 令牌
     * @return 邀请实体对象，不存在则返回 null
     */
    @Override
    @BizMethod(name = "根据令牌查询邀请记录", module = "z-ctc", description = "根据令牌查询邀请记录。", operation = BizMethod.Operation.QUERY)
    public InvitationDO selectByToken(String token) {
        return invitationMapper.selectByToken(token);
    }

    /**
     * 根据邀请码查询邀请记录。
     *
     * @param code 邀请码
     * @return 邀请实体对象，不存在则返回 null
     */
    @Override
    @BizMethod(name = "根据邀请码查询邀请记录", module = "z-ctc", description = "根据邀请码查询邀请记录。", operation = BizMethod.Operation.QUERY)
    public InvitationDO selectByInviteCode(String code) {
        return invitationMapper.selectByInviteCode(code);
    }

    /**
     * 根据租户编码查询邀请列表。
     *
     * @param tenantCode 租户编码
     * @return 邀请实体对象列表
     */
    @Override
    @BizMethod(name = "根据租户编码查询邀请列表", module = "z-ctc", description = "根据租户编码查询邀请列表。", operation = BizMethod.Operation.QUERY)
    public List<InvitationDO> selectByTenant(String tenantCode) {
        return invitationMapper.selectByTenant(tenantCode);
    }

    /**
     * 根据被邀请人邮箱查询邀请列表。
     *
     * @param email 邮箱地址
     * @return 邀请实体对象列表
     */
    @Override
    @BizMethod(name = "根据被邀请人邮箱查询邀请列表", module = "z-ctc", description = "根据被邀请人邮箱查询邀请列表。", operation = BizMethod.Operation.QUERY)
    public List<InvitationDO> selectByInviteeEmail(String email) {
        return invitationMapper.selectByInviteeEmail(email);
    }

    /**
     * 根据查询条件查询邀请列表。
     *
     * @param wrapper 查询条件包装器
     * @return 邀请实体对象列表
     */
    @Override
    @BizMethod(name = "根据查询条件查询邀请列表", module = "z-ctc", description = "根据查询条件查询邀请列表。", operation = BizMethod.Operation.QUERY)
    public List<InvitationDO> selectByQuery(QueryWrapper<InvitationDO> wrapper) {
        return invitationMapper.selectList(wrapper);
    }
}



