package moe.koseirin.nyanruaineo.Minecraft.protocol;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.util.AttributeKey;

/**
 * 代理端共享的协议常量，
 * 并额外包含本代理端所需的功能阈值。
 */
public final class ProtocolConstants {

    private ProtocolConstants() {
    }

    /** Pipeline handler names. */
    public static final String FRAME_DECODER = "frame-decoder";
    public static final String FRAME_PREPENDER = "frame-prepender";
    public static final String PACKET_DECODER = "packet-decoder";
    public static final String PACKET_ENCODER = "packet-encoder";
    public static final String CIPHER_DECODER = "cipher-decoder";
    public static final String CIPHER_ENCODER = "cipher-encoder";
    public static final String COMPRESSION_DECODER = "compression-decoder";
    public static final String COMPRESSION_ENCODER = "compression-encoder";
    public static final String HANDLER = "handler";

    /** 1.8 (47): the lowest version this proxy fully relays (BungeeCord's supported floor). */
    public static final int MINECRAFT_1_8 = 47;

    /** 1.9 (107). */
    public static final int MINECRAFT_1_9 = 107;

    /** 1.12 (335). */
    public static final int MINECRAFT_1_12 = 335;

    /** 1.12.1 (338). */
    public static final int MINECRAFT_1_12_1 = 338;

    /** 1.13 (393): channel names became namespaced ({@code minecraft:register}, {@code minecraft:brand}). */
    public static final int MINECRAFT_1_13 = 393;

    /** 1.14 (477). */
    public static final int MINECRAFT_1_14 = 477;

    /** 1.15 (573). */
    public static final int MINECRAFT_1_15 = 573;

    /** 1.16 (735): the LoginSuccess UUID became a raw 16-byte UUID. */
    public static final int MINECRAFT_1_16 = 735;

    /** 1.16.2 (751). */
    public static final int MINECRAFT_1_16_2 = 751;

    /** 1.17 (755). */
    public static final int MINECRAFT_1_17 = 755;

    /** 1.19 (759): LoginSuccess gained a properties list; LoginRequest gained a public key. */
    public static final int MINECRAFT_1_19 = 759;

    /** 1.19.1 (760): LoginRequest gained an optional UUID. */
    public static final int MINECRAFT_1_19_1 = 760;

    /** 1.19.3 (761): the LoginRequest public key was removed again. */
    public static final int MINECRAFT_1_19_3 = 761;

    /** 1.19.4 (762): the command-tree argument registry/parser format stabilised (1.19.4-1.20.1). */
    public static final int MINECRAFT_1_19_4 = 762;

    /** 1.20.2 (764): the LoginRequest UUID became mandatory. */
    public static final int MINECRAFT_1_20_2 = 764;

    /** 1.20.3 (765): NBT chat components, dimension as raw NBT. */
    public static final int MINECRAFT_1_20_3 = 765;

    /** 1.20.5 (766): LoginSuccess gained a trailing boolean (until 1.21.2). */
    public static final int MINECRAFT_1_20_5 = 766;

    /** 1.21 (767). */
    public static final int MINECRAFT_1_21 = 767;

    /** 1.21.2 (768): the trailing LoginSuccess boolean was removed. */
    public static final int MINECRAFT_1_21_2 = 768;

    /** 1.21.5 (770). */
    public static final int MINECRAFT_1_21_5 = 770;

    /** 1.21.6 (771). */
    public static final int MINECRAFT_1_21_6 = 771;

    /** 26.1 (775). */
    public static final int MINECRAFT_26_1 = 775;

    /** 1.26.2 (776): LoginSuccess gained a session id UUID. */
    public static final int MINECRAFT_26_2 = 776;

    /** Channel attribute holding the negotiated protocol version. */
    public static final AttributeKey<Integer> PROTOCOL_VERSION = AttributeKey.valueOf("protocol-version");

    /** Channel attribute holding the active packet {@link Protocol} state. */
    public static final AttributeKey<Protocol> PROTOCOL_STATE = AttributeKey.valueOf("protocol-state");
}
