# REST API

基础路径 `/api`，JSON 编码 UTF-8。认证使用同域 Cookie 会话；普通用户只能读写自己的申请、报名、借还和维修记录；`/admin/**` 必须是 ADMIN。

## 调用约定

1. `GET /api/auth/csrf`，保存响应 Cookie，并读取 `{ "token": "...", "headerName": "X-CSRF-TOKEN" }`。
2. POST / PUT / DELETE 请求携带该 Cookie 与 `X-CSRF-TOKEN` 请求头。
3. 登录成功、退出或会话失效后重新获取 CSRF 令牌。
4. 浏览器使用 `credentials: 'same-origin'`；非浏览器客户端需要 CookieJar。

错误响应例如 `{ "message": "报名人数已满" }`。常见状态：400 输入/状态不合法，401 未登录，403 无权限/CSRF 不合法，404 不存在，409 唯一约束冲突，429 登录限流。

## 公开接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 健康状态 |
| GET | `/public/overview` | 首页实际统计、是否为演示环境 |
| GET | `/clubs` | 正常运营社团、成员数 |
| GET | `/activities` | 已发布活动、名额与报名数，不包含签到码 |
| GET | `/equipment` | 可借用器材和实时库存 |
| GET | `/auth/csrf` | CSRF 令牌 |
| POST | `/auth/register` | 注册学生账号，201 |
| POST | `/auth/login` | 用户名密码登录 |

注册请求：

```json
{"username":"zhangsan","password":"YourStrongPassword2026!","name":"张三","email":"zhangsan@example.com","studentNo":"20260001","department":"计算机学院"}
```

用户请求中的 role 字段不能授予管理员权限。用户名规范化为小写；邮箱唯一；密码 8–64 字符且 UTF-8 不超过 72 字节。

## 登录用户接口

| 方法 | 路径 | 请求/说明 |
| --- | --- | --- |
| GET | `/auth/me` | 当前用户信息，不含密码哈希 |
| POST | `/auth/logout` | 撤销当前会话 |
| GET | `/me/dashboard` | memberships / registrations / loans / repairs，含关联详情 |
| PUT | `/me/profile` | name、email、department、bio |
| PUT | `/me/password` | oldPassword、newPassword；撤销所有会话 |
| POST | `/clubs/{id}/applications` | `{ "reason": "加入理由" }`，201 |
| POST | `/applications/{id}/withdraw` | 撤回申请或退出社团 |
| POST | `/activities/{id}/registrations` | 报名，201 |
| DELETE | `/registrations/{id}` | 取消自己的报名 |
| POST | `/registrations/{id}/checkin` | `{ "code": "现场签到码" }` |
| POST | `/loans` | equipmentId、quantity、purpose、dueDate（YYYY-MM-DD），201 |
| POST | `/loans/{id}/cancel` | 取消待审核借用 |
| POST | `/loans/{id}/return` | 提交已批准借用的归还申请 |
| POST | `/repairs` | equipmentId（可选）、title、description、location、priority，201 |

维修优先级为 LOW / NORMAL / HIGH。创建维修工单后自动创建首条进度记录，个人工作台只返回自己的工单。

## 管理员接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/{resource}` | resource 可为 clubs、activities、equipment、applications、registrations、loans、repairs、users |
| POST | `/admin/clubs` | 创建社团，201 |
| PUT | `/admin/clubs/{id}` | 编辑社团、招新和归档 |
| POST | `/admin/activities` | 发布活动，201 |
| PUT | `/admin/activities/{id}` | 编辑活动或取消 |
| POST | `/admin/equipment` | 器材登记，201 |
| PUT | `/admin/equipment/{id}` | 修改器材、总库存和借用开关 |
| PUT | `/admin/applications/{id}/review` | `{ "status": "APPROVED或REJECTED", "note": "审核意见" }` |
| PUT | `/admin/loans/{id}/review` | status 为 APPROVED / REJECTED / RETURNED；归还仅允许从 RETURN_PENDING 验收 |
| POST | `/admin/repairs/{id}/progress` | `{ "status": "PROCESSING", "note": "已安排检查" }` |
| PUT | `/admin/users/{id}/access` | `{ "role": "STUDENT或ADMIN", "enabled": true }`，禁止修改自身权限 |
| GET | `/admin/reports` | 概览、近六月趋势、社团分布、器材使用 |
| GET | `/admin/reports/export?type=activities` | CSV，type 可为 activities / members / equipment / repairs |

### 社团字段

`name、category、summary、description、icon、color（#RRGGBB）、location、contact、recruiting、active`。

### 活动字段

`clubId、title、category、description、location、startsAt、endsAt、registrationDeadline、capacity、checkinCode、featured、status`。

三个时间字段均为 ISO 8601，例如 `2026-11-01T10:00:00Z`。签到码 4–32 位，仅管理员活动列表返回。新活动开始时间必须在未来，编辑容量不能小于已报名人数。

### 器材字段

`name、category、assetCode、description、location、totalQuantity、active`。客户端不能直接设置 availableQuantity，服务端按在借数量计算。

## 请求实例

前端完整调用实现见 `frontend/src/api.ts`。例如：

```js
const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
const { token } = await csrfResponse.json()
const response = await fetch('/api/clubs/1/applications', {
  method: 'POST', credentials: 'same-origin',
  headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
  body: JSON.stringify({ reason: '希望和大家一起参加摄影活动。' })
})
const result = await response.json()
```
