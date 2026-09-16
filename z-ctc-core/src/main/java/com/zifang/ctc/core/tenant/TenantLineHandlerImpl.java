package com.zifang.ctc.core.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.Column;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * 租户隔离 Handler (FEATURE049).
 * <p>
 * 给 MyBatis-Plus 的 TenantLineInnerInterceptor 用, 实现:
 * 1. 拿当前请求的 tenant_code (从 {@link TenantContext})
 * 2. SQL 自动追加 {@code WHERE tenant_code = ?}
 * 3. INSERT 自动注入 {@code tenant_code = ?}
 * 4. 游离态 / 无租户上下文时, 跳过 (返回 false), 业务层需自行校验
 * 5. 例外表 (user / user_role / 邀请 / 登录日志 等全局表) 不参与租户隔离
 *
 * <h3>例外表</h3>
 * <ul>
 *   <li>z_ctc_ac_user — 全局身份表, 一个真实的人 = 一条记录, 不绑租户</li>
 *   <li>z_ctc_ac_auth — 凭证, 跟 user 走</li>
 *   <li>z_ctc_ac_login_log — 登录日志, 跟 user 走</li>
 *   <li>z_ctc_ac_membership — 本身就是租户关系的载体, 不再二次隔离</li>
 *   <li>z_ctc_ac_invitation — 邀请, 按 tenant_code 查 (允许)</li>
 *   <li>z_ctc_spc_user_role — 角色绑定表, 已经有 tenant_code 列, 不需要额外过滤</li>
 *   <li>z_ctc_ac_account — 兼容期, 暂不强制隔离</li>
 * </ul>
 */
public class TenantLineHandlerImpl implements TenantLineHandler {

    private static final Logger log = LogManager.getLogger(TenantLineHandlerImpl.class);

    /**
     * 不参与租户隔离的表名前缀 (即: 这些表 SQL 不追加 tenant_code 条件)
     */
    private static final List<String> IGNORE_TABLE_PREFIXES = Arrays.asList(
            "z_ctc_ac_tenant",          // 租户主表, 自身不参与租户隔离 (避免 INSERT 时 tenant_code 重复)
            "z_ctc_ac_domain",          // 域表, 实体已有 tenant_code 字段, 不再二次注入
            "z_ctc_ac_org",             // 组织表, 实体已有 tenant_code 字段
            "z_ctc_ac_membership",      // 成员关系表, 实体已有 tenant_code 字段
            "z_ctc_ac_user",
            "z_ctc_ac_auth",
            "z_ctc_ac_login_log",
            "z_ctc_ac_account",         // 兼容期, 标记 deprecated
            "z_ctc_spc_user_role",      // 已有 tenant_code 列, 不再二次隔离
            "z_ctc_spc_role",
            "z_ctc_spc_resource",
            "z_ctc_spc_role_resource"
    );

    /**
     * 租户列名 (F037 统一迁移到 tenant_code)
     */
    private static final String TENANT_COLUMN = "tenant_code";

    @Override
    public Expression getTenantId() {
        String tenantCode = TenantContext.getTenantCode();
        if (tenantCode == null || tenantCode.isEmpty()) {
            // 游离态 / 无上下文: 用占位值, 但 isIgnoreTable 会先判断, 实际不会执行
            log.debug("TenantLineHandlerImpl.getTenantId: 当前无 tenant 上下文 (游离态?), 跳过过滤");
            return new StringValue("__ORPHAN__");
        }
        return new StringValue(tenantCode);
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (tableName == null) { return true; }

        // 兼容 mapper 传入带表名前缀的情况
        String lower = tableName.toLowerCase();
        for (String prefix : IGNORE_TABLE_PREFIXES) {
            if (lower.contains(prefix.toLowerCase())) {
                return true;  // 忽略 (不追加 tenant_code)
            }
        }
        // 游离态: 业务表都忽略, 避免误过滤
        if (TenantContext.isOrphan()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean ignoreInsert(List<Column> columns, String tenantIdColumn) {
        // INSERT 行为: 例外表不注入, 其他表自动注入 tenant_code
        // (与 ignoreTable 协调, 例外表直接跳过)
        return false;
    }
}
