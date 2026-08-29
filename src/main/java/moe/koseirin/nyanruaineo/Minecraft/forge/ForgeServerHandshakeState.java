package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * 管理 BungeeCord 和 Forge 后端服务器之间握手流程的类，对应 BungeeCord 的
 * {@code ForgeServerHandshakeState}，状态机逻辑照 Forge 官方实现设计。
 */
enum ForgeServerHandshakeState implements IForgeServerPacketHandler<ForgeServerHandshakeState> {

    /**
     * Start the handshake.
     */
    START {
        @Override
        public ForgeServerHandshakeState handle(PluginMessage message, ServerConnection ch) {
            ForgeLogger.logServer(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            ch.sendPacket(message);
            return this;
        }

        @Override
        public ForgeServerHandshakeState send(PluginMessage message, UserConnection con) {
            // Send custom channel registration. Send Hello.
            return HELLO;
        }
    },
    HELLO {
        @Override
        public ForgeServerHandshakeState handle(PluginMessage message, ServerConnection ch) {
            ForgeLogger.logServer(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            if (message.getData()[0] == 1) {                          // Client Hello
                ch.sendPacket(message);
            }
            if (message.getData()[0] == 2) {                          // Client ModList
                ch.sendPacket(message);
            }
            return this;
        }

        @Override
        public ForgeServerHandshakeState send(PluginMessage message, UserConnection con) {
            // Send Server Mod List.
            return WAITINGCACK;
        }
    },
    WAITINGCACK {
        @Override
        public ForgeServerHandshakeState handle(PluginMessage message, ServerConnection ch) {
            ForgeLogger.logServer(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            ch.sendPacket(message);
            return this;
        }

        @Override
        public ForgeServerHandshakeState send(PluginMessage message, UserConnection con) {
            if (message.getData()[0] == 3 && message.getTag().equals(ForgeConstants.FML_HANDSHAKE_TAG)) {
                con.getForgeClientHandler().setServerIdList(message);
                return this;
            }
            if (message.getData()[0] == -1 && message.getTag().equals(ForgeConstants.FML_HANDSHAKE_TAG)) {
                // transition to COMPLETE after sending ACK
                return this;
            }
            if (message.getTag().equals(ForgeConstants.FORGE_REGISTER)) {
                // wait for Forge channel registration
                return COMPLETE;
            }
            return this;
        }
    },
    COMPLETE {
        @Override
        public ForgeServerHandshakeState handle(PluginMessage message, ServerConnection ch) {
            // Wait for ACK
            ForgeLogger.logServer(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            ch.sendPacket(message);
            return this;
        }

        @Override
        public ForgeServerHandshakeState send(PluginMessage message, UserConnection con) {
            // Send ACK
            return DONE;
        }
    },

    /**
     * 握手已经结束!
     * 之后如果再收到握手包就忽略掉，不再回复了。
     */
    DONE {
        @Override
        public ForgeServerHandshakeState handle(PluginMessage message, ServerConnection ch) {
            // RECEIVE 2 ACKS
            ForgeLogger.logServer(ForgeLogger.LogDirection.RECEIVED, this.name(), message);
            ch.sendPacket(message);
            return this;
        }

        @Override
        public ForgeServerHandshakeState send(PluginMessage message, UserConnection con) {
            return this;
        }
    }
}
