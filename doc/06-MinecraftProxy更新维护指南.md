# MinecraftProxy 更新维护指南

本文面向代理代码的日常维护：理解结构、更新 Minecraft 版本、扩展命令/权限/配置、排查常见问题。

## 1. 代码结构速览

### 网络管道（Netty Pipeline）

`MinecraftProxy.initChannel` 为每个入站连接创建前端管道；`ServerConnector.connect` 为每个后端连接创建后端管道。

```
前端：FirewallHandler → Varint21FrameDecoder → PacketDecompressor → PacketDecoder
      → PacketEncoder → PacketCompressor → Varint21LengthFieldPrepender → HandlerBoss
后端：Varint21FrameDecoder → PacketDecompressor → PacketDecoder
      → PacketEncoder → PacketCompressor → Varint21LengthFieldPrepender → HandlerBoss
```

- `Varint21FrameDecoder` / `Varint21LengthFieldPrepender`：按 21 位 VarInt 前缀切帧。
- `PacketDecoder` / `PacketEncoder`：按当前 `Protocol`（HANDSHAKE/STATUS/LOGIN/CONFIGURATION/GAME）+ 协议版本映射数据包 ID ↔ `DefinedPacket`。
- `HandlerBoss`：把入站数据包分发给当前 handler（`InitialHandler` / `UpstreamBridge` / `DownstreamBridge` / `LoginListener`）。
- `FirewallHandler`：最先执行，按 IP 限流/封禁。

### 关键 handler

| Handler | 职责 |
| --- | --- |
| `InitialHandler` | 握手、状态、登录、加密、封禁拦截、登录完成后交给 `ServerConnector` |
| `ServerConnector` | 连接后端、后端登录握手（内部 `LoginListener`）、安装 `DownstreamBridge` |
| `UpstreamBridge` | 客户端→后端转发；拦截命令/聊天/补全/ClientSettings/插件消息 |
| `DownstreamBridge` | 后端→客户端转发；TabList 拦截、品牌重写、`declare_commands` 替换、BungeeCord 频道处理 |

### 关键 service

| Service | 职责 |
| --- | --- |
| `PlayerAuthService` | Yggdrasil / Mojang 会话校验 |
| `PlayerTransferService` | 跨服切换（幂等） |
| `PlayerKickService` | 踢出（版本适配的踢出包） |
| `PlayerMessageService` | 发消息 |
| `PlayerQueryService` | 在线玩家查询 |
| `PlayerStateService` | 计分板/队伍/BossBar 等状态跟踪 |
| `TabListService` | TabList 前缀/后缀/header/footer 拦截 |
| `PluginMessageService` | BungeeCord 频道子命令、Forward 队列 |
| `FirewallService` | 连接防火墙 |
| `ProxyBanService` | 封禁（type 5/6，UID/UUID） |
| `BackendServerManager` / `ServerStatusService` | 子服务器列表 / 在线探测 |
| `PingResponseProvider` | MOTD / ping 响应 |

## 2. 登录与切换流程

**首次登录**：
1. `Handshake` → 确定协议版本与目标服务器。
2. `LoginRequest`（`LoginStart`）→ 防火墙登录限流 → 若 `online-mode` 走加密。
3. `EncryptionResponse` → 建立加密 → `PlayerAuthService.authenticate`（先 Yggdrasil 内网校验，再 Mojang 兜底）。
4. `finishLogin`：封禁拦截 → 创建 `UserConnection` → `ServerConnector.connect`。
5. 后端登录成功 → 安装桥接 → 转发 `JoinGame` → 注册在线玩家。

**跨服切换**：
1. `PlayerTransferService.transfer`：置 `switchingServer=true`、暂停客户端读取、`nextServerGeneration`、关闭旧后端、`new ServerConnector(...).connect()`。
2. 新后端 `LoginSuccess` →（1.20.2+）配置阶段握手；（旧版）直接进 GAME。
3. 新后端 `JoinGame` → 重置世界状态（计分板/TabList/Respawn）→ 清除 `switchingServer`、恢复读取。

> `switchingServer` 用于丢弃切换期间客户端残留的 GAME 帧；`serverGeneration` 用于防止旧后端关闭时误踢客户端。

## 3. 更新 Minecraft 版本（新增/调整协议）

涉及的文件：

| 文件 | 作用 |
| --- | --- |
| `protocol/ProtocolConstants.java` | 版本号常量（如 `MINECRAFT_1_19_4 = 762`） |
| `protocol/Protocol.java` | 各 `Protocol`（方向）的数据包 ID 注册表 |
| `protocol/ProtocolData.java` | 每版本的数据包映射实现 |
| `protocol/packet/*.java` | 具体 `DefinedPacket` 子类（read/write） |
| `handler/UpstreamBridge.java` | 补全响应包 ID（`buildTabCompleteResponse`） |
| `handler/DownstreamBridge.java` | `declare_commands` 包 ID（`declareCommandsId`） |
| `service/PlayerKickService.java` | 踢出包 ID（`kickPacketId`） |

