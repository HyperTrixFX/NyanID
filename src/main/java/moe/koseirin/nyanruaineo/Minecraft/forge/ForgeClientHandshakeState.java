package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

import java.util.Map;

/**
 * 管理 BungeeCord 和 Forge 客户端之间握手流程的类，对应 BungeeCord 中的
 * {@code ForgeClientHandshakeState}。它的状态机逻辑是照着 Forge 官方的实现来设计的
 * （源码参考：https://github.com/MinecraftForge/FML/blob/master/src/main/java/cpw/mods/fml/common/network/handshake/FMLHandshakeClientState.java）。
 */
enum ForgeClientHandshakeState implements IForgeClientPacketHandler<ForgeClientHandshakeState> {

    /**
     * 这个状态在客户端握手启动时创建，等初始化完成后就会切换到 HELLO 状态。
     * 使用前提：必须有一个 {@link UserConnection} 实例。
     */
    START {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            con.sendPacket(message);
            con.getForgeClientHandler().setState();
            return HELLO;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            return HELLO;
        }
    },
    /**
     * 这个状态会一直等着客户端发来 HELLO 包和模组列表。拿到模组列表后会先记下来，然后等着服务器那边的数据。
     * 如果连上的不是 Forge 客户端，就会一直卡在这个状态——没关系，这是预期行为。
     */
    HELLO {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            // Server Hello.
            if (message.getData()[0] == 0) {
                con.sendPacket(message);
            }
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            // Client Hello.
            if (message.getData()[0] == 1) {
                return this;
            }
            // Mod list.
            if (message.getData()[0] == 2) {
                if (con.getForgeClientHandler().getClientModList() == null) {
                    // 首次 Forge 连接——获取模组列表。
                    // 一旦完成，就没必要重复执行。
                    Map<String, String> clientModList = ForgeUtils.readModList(message);
                    con.getForgeClientHandler().setClientModList(clientModList);
                }
                return WAITINGSERVERDATA;
            }
            return this;
        }
    },
    WAITINGSERVERDATA {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            // Mod list.
            if (message.getData()[0] == 2) {
                con.sendPacket(message);
            }
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            // ACK
            return WAITINGSERVERCOMPLETE;
        }
    },
    WAITINGSERVERCOMPLETE {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            // Mod ID's.
            if (message.getData()[0] == 3) {
                con.sendPacket(message);
                return this;
            }
            con.sendPacket(message);                                  // pass everything else
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            // Send ACK.
            return PENDINGCOMPLETE;
        }
    },
    PENDINGCOMPLETE {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            // Ack.
            if (message.getData()[0] == -1) {
                ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
                con.sendPacket(message);
            }
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            // Send an ACK
            return COMPLETE;
        }
    },
    COMPLETE {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            // Ack.
            if (message.getData()[0] == -1) {
                ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
                con.sendPacket(message);
            }
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            return DONE;
        }
    },

    /**
     * 握手(忍耐)已经结束！
     * 之后如果再收到握手包就直接丢掉不管了。
     */
    DONE {
        @Override
        public ForgeClientHandshakeState handle(PluginMessage message, UserConnection con) {
            ForgeLogger.logClient(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            return this;
        }

        @Override
        public ForgeClientHandshakeState send(PluginMessage message, UserConnection con) {
            return this;
        }
    }
}
