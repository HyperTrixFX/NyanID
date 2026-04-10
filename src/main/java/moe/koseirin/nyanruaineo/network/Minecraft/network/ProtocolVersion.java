package moe.koseirin.nyanruaineo.network.Minecraft.network;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;

@Getter
public enum ProtocolVersion {
    UNKNOWN(-1, "Unknown"),
    MC_1_7_10(5, "1.7.10"),
    MC_1_8_8(47, "1.8.8"),
    MC_1_8_9(47, "1.8.9"),
    MC_1_12_2(340, "1.12.2"),
    MC_1_16_5(754, "1.16.5"),
    MC_1_17_1(756, "1.17.1"),
    MC_1_20_1(763, "1.20.1"),
    MC_1_21_1(766, "1.21.1");

    private final int protocol;
    private final String versionName;

    ProtocolVersion(int protocol, String versionName) {
        this.protocol = protocol;
        this.versionName = versionName;
    }

    public static ProtocolVersion fromProtocol(int protocol) {
        for (ProtocolVersion v : values()) {
            if (v.protocol == protocol) return v;
        }
        return UNKNOWN;
    }
}