更新步骤：
1. 在 `ProtocolConstants` 增加版本常量。
2. 在 `Protocol` 中注册该版本下发生变动的数据包 ID（对照 BungeeCord 的 `Protocol.java` 与 `ArgumentRegistry`）。
3. 若数据包结构变化，更新对应 `DefinedPacket.read/write`。
4. 更新三处版本相关的「裸帧包 ID」：补全响应、命令树、踢出包。
5. 编译并实测「登录、切换、Tab 补全、TabList、皮肤/披风」全链路。

### 常见易错点（本仓库已踩过的坑）

- **`command_suggestions` 与 `cookie_request` ID 混淆**：1.20.5+ 的 play 阶段 `cookie_request`（0x16/0x15）曾被误用作补全响应 ID，导致 1.21+ 客户端解码失败。补全响应用 `command_suggestions` 的 ID。
- **`declare_commands` 解析器属性宽度**：跳过节点属性时，`float/integer` 边界 4 字节、`double/long` 边界 8 字节、`time` 4 字节 int、`string` VarInt。写错会导致命令树遍历漂移，后端命令补全失效。
- **1.20.2+ 配置阶段**：切换服务器后需重放 `ClientSettings`（含 skinParts，影响皮肤层/披风）、品牌、频道注册，否则皮肤层丢失。
- **`Varint21FrameDecoder`**：只允许 3 字节 VarInt（21 位），超长/空包抛 `CorruptedFrameException`。

## 4. 添加一个代理命令

1. 在 `command/BuiltinCommands.register()` 里 `commandManager.registerCommand(new ProxyCommand("名称", 权限节点, "别名") {...})`。
2. 实现 `execute(CommandSender sender, String[] args)`；需要补全时覆写 `onTabComplete`。
3. 若需要权限，在 `utils/System/PermissionNodes` 增加节点常量。

命令分发在 `UpstreamBridge`（`Chat`/`ClientCommand`/`UnsignedClientCommand`），未命中的命令原样转发后端。

## 5. 添加一个权限节点

1. 在 `utils/System/PermissionNodes` 增加 `public static final String XXX = "minecraftproxy...."`。
2. 在命令注册时传入该节点，或在代码里调用 `sender.hasPermission(节点)`。
3. 通过 `/op <uid>` 授予 root，或 `POST /api/zako/v3/proxy/permissions?uid=&node=` 授予具体节点。

权限匹配规则：`*` 根节点、精确匹配、`prefix.*` 通配（见 `services/PermissionService`）。

## 6. 添加一个动态配置项

1. 在 `config/ProxyProperties` 定义 `KEY_*` 常量与读取方法（缺省时写回默认值）。
2. 若配置是结构化对象，在 `config/cfg/` 建一个 `@Data` 类，用 fastjson2 序列化/反序列化。
3. 修改后通过 `ReloadConfig` 控制台命令或重启生效（`SystemConfigCacheService.loadConfigs` 重新加载内存缓存）。

## 7. 添加 BungeeCord 频道子命令

在 `service/PluginMessageService.handleBungeeCordChannel` 的 `switch` 中加 `case`。子命令通常读 `DataInputStream`，需要回复时写 `out`；`Forward`/`ForwardToPlayer` 之类会把 `out` 置空不回复。

> `Connect`/`ConnectOther` 有按 `(目标, 服务器)` 的 1 秒去重（后端会通过每个在线玩家连接重复下发同一消息）。

## 8. 构建与验证

```bash
mvn clean package -DskipTests
```

无 Maven 时可用 `javac` 编译校验（注意必须启用 Lombok 注解处理，即 `-proc:full`，不能用 `-proc:none`）：

```bash
javac -encoding UTF-8 -proc:full \
  -cp "target/classes:$(find ~/.m2/repository -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | tr '\n' ':')" \
  -d /tmp/out $(find src/main/java -name '*.java')
```

验证清单：
- 登录（Yggdrasil / 正版 / 离线）。
- `/server` 切换与回来（注意 1.20.2+ 的「reconfiguring」卡住问题）。
- Tab 补全（代理命令 + 后端命令）。
- TabList（header/footer、前后缀）。
- 皮肤第二层/披风（切换后仍正常）。
- 封禁、防火墙、权限。

## 9. 数据库结构变更注意

实体主键/列重命名时，`spring.jpa.hibernate.ddl-auto=update` **只加列、不删列**，旧列（尤其 NOT NULL 无默认值的列）会导致插入报错（如 `Field 'xxx' doesn't have a default value`）。必要时手动 `DROP TABLE` 让 Hibernate 重建，或手工 `ALTER TABLE`。

## 10. 参考

- 可自行查阅BungeeCord源码进行修改
