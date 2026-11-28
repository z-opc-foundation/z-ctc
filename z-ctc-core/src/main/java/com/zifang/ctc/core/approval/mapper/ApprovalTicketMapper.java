package com.zifang.ctc.core.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.approval.entity.ApprovalTicketDO;

/** 审批票 mapper（独立子包：@MapperScan 按包扫描会吃掉接口，服务类不能混进来）。 */
public interface ApprovalTicketMapper extends BaseMapper<ApprovalTicketDO> {
}
