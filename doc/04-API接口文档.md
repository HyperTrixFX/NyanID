# API 接口文档

## 通用约定

### 认证头

大部分需要登录态的接口使用：

```
Authorization: Bearer <RSA 加密后的 accessToken>
Event: <操作类型>
```

- `accessToken`：登录后返回的 32 位原始 token（`UserDevices.Token`）。
- 传输时用 RSA **公钥** 加密（客户端），服务端用 `yggdrasil.privateKey` 解密。
- `Event` 头与 HTTP 方法需匹配（由 `AuthenticateCheck` 拦截器校验）：

| Event | 含义 | 对应 HTTP 方法 |
| --- | --- | --- |
| `0` | 获取信息 | GET |
| `1` | 上传数据 | POST |
| `2` | 上传资源 | PUT |
| `3` | 删除 | DELETE |

### 拦截器覆盖范围（AuthenticateCheck）

`/api/zako/v1/userdata`、`/api/zako/v1/userinfo`、`/api/zako/v1/bma`、`/api/zako/v1/cl`、`/api/zako/v1/user/devices`、`/api/yggdrasil/open/account`、`/api/zako/v1/user/violation/history`、`/api/v3/zako/administration/validate`、`/api/zako/v1/user/2fa/open2fa`、`/api/zako/v1/user/2fa/close2fa`、`/api/yggdrasil/textures/**`、`/api/zako/v3/**`。

### 响应格式

- 成功多为 `200` + JSON；部分为 `204 No Content`。
- 错误多返回 `ErrorResponse`（`{ "error": "...", "message": "...", "timestamp": "..." }`）或 `Respond.respond(...)` 生成的 JSON。

---

## 根路径

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/` | 返回 `Ok`，并设置 `X-Authlib-Injector-API-Location: /api/yggdrasil` |
| POST | `/` | 同上（JSON `Ok`） |

---

## V1：用户认证与资料（`/api/zako/v1`）

### 注册 / 登录

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| POST | `/register` | `RegisterDTO` | 注册（发送验证邮件） |
| POST | `/verification` | `RegisterConfirmDTO` | 用邮件验证码完成注册 |
| POST | `/login` | `LoginDTO` | 邮箱 + 密码登录 |
| POST | `/2fa` | `LoginDTO` | 2FA 二次验证登录 |

**RegisterDTO**

```json
{ "email": "...", "password": "...", "username": "...", "idempotencyKey": "..." }
```

**RegisterConfirmDTO**

```json
{ "code": "..." }
```

**LoginDTO**

```json
{ "email": "...", "password": "...", "idempotencyKey": "...", "have2fa": false, "token": "...", "verifyCode": "..." }
```

### 微软登录

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/microsoft/newlogin` | - | 重定向到微软登录 |
| POST | `/microsoft/newlogin` | `code`（必填）、`id_token`（可选）、`state`（必填） | 微软 OAuth 回调 |

### 忘记 / 重置密码

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | `/forgetpwd?email=` | query `email` | 发送找回密码邮件 |
| POST | `/resetpwd` | `ResetPwdDTO` | 用 token + code + 新密码重置 |

**ResetPwdDTO**

```json
{ "token": "...", "code": "...", "password": "..." }
```

### 用户资料

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| POST | `/userdata` | `UserDataDTO` | 按 `action` 执行资料操作（改昵称/用户名/描述等） |
| PUT | `/userdata` | multipart `avatar` | 上传头像 |

**UserDataDTO**

```json
{ "action": 0, "nickname": "...", "username": "...", "description": "...", "code": "..." }
```

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | `/userinfo` | -（需认证） | 获取当前用户完整资料 |

### 安全 / 设备

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| POST | `/user/2fa/open2fa` | -（需认证） | 开启 2FA |
| POST | `/user/2fa/close2fa` | -（需认证） | 关闭 2FA |
| GET | `/user/devices` | -（需认证） | 查询登录设备 |
| DELETE | `/user/devices` | `DeleteDevicesDTO` | 注销设备 |

**DeleteDevicesDTO**

```json
{ "value": "设备标识" }
```

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | `/user/violation/history` | -（需认证） | 违规/封禁历史 |

