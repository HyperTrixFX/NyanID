package moe.koseirin.nyanruaineo.Minecraft.protocol;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;

/**
 * Human-readable protocol version mapping, mirroring BungeeCord's {@code ProtocolConstants}. The raw
 * integer protocol number drives wire-format decisions; this enum only maps it to a display name.
 */
@Getter
public enum ProtocolVersion {

    UNKNOWN(-1, "Unknown"),
    MC_1_7_2(4, "1.7.2-1.7.5"),
    MC_1_7_10(5, "1.7.6-1.7.10"),
    MC_1_8(47, "1.8.x"),
    MC_1_9(107, "1.9"),
    MC_1_9_1(108, "1.9.1"),
    MC_1_9_2(109, "1.9.2"),
    MC_1_9_4(110, "1.9.3-1.9.4"),
    MC_1_10(210, "1.10.x"),
    MC_1_11(315, "1.11"),
    MC_1_11_1(316, "1.11.1-1.11.2"),
    MC_1_12(335, "1.12"),
    MC_1_12_1(338, "1.12.1"),
    MC_1_12_2(340, "1.12.2"),
    MC_1_13(393, "1.13"),
    MC_1_13_1(401, "1.13.1"),
    MC_1_13_2(404, "1.13.2"),
    MC_1_14(477, "1.14"),
    MC_1_14_1(480, "1.14.1"),
    MC_1_14_2(485, "1.14.2"),
    MC_1_14_3(490, "1.14.3"),
    MC_1_14_4(498, "1.14.4"),
    MC_1_15(573, "1.15"),
    MC_1_15_1(575, "1.15.1"),
    MC_1_15_2(578, "1.15.2"),
    MC_1_16(735, "1.16"),
    MC_1_16_1(736, "1.16.1"),
    MC_1_16_2(751, "1.16.2"),
    MC_1_16_3(753, "1.16.3"),
    MC_1_16_4(754, "1.16.4-1.16.5"),
    MC_1_17(755, "1.17"),
    MC_1_17_1(756, "1.17.1"),
    MC_1_18(757, "1.18-1.18.1"),
    MC_1_18_2(758, "1.18.2"),
    MC_1_19(759, "1.19-1.19.2"),
    MC_1_19_3(761, "1.19.3"),
    MC_1_19_4(762, "1.19.4"),
    MC_1_20(763, "1.20-1.20.1"),
    MC_1_20_2(764, "1.20.2"),
    MC_1_20_3(765, "1.20.3-1.20.4"),
    MC_1_20_5(766, "1.20.5-1.20.6"),
    MC_1_21(767, "1.21-1.21.1"),
    MC_1_21_2(768, "1.21.2-1.21.3"),
    MC_1_21_4(769, "1.21.4"),
    MC_1_21_5(770, "1.21.5"),
    MC_1_21_6(771, "1.21.6"),
    MC_1_21_7(772, "1.21.7-1.21.8"),
    MC_1_21_9(773, "1.21.9-1.21.10"),
    MC_1_21_11(774, "1.21.11+"),
    MC_26_1(775, "26.1"),
    MC_26_2(776, "26.2");

    private final int protocol;
    private final String versionName;

    ProtocolVersion(int protocol, String versionName) {
        this.protocol = protocol;
        this.versionName = versionName;
    }

    public static ProtocolVersion fromProtocol(int protocol) {
        for (ProtocolVersion version : values()) {
            if (version.protocol == protocol) {
                return version;
            }
        }
        return UNKNOWN;
    }
}
