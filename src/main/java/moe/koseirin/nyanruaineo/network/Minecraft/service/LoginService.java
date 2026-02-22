package moe.koseirin.nyanruaineo.network.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.EventBus;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerAuthenticatedEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ProtocolVersion;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.EncryptionRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.EncryptionResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginStartPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginSuccessPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.util.PacketSender;
import moe.koseirin.nyanruaineo.utils.System.EnumList.UUIDtype;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginService {
    private final ProxyProperties properties;
    private final BackendServerManager backendServerManager;
    private final MojangAuthService mojangAuthService;
    private final PacketSender packetSender;
    private final utilset utilset; // 你的RSA工具
    private final EventBus eventBus;
    private KeyPair serverKeyPair;
    private final ConcurrentHashMap<Channel, AuthContext> authContexts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            serverKeyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate RSA key pair", e);
        }
    }

    @EventHeader
    public void startLogin(Channel channel, LoginStartPacket packet) {
        if (!properties.isOnlineMode()) {
            log.info("Offline mode: player {} logging in", packet.getUsername());
            UUID offlineUuid = UUID.fromString(utilset.GenerateUUID(UUIDtype.Yggdrasil,false,packet.getUsername()));
            LoginSuccessPacket successPacket = new LoginSuccessPacket(
                    utilset.GenerateUUID(UUIDtype.Yggdrasil,false,packet.getUsername()),
                    packet.getUsername()
            );
            channel.writeAndFlush(successPacket).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("Failed to send LoginSuccess", future.cause());
                    channel.close();
                }
            });
            channel.attr(MinecraftProtocolHandler.STATE_KEY).set(ConnectionState.PLAY);
            LoginStartPacket loginStart = new LoginStartPacket(packet.getUsername());
            backendServerManager.connectAndForward(channel, loginStart);
//            eventBus.post(new PlayerAuthenticatedEvent(channel, packet.getUsername(), offlineUuid,false));
        } else {
            log.info("Online mode: player {} logging in, starting authentication", packet.getUsername());

            byte[] verifyToken = generateVerifyToken();
            byte[] publicKey = serverKeyPair.getPublic().getEncoded();

            authContexts.put(channel, new AuthContext(packet.getUsername(), verifyToken));

            EncryptionRequestPacket request = new EncryptionRequestPacket("", publicKey, verifyToken);
            packetSender.send(channel, request);
        }
    }

    public void completeLogin(Channel channel, EncryptionResponsePacket response) {
        AuthContext ctx = authContexts.remove(channel);
        if (ctx == null) {
            channel.close();
            return;
        }

        try {
            byte[] decryptedSecret = utilset.rsaDecrypt(response.getSharedSecret(), serverKeyPair.getPrivate());
            byte[] decryptedToken = utilset.rsaDecrypt(response.getVerifyToken(), serverKeyPair.getPrivate());

            if (!Arrays.equals(decryptedToken, ctx.verifyToken)) {
                channel.close();
                return;
            }

            mojangAuthService.authenticate(ctx.username, decryptedSecret, serverKeyPair.getPublic().getEncoded())
                    .thenAccept(playerProfile -> {
                        // 确保操作在 channel 的 EventLoop 中执行
                        channel.eventLoop().execute(() -> {
                            try {
                                // 1. 如果是在线模式，设置加密器
                                if (properties.isOnlineMode()) {
                                    SecretKeySpec secretKey = new SecretKeySpec(decryptedSecret, "AES");
                                    Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                                    encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(decryptedSecret));
                                    Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                                    decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(decryptedSecret));

                                    channel.attr(MinecraftProtocolHandler.ENCRYPTION_CIPHER).set(encryptCipher);
                                    channel.attr(MinecraftProtocolHandler.DECRYPTION_CIPHER).set(decryptCipher);
                                    log.info("Encryption enabled for channel {}", channel.id());
                                }

                                // 2. 根据客户端协议版本决定是否包含 properties
                                ProtocolVersion version = channel.attr(MinecraftProtocolHandler.PROTOCOL_KEY).get();
//                                List<LoginSuccessPacket.Property> properties = new ArrayList<>();
//                                if (version.getProtocol() >= 758) { // 1.19+
//                                    properties = playerProfile.getProperties();
//                                }

                                // 3. 发送 LoginSuccess（此时如果加密器已设置，会自动加密）
                                LoginSuccessPacket successPacket = new LoginSuccessPacket(
                                        playerProfile.getUuid().toString(),
                                        playerProfile.getName()

                                );
                                channel.writeAndFlush(successPacket).addListener(future -> {
                                    if (!future.isSuccess()) {
                                        log.error("Failed to send LoginSuccess", future.cause());
                                        channel.close();
                                    }
                                });
                                log.info("LoginSuccess sent to {} ({})", playerProfile.getName(), playerProfile.getUuid());

                                // 4. 切换客户端状态为 PLAY
                                channel.attr(MinecraftProtocolHandler.STATE_KEY).set(ConnectionState.PLAY);

                                // 5. 连接后端，发送 LoginStart（后端期望明文，无需加密）
                                LoginStartPacket loginStart = new LoginStartPacket(playerProfile.getName());
                                backendServerManager.connectAndForward(channel, loginStart);

                            } catch (Exception e) {
                                log.error("Error during login completion", e);
                                channel.close();
                            }
                        });
                    })
                    .exceptionally(throwable -> {
                        log.error("Mojang authentication failed for {}", ctx.username, throwable);
                        channel.close();
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to process encryption response", e);
            channel.close();
        }
    }
    public void continueLogin(Channel channel, String username, UUID uuid) {
        if (!channel.isActive()) {
            log.warn("Channel is no longer active, cannot continue login for {}", username);
            return;
        }
        LoginStartPacket loginPacket = new LoginStartPacket(username);
        channel.attr(AttributeKey.valueOf("uuid")).set(uuid);
        backendServerManager.connectAndForward(channel, loginPacket);
    }

    private byte[] generateVerifyToken() {
        byte[] token = new byte[16];
        new SecureRandom().nextBytes(token);
        return token;
    }

    @Data
    @AllArgsConstructor
    private static class AuthContext {
        private String username;
        private byte[] verifyToken;
    }
}