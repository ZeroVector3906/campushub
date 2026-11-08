# 西安欧亚学院征途单车俱乐部&社团 · 综合管理网站

服务于西安欧亚学院征途单车俱乐部的完整应用，实际连接 PostgreSQL，使用 **Java 21、Spring Boot 3.5、Spring Data JPA、Spring Security、Vue 3、Vite、Element Plus**。前端构建后打包进同一个 Spring Boot JAR，电脑、手机使用同一个站点。

## 立即访问

公网网站：[征途单车俱乐部](https://campushub-2ar9.onrender.com/#/)；[俱乐部介绍](https://campushub-2ar9.onrender.com/#/clubs)；[管理员后台](https://campushub-2ar9.onrender.com/#/admin)。部署于 Render，数据库运行于 Neon，不依赖本机开机。当前版本与实际验证范围见 [征途专属改版说明](docs/征途专属改版.md)，简介内容取舍见 [简介内容整理](docs/简介内容整理.md)。免费实例空闲后可能休眠，再次打开需要等待冷启动。

本机预览地址：[http://localhost:8080/#/](http://localhost:8080/#/)。这是本机地址，只在运行网站的电脑上可用。

本机预览若出现“拒绝连接 / ERR_CONNECTION_REFUSED”，请双击项目根目录的 `CampusHub.cmd`，它会启动本项目的数据库和网站，成功后自动打开浏览器。已有构建无需重新安装依赖；关机后需要再次启动。自行部署步骤见 [部署教程](docs/部署教程.md)。

最终交付目录为 `release/`：`CampusHub-source.zip` 是完整源码包，`CampusHub-1.0.0.jar` 是包含前端的可运行包，`verification.json` 和 `测试摘要.txt` 记录本次实际执行结果。源码包不包含体积较大的独立运行环境或数据库文件；新电脑先按下方步骤准备环境。

| 身份 | 用户名 | 演示密码 |
| --- | --- | --- |
| 学生 | `student` | `CampusHub@2026` |
| 管理员 | `admin` | `CampusHub@2026` |

本地演示账号仅由 `SEED_DEMO=true` 创建。正式部署默认关闭演示模式，首次启动通过 `ADMIN_PASSWORD` 设置管理员密码（至少 12 位）。已存在的账号不会因重启而重置密码。

以上演示账号仅适用于本机。公网管理员为 `admin`，首次部署生成独立随机密码，初始密码保存在部署者本机的 `.runtime/cloud-admin-password.txt`，不进入 Git 或源码包；手动修改后应使用自己设置的新密码。普通用户请在公网注册并选择俱乐部或社团。V2 迁移将原通用平台的示例社团、活动和器材归档，历史记录保留；V6 增加两类成员身份，不重置账号。正式活动、器材库存和联系渠道由管理员核实后录入。当前行为与验收见 [俱乐部与社团身份管理](docs/俱乐部与社团身份.md)。

## 功能

2026-11-08 增加俱乐部 / 社团注册选择、首页权益责任对比及管理员成员资料编辑，移除成员中心的「我的俱乐部」。场地申请按审核后的身份限制，器材申请权限保持原流程不变。旧账号待管理员确认类别，密码和历史记录保留。详见 [俱乐部与社团身份管理](docs/俱乐部与社团身份.md)。

2026-11-08 新增：[活动建议](https://campushub-2ar9.onrender.com/#/suggestions)、[骑行室使用申请](https://campushub-2ar9.onrender.com/#/rooms)，管理员在「活动建议 / 骑行室」查看和处理。入社申请增加联系方式、年级和学院。操作及 API 说明见 [骑行申请功能](docs/骑行申请功能.md)。

| 模块 | 学生端 | 管理员端 |
| --- | --- | --- |
| 用户 | 注册、登录、退出、修改资料、修改密码 | 学生/管理员角色、启用/停用账号 |
| 社团 | 分类筛选、搜索、详情、申请加入、撤回申请、退出社团 | 新增、编辑、招新开关、归档、成员审核 |
| 活动 | 分类/日期筛选、报名、取消报名、现场签到、历史记录 | 发布、编辑、取消、名额维护、签到码、报名签到列表 |
| 器材 | 搜索、申请借用、撤回、归还申请、逾期提示 | 器材登记、库存维护、停用、借用审核、归还验收 |
| 维修 | 提交工单、查看进度和处理时间线 | 受理、记录进度、修复、重新处理、关闭工单 |
| 统计 | 首页实际数据 | 成员分布、活动趋势、报名签到、器材使用、4 类 CSV 报表 |

所有导航、搜索、筛选、表单和操作按钮都连接实际逻辑。没有前端随机数或 localStorage 模拟业务数据库。

## Windows 一键准备和启动

本机已经准备项目独立的 JDK 21、PostgreSQL 17.6，位于 `.tools/`；运行数据位于 `.runtime/`。不会修改系统默认 Java 17，不注册数据库系统服务。

在项目根目录运行 PowerShell：

```powershell
# 新电脑首次准备：下载独立 JDK / PostgreSQL，初始化数据库，安装前端依赖
.\scripts\setup.ps1

# 启动数据库，执行测试和构建，以隐藏窗口方式启动完整应用
.\scripts\start.ps1

# 已有最新构建时快速启动
.\scripts\start.ps1 -NoBuild

# 停止网站，保留数据库
.\scripts\stop.ps1

# 网站和本项目数据库都停止（数据保留）
.\scripts\stop.ps1 -Database
```

需要 Node.js 22.12+ 或 24 LTS；本机使用 Node 24。Maven 3.9+ 已安装；新电脑没有 Maven 时，`setup.ps1` 会下载项目独立版本。首次下载 JDK 和 PostgreSQL 约 500 MB，需要联网。

如果 PowerShell 拦截本地脚本，可在本次 PowerShell 窗口执行 `Set-ExecutionPolicy -Scope Process Bypass`，关闭窗口即失效。

日志：`.runtime/app.log`、`.runtime/app-error.log`、`.runtime/postgres.log`。本地 PostgreSQL 仅监听 `127.0.0.1:54329`，开发库 `campushub`、测试库 `campushub_test`。本地开发数据库账号为 `campushub / campushub_local_dev`，不要把这个密码用于云部署。

若需连接已有数据库，设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，然后运行 `start.ps1 -ExternalDatabase`。集成测试使用单独的 `TEST_DB_URL`，默认指向本机 `campushub_test`，请先准备好测试库。

## 前后端分别开发

终端一：

```powershell
. .\scripts\env.ps1
.\scripts\database.ps1
mvn -f backend\pom.xml spring-boot:run
```

终端二：

```powershell
cd frontend
npm ci
npm run dev
```

前端 http://localhost:5173，后端 http://localhost:8080。Vite 将 `/api` 代理到后端，不需要放宽 CORS。使用哈希路由，刷新和直接打开 `/#/clubs`、`/#/admin` 均可正常访问。

手机与电脑接入相同局域网后，可访问 `http://电脑局域网IP:8080`；需要 Windows 防火墙允许该端口。本次没有修改防火墙，手机实机访问尚未验收。

## 构建与测试

```powershell
.\scripts\test.ps1          # 前端测试、类型检查、生产构建、后端测试和打包
.\scripts\build.ps1         # 已有 PostgreSQL 时，测试并构建
.\scripts\build.ps1 -SkipTests
```

跨平台手动构建（先准备 Java 21、Maven、Node 和 PostgreSQL）：

```sh
cd frontend
npm ci
npm test
npm run build
cd ../backend
# DB_USERNAME / DB_PASSWORD 与测试库权限一致时可直接运行
# TEST_DB_URL=jdbc:postgresql://localhost:5432/campushub_test
mvn verify
```

产物 `backend/target/campushub-1.0.0.jar` 已包含前端，设置数据库环境变量后执行 `java -jar backend/target/campushub-1.0.0.jar`。

测试结果、实际验收范围和未测试事项见 [测试报告](docs/测试报告.md)。请区分接口/组件测试与真实浏览器视觉验收。

后续已补做 [Edge 真实浏览器验收](docs/浏览器验收.md)，运行截图保存在 `docs/screenshots/`。桌面主要业务流程已实测，窄屏/手机、Docker 和云端部署仍未验收。

## Docker 启动

```sh
cp .env.example .env
# 编辑 .env：替换 DB_PASSWORD 和 ADMIN_PASSWORD
docker compose up --build -d
docker compose logs -f app
```

访问 http://localhost:8080，使用 `.env` 中的管理员账号。Compose 自动创建 PostgreSQL，Flyway 自动创建表、索引与会话存储。数据库使用命名卷持久化，`docker compose down` 不删除数据。备份命令见部署教程。本机未安装 Docker，镜像构建与 Compose 运行尚未实际验证。

## 免费云部署

推荐 **Render 免费 Docker Web Service + Neon 免费 PostgreSQL**。Java 运行在本项目的 Java 21 容器内，前后端同域，不需要额外静态站点或跨域配置。项目提供 `render.yaml`。

免费服务会有休眠、资源和额度限制，适合课程展示与小规模试用。Render 自带免费 PostgreSQL 会在创建 30 天后到期，因此教程使用 Neon 保存数据。详见 [Render 官方免费限制](https://render.com/docs/free) 和 [Neon 套餐说明](https://neon.com/docs/introduction/plans)。完整操作见 [免费部署教程](docs/部署教程.md)。本次仅完成本地启动，没有创建云端账号或发布公网站点。

## 项目结构

```text
backend/
  pom.xml
  src/main/java/cn/campushub/    # 实体、仓库、权限、认证、业务、报表
  src/main/resources/
    application.yml
    db/migration/              # Flyway SQL，唯一数据库结构来源
  src/test/java/               # PostgreSQL 集成测试与真实 HTTP 验收
frontend/
  src/pages/                   # 首页、社团、活动、器材、报修、成员、管理、认证
  src/components/              # 卡片、状态、时间线、管理编辑器
  src/api.ts                   # 同域会话、CSRF、请求与下载
  src/store.ts                 # 实际 API 数据
  src/style.css                # 桌面和手机响应式样式
scripts/                       # 准备、启动、停止、构建、测试、打包
docs/                          # 数据库设计、API、测试、部署
Dockerfile / compose.yml / render.yaml
```

## 安全与业务约定

- BCrypt（成本 12）加密密码；登录失败按来源地址限流；注册接口固定创建学生角色。
- HttpOnly、SameSite=Lax 的会话 Cookie，CSRF 令牌；生产 HTTPS 配置 `COOKIE_SECURE=true`。令牌和密码不写入 localStorage。
- Spring Session JDBC 持久化会话；修改密码撤销所有登录会话；停用与角色变更在下一次请求重新校验。
- 后端对每个管理员接口检查角色，对个人业务记录检查归属。公开活动接口不返回签到码，任何接口不返回密码哈希。
- 报名通过 PostgreSQL 悲观锁保护名额。借用申请阶段不占库存，审核通过扣库存，管理员验收归还后恢复库存，重复验收会被拒绝。
- 活动签到窗口为开始前 30 分钟至结束，需正确现场签到码；已签到的报名不能取消。
- 报表为 UTF-8 BOM CSV，支持 Excel，并处理引号、换行和公式注入前缀。
- 管理对象通过归档、停用、取消保留业务历史，不提供破坏关联数据的硬删除按钮。

## 文档

- [数据库设计与关系图](docs/数据库设计.md)
- [REST API 说明](docs/API.md)
- [免费部署与维护](docs/部署教程.md)
- [测试报告与验收限制](docs/测试报告.md)
- [图片与开源依赖说明](docs/素材说明.md)


当前管理员层级：普通用户 → 管理员 → 超级管理员。现有默认 admin 在 V7 升级为超级管理员，密码保留；管理员任免与管理员资料修改仅开放给超级管理员。骑行室用途名称为“存取车辆”。详见 docs/超级管理员.md。

2026-11-08 界面升级已上线：顶部导航、大标题与留白、白灰蓝视觉体系及手机布局，详见 [界面升级说明](docs/界面升级.md)。
