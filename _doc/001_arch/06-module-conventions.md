# z-ctc 模块规范

## 1. 模块结构

```
z-ctc/
├── z-ctc-core/                      # 核心业务（Domain 层）
│   └── src/main/java/com/zifang/ctc/core/
│       ├── domain/
│       │   ├── entity/              ← 数据库实体（MyBatis-Plus @TableName）
│       │   ├── mapper/              ← Mapper 接口（继承 BaseMapper<Entity>）
│       │   └── service/             ← I*Service 接口 + impl/ 实现
│       └── service/
│           ├── dto/                  ← 数据传输对象（进程内流转）
│           │   └── converter/        ← DTO ↔ Entity 转换器（可选）
│           └── model/
│               └── request/         ← 业务请求（带分页/筛选字段）
├── z-ctc-web/                       # Web 层（Controller 层）
│   └── src/main/java/com/zifang/z/ctc/web/api/
│       ├── request/                 ← *Req（Controller 接收参数）
│       └── response/                ← *Resp（Controller 返回对象）
├── z-ctc-admin/                     # 管理后台
├── z-ctc-sdk/                      # SDK
├── z-ctc-sso/                      # SSO 客户端（TokenService.verifyToken）
├── z-ctc-audit-starter/            # 审计客户端（其他应用引入）
│   ├── Audit.java                  # @Audit 注解，标记住方法
│   ├── EnableAudit.java            # @EnableAudit 启用开关
│   ├── AuditEvent.java             # 事件 POJO
│   ├── AuditClient.java            # HTTP 异步上报到 CTC
│   ├── AuditAspect.java            # AOP 拦截 @Audit 方法
│   └── AuditAutoConfiguration      # Spring Boot 自动配置
└── bootstrap/                      # 启动器
```

## 2. 分层规范

```
Controller  ──(toDto/toResp)──►  BizService  ──►  DomainService  ──►  Mapper
       │                                │
       ▼                                ▼
   web/api/request/              service/dto/
   web/api/response/            domain/entity/
```

**关键原则**：

- Entity（DO）不出 Service 层
- Controller 只做参数转换，不写业务逻辑
- DomainService 处理纯领域逻辑（校验、状态机）
- BizService 编排 DomainService，处理跨领域逻辑
- 方法签名直接用具体请求类型（`UserPageReq`），**不用基类 PageRequest + instanceof**

## 3. Controller 规范

### 3.1 路由规范（仅 GET / POST）

```
GET  /api/xxx/page      → 分页查询（body 接收分页参数）
GET  /api/xxx/{id}      → 详情（路径参数）
POST /api/xxx           → 新增
POST /api/xxx/update    → 更新（body 完整对象）
POST /api/xxx/{id}/delete → 删除（id 在路径）
```

**禁止使用 PUT / DELETE / PATCH**

### 3.2 注解规范

```java
@Tag(name = "模块名称")
@RestController
@RequestMapping("/api/xxx")
public class XxxController {

    @Operation(summary = "001_分页查询")
    @PostMapping("/page")
    public IPage<XxxResp> page(@RequestBody XxxReq req) {
        XxxDTO dto = toDto(req);
        return xxxBizService.page(dto, req.getCurrent(), req.getPageSize())
                .convert(this::toResp);
    }
}
```

### 3.3 参数转换

Controller 提供私有方法做 DTO 转换，**不做 Converter 类膨胀**：

```java
private XxxDTO toDto(XxxReq req) {
    XxxDTO dto = new XxxDTO();
    BeanUtils.copyProperties(req, dto);
    return dto;
}

private XxxResp toResp(Xxx entity) {
    XxxResp resp = new XxxResp();
    BeanUtils.copyProperties(entity, resp);
    return resp;
}
```

## 4. Service 规范

### 4.1 BizService 接口

位置：`z-ctc-core/src/main/java/com/zifang/ctc/core/service/*BizService.java`

```java
public interface AccountBizService {
    IPage<AccountDTO> page(AccountDTO query, int pageNum, int pageSize);
    AccountDTO getById(Long id);
    void save(AccountDTO dto);
    void update(AccountDTO dto);
    void delete(Long id);
}
```

### 4.2 方法签名原则

