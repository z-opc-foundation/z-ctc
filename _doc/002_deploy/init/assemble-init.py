#!/usr/bin/env python3
"""把散在 4 个仓的 z_ctc DDL 拼成一个有序的初始化脚本。

顺序是硬约束:
  1. z-ctc-ac.sql            建 7 张 ac 表
  2. z-ctc-authz.sql         建 4 张 spc 表
  3. z-opc-schema.sql 摘录    建 z_ctc_app_menu / z_ctc_ac_domain / verify_code / surl_mapping
  4. z-ctc-authz-override    建 deny/grant, 并 ALTER z_ctc_app_menu -> 必须在 3 之后
  5. feature049              建 user/membership/invitation, 并 ALTER spc_user_role/ac_account -> 必须在 2 和 1 之后
"""
import os
import re
import sys

# 本脚本同时读 z-ctc 与 z-opc 两个仓:
#   ZCTC_ROOT   = init/ 的上三级 (z-ctc/_doc/002_deploy/init -> z-ctc)
#   ZOPC_ROOT   = ZCTC_ROOT 的同级兄弟仓 z-opc
# 两者都可用环境变量覆盖 (集成分支下两个仓未必并排)。
_HERE = os.path.dirname(os.path.abspath(__file__))
ZCTC_ROOT = os.environ.get("ZCTC_ROOT", os.path.abspath(os.path.join(_HERE, "..", "..", "..")))
ZOPC_ROOT = os.environ.get("ZOPC_ROOT", os.path.join(os.path.dirname(ZCTC_ROOT), "z-opc"))
OUT = sys.argv[1]

WANT_FROM_SCHEMA = [
    "z_ctc_ac_domain",
    "z_ctc_app_menu",
    "z_ctc_verify_code",
    "z_ctc_surl_mapping",
]


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def strip_db(txt):
    """去掉 USE / CREATE DATABASE, 全部落在调用方指定的库里。"""
    txt = re.sub(r"^\s*USE\s+[`\w]+\s*;\s*$", "", txt, flags=re.M | re.I)
    txt = re.sub(r"^\s*CREATE\s+DATABASE[^;]*;\s*$", "", txt, flags=re.M | re.I)
    return txt


def fix_mysql8(txt):
    """MySQL 8 没有 ALTER TABLE ... ADD COLUMN IF NOT EXISTS (那是 MariaDB 语法),
    会报 1064。统一降级成普通 ADD COLUMN —— 这些脚本只在**空库首灌**时跑,
    列必然不存在, 所以去掉 IF NOT EXISTS 是安全的, 而重复执行靠 DROP TABLE 保证。"""
    return re.sub(r"ADD\s+COLUMN\s+IF\s+NOT\s+EXISTS", "ADD COLUMN", txt, flags=re.I)


def make_idempotent(txt):
    """每条裸 CREATE TABLE 前补 DROP TABLE IF EXISTS, 让整脚本可重复执行。"""
    def repl(m):
        name = m.group(2)
        return f"DROP TABLE IF EXISTS `{name}`;\nCREATE TABLE `{name}`"

    return re.sub(
        r"(?i)(CREATE\s+TABLE\s+)(?!IF\s+NOT\s+EXISTS)(?:`)?(\w+)(?:`)?",
        repl,
        txt,
    )


def extract_tables(txt, names):
    """从大 SQL 里摘出指定表的 CREATE TABLE 整块。

    不能用 `index(';')` 找结尾: COMMENT 字符串里常带括号和分号,
    例如 COMMENT '关联权限资源 ID (NULL=无权限控制)' —— 按第一个 ')' 定位会把
    语句截断, 拼出来的脚本在 MySQL 上直接 1064。这里逐字符扫, 跳过
    单引号/反引号/双横线注释里的内容, 只认字符串外的分号。
    """
    out = []
    for n in names:
        m = re.search(
            r"CREATE\s+TABLE\s+(IF\s+NOT\s+EXISTS\s+)?`?" + re.escape(n) + r"`?\s*\(",
            txt,
            flags=re.I,
        )
        if not m:
            raise SystemExit(f"schema 里找不到表 {n}")

        i = m.start()
        in_s = False      # 单引号字符串内
        in_b = in_d = False
        while i < len(txt):
            c = txt[i]
            if in_s:
                if c == "\\":
                    i += 2
                    continue
                if c == "'":
                    in_s = False
            elif in_b:
                if c == "`":
                    in_b = False
            elif c == "'":
                in_s = True
            elif c == "`":
                in_b = True
            elif c == "-" and txt.startswith("--", i):
                j = txt.find("\n", i)
                i = len(txt) if j < 0 else j
                continue
            elif c == ";":
                break
            i += 1
        else:
            raise SystemExit(f"表 {n} 的 CREATE TABLE 没有正常收尾")

        out.append(txt[m.start():i + 1])
    return out


parts = []
parts.append("-- ==== 1. ac 基础表 ====")
parts.append(strip_db(read(f"{ZCTC_ROOT}/_doc/002_deploy/init/z-ctc-ac.sql")))

parts.append("-- ==== 2. spc 权限表 ====")
authz = strip_db(read(f"{ZCTC_ROOT}/_doc/002_deploy/init/z-ctc-authz.sql"))
# authz.sql 落后于实体: UserRoleDO 有 tenant_code, 但 DDL 只建到 granted_by/expire_at。
# feature049 又要按 (user_id, role_id, tenant_code) 加联合唯一键, 于是 1072
# "Key column 'tenant_code' doesn't exist"。直接在 CREATE 里补这一列。
authz = authz.replace(
    "    `expire_at` DATETIME NULL COMMENT '过期时间 (临时授权场景)',",
    "    `tenant_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '租户编码',\n"
    "    `expire_at` DATETIME NULL COMMENT '过期时间 (临时授权场景)',",
    1,
)
assert "tenant_code" in authz, "没能把 tenant_code 插进 z_ctc_spc_user_role"
parts.append(authz)

parts.append("-- ==== 3. z-opc-schema.sql 摘录 ====")
schema = read(f"{ZOPC_ROOT}/_doc/004_sql/z-opc-schema.sql")
parts.append("\n\n".join(extract_tables(schema, WANT_FROM_SCHEMA)))

parts.append("-- ==== 4. spc deny/grant + app_menu 加列 (依赖 3) ====")
parts.append(strip_db(read(f"{ZCTC_ROOT}/_doc/002_deploy/init/z-ctc-authz-override.sql")))

parts.append("-- ==== 5. user/membership/invitation (依赖 1、2) ====")
parts.append(strip_db(read(f"{ZOPC_ROOT}/_doc/004_sql/feature049-membership-migration.sql")))

body = "\n\n".join(parts)
body = fix_mysql8(body)
body = make_idempotent(body)

header = (
    "-- z-ctc 4A 认证中心初始化脚本\n"
    "-- 由 deploy/gen-init-sql.py 从 4 个源文件装配而来, 请勿手改\n"
    "CREATE DATABASE IF NOT EXISTS z_ctc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\n"
    "USE z_ctc;\n\n"
)
with open(OUT, "w", encoding="utf-8") as f:
    f.write(header + body)

created = re.findall(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?", body, flags=re.I)
print(f"wrote {OUT}")
print(f"tables created: {len(created)}")
for t in created:
    print("  ", t)
