# Minecraft 代理使用

代理是 BungeeCord 轻量移植跨服代理。玩家连接 `proxy.port`（默认 25565），登录后根据 `proxy.backend.servers` 路由到子服务器。

## 游戏内命令

| 命令 | 权限节点 | 说明 |
| --- | --- | --- |
| `/bind` | - | 生成 NyanID 账号绑定码（6 位数字，180 秒有效） |
| `/lobby`（别名 `/hub`） | - | 返回默认子服务器 |
| `/server [名称]` | `minecraftproxy.command.server` | 列出/切换到子服务器 |
| `/ip [玩家]` | `minecraftproxy.command.ip` | 查看自己或目标玩家 IP |
| `/list` | `minecraftproxy.command.list` | 在线玩家列表 |
| `/status` | `minecraftproxy.command.status` | 子服务器在线状态 |
| `/broadcast <消息>`（别名 `/bc`） | `minecraftproxy.command.broadcast` | 全服广播 |
| `/kick <玩家> [原因]` | `minecraftproxy.command.kick` | 踢出玩家 |
| `/ban <玩家> [原因] [时长]` | `minecraftproxy.command.ban` | 封禁玩家（默认 type=5） |

### /ban 用法

```
/ban <玩家> [原因] [时长]
```

- 时长格式：`30s` / `30m` / `2h` / `7d` / `1w`；缺省为**永久**。
- 目标玩家若是 Yggdrasil（NyanID 外置登录）玩家 → 封其 NyanID UID；若是正版（Mojang）玩家 → 封其 UUID。
- 封禁后立即踢下线，并在下次登录时拦截。

### 命令权限

命令默认注册了权限节点，没有授予该节点（或 `*`）的玩家会被拒绝并提示「你没有权限使用这个指令」。

> 未注册为代理命令的 `/xxx` 会原样转发给后端执行。

## 权限节点

见 [08-权限与封禁系统](./08-权限与封禁系统.md)。

常用节点：

- `*` —— 全部权限
- `minecraftproxy.admin` —— 代理管理（HTTP 面板 + 全部代理命令）
- `minecraftproxy.command.*` —— 全部代理命令
- `minecraftproxy.command.server` / `.kick` / `.ban` / `.list` / `.status` / `.broadcast` / `.ip`

## 封禁系统

- 游戏封禁 type：`5`（禁止登录游戏，可登录网站）、`6`（死封，永久）。
- 目标类型：`0`=NyanID UID，`1`=Mojang UUID。
- 自动解封：见 [08-权限与封禁系统](./08-权限与封禁系统.md)。

## 防火墙

`proxy.firewall` 配置每 IP 的连接速率、并发、登录尝试/失败限制，超限临时封禁 IP。玩家进入后端后该 IP 计数重置。

## TabList 拦截

`proxy.tablist` 配置 header/footer 与 prefix/suffix，占位符 `%online%`、`%max%`、`%server%`。

## MOTD

`proxy.motd` 配置服务器列表显示的 MOTD 行、在线人数（含假人）、版本名。

## 插件消息（BungeeCord 频道）

后端插件通过 `BungeeCord` 频道与代理通信。代理支持 `Connect`/`ConnectOther`、`Forward`、`Message`、`KickPlayer`、`GetServer` 等子命令，详见 [04-API接口文档](./04-API接口文档.md) 末尾。