- **直接用具体请求类型**，不抽象基类
- `page()` 方法签名：`IPage<XxxDTO> page(XxxDTO query, int pageNum, int pageSize)`
- **禁止 `instanceof` 判断请求类型**

## 5. 审计模块（z-ctc-audit-starter）

### 5.1 架构

```
其他应用（如 z-task）
  pom.xml 引入 z-ctc-audit-starter
  启动类加 @EnableAudit + @EnableAsync
  Controller 方法加 @Audit("操作描述")

z-ctc-audit-starter（client 端，AOP + HTTP 上报）
  AuditAspect  → 拦截 @Audit 方法，采集数据
  AuditClient  → 异步 HTTP POST JSON 到 CTC

z-ctc（CTC 服务端）
  AuditManagerController
    POST /api/audit/event  ← 接收上报事件
    GET  /api/audit/log    ← 分页查询
    GET  /api/audit/export ← 导出 CSV
  AuditBizService → 写入 AuditLog 表
```

### 5.2 接入方式

```xml
<dependency>
    <groupId>com.zifang</groupId>
    <artifactId>z-ctc-audit-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

```yaml
audit:
  enabled: true
  application: z-task          # 应用名，必填
  ctc:
    url: http://ctc-service:8080 # CTC 服务地址
```

```java
@SpringBootApplication
@EnableAsync          // 必须，否则异步上报不生效
@EnableAudit           // 开启审计拦截
public class ZTaskApplication {}
```

```java
@Audit("创建任务")
@PostMapping("/api/task")
public Long create(@RequestBody @Valid TaskReq req) {
    return taskBizService.create(req);
}
```

### 5.3 审计事件字段

| 字段                | 说明                           |
|-------------------|------------------------------|
| traceId           | 链路追踪 ID                      |
| application       | 调用方应用名                       |
| operationType     | 操作类型（如 CREATE/UPDATE/DELETE） |
| operationDesc     | 操作描述（@Audit 注解值）             |
| userId / userName | 当前操作用户                       |
| ip / url / method | 请求 IP、URL、HTTP 方法            |
| params            | 请求参数（JSON）                   |
| duration          | 执行耗时（ms）                     |
| status            | 状态（1成功/0失败）                  |
| errorMsg          | 异常信息                         |

## 6. 已有功能清单

### 6.1 AccountManagerController（账号管理）

| 方法      | 路径                            | 说明   |
|---------|-------------------------------|------|
| page    | POST /api/account/page        | 分页查询 |
| getById | GET /api/account/{id}         | 详情   |
| create  | POST /api/account             | 新增   |
| update  | POST /api/account/{id}/update | 更新   |
| delete  | POST /api/account/{id}/delete | 删除   |

### 6.2 PermissionManagerController（权限管理）

| 方法      | 路径                               | 说明   |
|---------|----------------------------------|------|
| page    | POST /api/permission/page        | 分页查询 |
| getById | GET /api/permission/{id}         | 详情   |
| create  | POST /api/permission             | 新增   |
| update  | POST /api/permission/{id}/update | 更新   |
| delete  | POST /api/permission/{id}/delete | 删除   |

### 6.3 OrgManagerController / DeptManagerController / GroupManagerController

同上 CRUD + page 结构。

### 6.4 AuditManagerController（审计日志）

| 方法     | 路径                    | 说明     |
|--------|-----------------------|--------|
| page   | GET /api/audit/log    | 分页查询   |
| export | GET /api/audit/export | 导出 CSV |

## 7. 数据库表

| 表名               | 说明                                               |
|------------------|--------------------------------------------------|
| z_ctc_account    | 账号表                                              |
| z_ctc_permission | 权限表（含 parent_id 树形结构，perm_type: MENU/BUTTON/API） |
| z_ctc_role       | 角色表                                              |
| z_ctc_audit_log  | 审计日志表                                            |
| z_ctc_org        | 组织表                                              |
| z_ctc_dept       | 部门表                                              |
| z_ctc_group      | 组表                                               |
| z_ctc_tenant     | 租户表                                              |
| z_ctc_domain     | 域表                                               |

**注意**：权限表支持树形（parent_id），perm_type = MENU 时可做菜单树，perm_type = BUTTON 时可做按钮级权限。