### 占位

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/devices` | 目前返回 `null`（未实现） |

---

## V2：公开信息（`/api/zako/v2`）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | `/userinfo/{uuid}` | path `uuid` | 公开用户信息（昵称、exp、description、uid…） |
| POST | `/searchuser` | `UserResponseDTO` | 搜索用户 |
| GET | `/server` | - | 服务器信息 |

**UserResponseDTO**

```json
{ "value": "关键词" }
```

> `StaticResourceController`（`/api/zako/v2`）目前为空。

---

## V3：管理（`/api/zako/v3`）

### 代理管理（`/api/zako/v3/proxy`，需 `minecraftproxy.admin` 权限）

所有接口都需要 `Authorization: Bearer <token>`；由于 `/api/zako/v3/**` 会经过 `AuthenticateCheck` 拦截器，还需携带 `Event` 头（`0`/`1`/`2`/`3`，与 HTTP 方法匹配）。服务端解析出 uid 后校验 `minecraftproxy.admin` 权限；无权限返回 `403 Permission denied!`。

#### 后端服务器管理

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| GET | `/servers` | - | 列出所有后端服务器 |
| POST | `/servers` | `BackendServer` | 新增后端服务器 |
| PUT | `/servers/{uid}` | `BackendServer` | 更新指定后端 |
| DELETE | `/servers/{uid}` | - | 删除指定后端 |

**BackendServer**

```json
{ "uid": "lobby-001", "priority": 1, "name": "lobby", "host": "localhost", "port": 25566 }
```

#### 权限管理

| 方法 | 路径 | 参数 | 说明 |
| --- | --- | --- | --- |
| GET | `/permissions` | `uid` | 查询用户权限节点集合 |
| POST | `/permissions` | `uid`、`node` | 授予权限节点 |
| DELETE | `/permissions` | `uid`、`node` | 回收权限节点 |

### 其他 V3 控制器

`AdministrationController`、`ServerManagerController`、`ServerSyncController`（均在 `/api/zako/v3`）目前为空占位。

---

## V4 / V5（占位）

- `ApplicationController`、`Oauth2Controller`（`/api/zako/v4`）：空。
- `DevsManager`（`/api/zako/v5` 相关）：空。

---

## AI 聊天（`/api/v6/romyuai`）

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| POST | `/chat` | `AIChatRequestDTO` | 发送聊天消息；`ai.enable=false` 时返回 `204` |

**AIChatRequestDTO**

```json
{ "message": "..." }
```

---

## Yggdrasil 外置登录

### 元信息（`/api/yggdrasil`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/` 或 `""` | 返回 Yggdrasil 元信息（meta、skinDomains、signaturePublickey），并设置 `X-Authlib-Injector-API-Location` |

### 账号（`/api/yggdrasil/open/account`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/open/account` | 为当前登录用户创建 Yggdrasil 角色（需 `Authorization`）；已存在返回 404 |

### Authserver（`/api/yggdrasil/authserver`）

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| POST | `/authenticate` | Yggdrasil authenticate JSON | 登录（`username`=邮箱、`password`、`clientToken`、`requestUser`、`agent`） |
| POST | `/refresh` | `accessToken`、`clientToken`、`requestUser`… | 刷新 accessToken |
| POST | `/validate` | `accessToken`、`clientToken` | 校验 accessToken（成功 204） |

### Sessionserver（`/api/yggdrasil/sessionserver/session/minecraft`）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| POST | `/join` | `accessToken`、`selectedProfile`、`serverId` | 客户端加入服务器时登记会话 |
| GET | `/hasJoined?username=&serverId=` | query | 服务端校验玩家会话 |
| GET | `/profile/{uuid}` | path `uuid`（可带 `unsigned` query） | 获取角色 profile（含 textures 属性） |

### Textures（`/api/yggdrasil/textures`）

| 方法 | 路径 | 请求 | 说明 |
| --- | --- | --- | --- |
| PUT | `/skin` | multipart `skin`（+`model`=default/slim） | 上传皮肤 |
| PUT | `/cape` | multipart `cape` | 上传披风 |

（均需 `Authorization`，仅 PNG 有效）

---

## 资源（`/api/zako/res`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/zako/res/{type}/{data}` | `type` 为 `avatar`（32 位 uid）或 `textures`（纹理 hash）；返回对应图片 |

---

## 代理插件消息（BungeeCord 频道）

后端（Spigot/Paper）通过 `BungeeCord` 频道向代理发送的**子命令**（非 HTTP，但属于代理 API）：

| 子命令 | 说明 |
| --- | --- |
| `ForwardToPlayer` | 转发插件消息到指定玩家的后端 |
| `Forward` | 转发到 `ALL` / `ONLINE` / 指定服务器 |
| `Connect` / `ConnectOther` | 让玩家连接到指定服务器（已做去重） |
| `GetPlayerServer` | 查询玩家所在服务器 |
| `IP` / `IPOther` | 查询玩家 IP |
| `PlayerCount` | 查询在线人数 |
| `PlayerList` | 查询玩家列表 |
| `GetServers` / `GetServer` | 查询服务器列表 / 当前服务器 |
| `Message` / `MessageRaw` | 发送消息 |
| `UUID` / `UUIDOther` | 查询玩家 UUID |
| `ServerIP` | 查询服务器地址 |
| `KickPlayer` / `KickPlayerRaw` | 踢出玩家 |
