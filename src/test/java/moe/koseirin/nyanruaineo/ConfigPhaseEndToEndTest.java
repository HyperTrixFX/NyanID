package moe.koseirin.nyanruaineo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import moe.koseirin.nyanruaineo.Minecraft.netty.PacketDecoder;
import moe.koseirin.nyanruaineo.Minecraft.netty.PacketEncoder;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.LoginAcknowledged;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.LoginSuccess;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Drives the real Netty codecs (with the nextProtocol phase transitions) through a 1.21.1
 * NeoForge configuration exchange: backend LoginSuccess → client LoginAcknowledged → the proxy
 * configuring the backend → the backend's "minecraft:hello" reaching the client byte-identical.
 */
class ConfigPhaseEndToEndTest {

    private static final int V = 767;

    private static byte[] readAll(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    @Test
    void helloSurvivesRealCodecPipeline() {
        // ---- Backend channel: decoder(TO_CLIENT), encoder(TO_SERVER) ----
        EmbeddedChannel backend = new EmbeddedChannel();
        PacketDecoder backendDecoder = new PacketDecoder(Protocol.LOGIN, Direction.TO_CLIENT);
        PacketEncoder backendEncoder = new PacketEncoder(Protocol.LOGIN, Direction.TO_SERVER);
        backendDecoder.setProtocolVersion(V);
        backendEncoder.setProtocolVersion(V);
        backend.pipeline().addLast("dec", backendDecoder);
        backend.pipeline().addLast("enc", backendEncoder);

        // ---- Frontend channel: decoder(TO_SERVER), encoder(TO_CLIENT) ----
        EmbeddedChannel frontend = new EmbeddedChannel();
        PacketDecoder frontendDecoder = new PacketDecoder(Protocol.LOGIN, Direction.TO_SERVER);
        PacketEncoder frontendEncoder = new PacketEncoder(Protocol.LOGIN, Direction.TO_CLIENT);
        frontendDecoder.setProtocolVersion(V);
        frontendEncoder.setProtocolVersion(V);
        frontend.pipeline().addLast("dec", frontendDecoder);
        frontend.pipeline().addLast("enc", frontendEncoder);

        // 1. Backend sends LoginSuccess (LOGIN state).
        byte[] loginFrame = loginSuccessFrame();
        backend.writeInbound(Unpooled.wrappedBuffer(loginFrame));
        Object loginSuccess = backend.readInbound();
        assertInstanceOf(LoginSuccess.class, loginSuccess);

        // 2. Proxy forwards it to the client (frontend encoder, LOGIN).
        frontend.writeOutbound(loginSuccess);
        byte[] clientSaw = readAll((ByteBuf) frontend.readOutbound());
        assertArrayEquals(loginFrame, clientSaw, "client must see the backend's LoginSuccess");

        // 2.5 Proxy's enterConfiguration (1.20.2+ first join): front-end OUTBOUND codec → CONFIG.
        frontendEncoder.setProtocol(Protocol.CONFIGURATION);

        // 3. Client sends LoginAcknowledged (serverbound LOGIN 0x03) → decoder advances to CONFIG.
        ByteBuf ack = Unpooled.buffer();
        DefinedPacket.writeVarInt(0x03, ack);
        frontend.writeInbound(ack);
        Object ackPacket = frontend.readInbound();
        assertInstanceOf(LoginAcknowledged.class, ackPacket);
        assertEquals(Protocol.CONFIGURATION, frontendDecoder.getProtocol(),
                "frontend decoder must advance to CONFIGURATION");
        // The proxy consumes it (does not forward the client's ack verbatim).

        // 4. Proxy drives the backend into config (configureServer): backend decoder → CONFIG,
        //    write LoginAcknowledged (LOGIN encode → advances backend encoder to CONFIG).
        backendDecoder.setProtocol(Protocol.CONFIGURATION);
        backend.writeOutbound(new LoginAcknowledged());
        backend.readOutbound();                                        // the ack frame (LOGIN 0x03)
        assertEquals(Protocol.CONFIGURATION, backendEncoder.getProtocol(),
                "backend encoder must advance to CONFIGURATION via nextProtocol");
        backend.writeOutbound(new PluginMessage("minecraft:register",
                "bungeecord:main".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        backend.readOutbound();

        // 5. Backend sends "minecraft:hello" (config clientbound 0x01).
        byte[] helloFrame = configClientboundFrame("minecraft:hello", new byte[]{0x00});
        backend.writeInbound(Unpooled.wrappedBuffer(helloFrame));
        Object hello = backend.readInbound();
        assertInstanceOf(PluginMessage.class, hello);
        assertEquals("minecraft:hello", ((PluginMessage) hello).getTag());

        // 6. Proxy forwards it to the client (frontend encoder, CONFIG).
        assertEquals(Protocol.CONFIGURATION, frontendEncoder.getProtocol());
        frontend.writeOutbound(hello);
        byte[] clientHello = readAll((ByteBuf) frontend.readOutbound());
        assertArrayEquals(helloFrame, clientHello,
                "the backend's hello must reach the client byte-identical through the real codecs");
    }

    private static byte[] loginSuccessFrame() {
        LoginSuccess success = new LoginSuccess(UUID.randomUUID(), "test",
                List.of(new LoginSuccess.Property("textures", "abc", null)));
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeVarInt(0x02, buf);                          // LOGIN TO_CLIENT LoginSuccess
        success.write(buf, V);
        byte[] bytes = readAll(buf);
        buf.release();
        return bytes;
    }

    private static byte[] configClientboundFrame(String tag, byte[] payload) {
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeVarInt(0x01, buf);                          // CONFIG TO_CLIENT custom payload
        DefinedPacket.writeString(tag, buf);
        buf.writeBytes(payload);
        byte[] bytes = readAll(buf);
        buf.release();
        return bytes;
    }
}
