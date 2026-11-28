package com.zifang.ctc.core.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.ctc.core.approval.entity.ApprovalTemplateDO;

/** 审批模板 mapper（独立子包：@MapperScan 按包扫描会吃掉接口，服务类不能混进来）。 */
public interface ApprovalTemplateMapper extends BaseMapper<ApprovalTemplateDO> {
}
