# 模块划分

## dic dict-center

## ctc Comprehensive-Tissue-Centre

## zcc zero-code-center

## gwc gate-way-center

## mgc message-center

## z-x

# 4A中心

认证模块：用户输入凭证，验证通过后生成令牌。
账户管理模块：查询用户权限和可访问的应用列表。
授权模块：确定用户对目标应用的访问权限（如是否允许访问）。
会话管理模块：创建全局会话，记录登录状态。
令牌管理模块：向应用系统颁发令牌，用于后续请求验证。
审计模块：记录认证和授权操作日志，供安全审计。# zb-ctc


## 文档目录

本项目文档统一收口在 `_doc/` 下:

- [`_doc/001_arch/`](_doc/001_arch/) — 架构文档 (项目总览 / 模块结构 / 接口清单 / DB schema / 前端 / 能力 / roadmap):
  - [`00-overview.md`](_doc/001_arch/00-overview.md)
  - [`01-module-structure-2.md`](_doc/001_arch/01-module-structure-2.md)
  - [`01-module-structure.md`](_doc/001_arch/01-module-structure.md)
  - [`02-api.md`](_doc/001_arch/02-api.md)
  - [`03-db-schema.md`](_doc/001_arch/03-db-schema.md)
  - [`04-4a-migration.md`](_doc/001_arch/04-4a-migration.md)
  - [`05-frontend.md`](_doc/001_arch/05-frontend.md)
  - [`07-roadmap.md`](_doc/001_arch/07-roadmap.md)

- [`_doc/003_script/`](_doc/003_script/) — 运维脚本:
  - [`build.sh`](_doc/003_script/build.sh)
  - [`deploy_maven_center.sh`](_doc/003_script/deploy_maven_center.sh)
  - [`docker-entrypoint.sh`](_doc/003_script/docker-entrypoint.sh)
  - [`install-settings.sh`](_doc/003_script/install-settings.sh)
  - [`package.sh`](_doc/003_script/package.sh)

各文档详细说明见各子目录。
