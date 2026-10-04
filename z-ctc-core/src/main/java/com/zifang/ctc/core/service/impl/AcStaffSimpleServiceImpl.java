package com.zifang.ctc.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.ctc.core.domain.entity.UserDO;
import com.zifang.ctc.core.domain.service.UserDbService;
import com.zifang.ctc.core.service.StaffSimpleService;
import com.zifang.ctc.core.vo.StaffSimpleVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 员工名录服务实现.
 *
 * <p>数据源：{@code z_ctc_ac_user}（只读，<b>不输出 password_hash</b>）。
 *
 * <p><b>为什么不用 membership 过滤</b>：{@code z_ctc_ac_membership} 记录的是
 * 「用户加入了哪个租户」，而 {@code UserDO} 本身没有 tenantCode 维度
 * （本表列：id / user_no / email / phone / username / nickname / avatar /
 * password_hash / status / last_login_at / last_login_ip / created_at / updated_at）。
 * 因此 tenantCode 只能作为<b>调用方上下文</b>透传给消费方，不用于过滤 ——
 * 若强行用它过滤会返回空列表，那正是"接口能通但数据恒空"的一种形态。
 *
 * @author zifang
 * @since 1.0.0
 */
@Service
public class AcStaffSimpleServiceImpl implements StaffSimpleService {

    private static final Logger log = LogManager.getLogger(AcStaffSimpleServiceImpl.class);

    /** 账号状态：1=正常（对齐 UserDO.status 注释） */
    private static final int USER_STATUS_ACTIVE = 1;

    private final UserDbService userDbService;

    public AcStaffSimpleServiceImpl(UserDbService userDbService) {
        this.userDbService = userDbService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffSimpleVO> listSimpleStaff(String tenantCode) {
        List<UserDO> users;
        try {
            users = userDbService.selectByQuery(new QueryWrapper<UserDO>()
                    .eq("status", USER_STATUS_ACTIVE)
                    .orderByAsc("id"));
        } catch (Exception e) {
            log.warn("[AcStaffSimpleServiceImpl] 查询员工名录失败: {}", e.toString());
            return Collections.emptyList();
        }
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        List<StaffSimpleVO> out = new ArrayList<>(users.size());
        for (UserDO u : users) {
            if (u == null || u.getId() == null) {
                continue;
            }
            out.add(convert(u));
        }
        log.info("[AcStaffSimpleServiceImpl] 员工名录: tenant={}, 原始={}, 有效={}",
                tenantCode, users.size(), out.size());
        return out;
    }

    private static StaffSimpleVO convert(UserDO u) {
        StaffSimpleVO vo = new StaffSimpleVO();
        vo.setId(u.getId());
        vo.setJobNumber(u.getUserNo());
        vo.setAccountNo(u.getUsername());
        // 姓名回落：nickname 为空时用 username，避免名录里出现无名条目
        vo.setName(u.getNickname() != null && !u.getNickname().trim().isEmpty()
                ? u.getNickname() : u.getUsername());
        vo.setTelephone(u.getPhone());
        vo.getExtend().put("email", u.getEmail());
        vo.getExtend().put("nickname", u.getNickname());
        vo.getExtend().put("avatar", u.getAvatar());
        vo.getExtend().put("userNo", u.getUserNo());
        // 消费方 parseStaffResponse 读取 extend.flowerName 作为花名；
        // 本仓无花名字段，用 nickname 兜底以免该字段恒空。
        vo.getExtend().put("flowerName", u.getNickname());
        return vo;
    }
}
