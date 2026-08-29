package moe.koseirin.nyanruaineo.Minecraft.config.cfg;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Data;
import moe.koseirin.nyanruaineo.Minecraft.config.ProxyProperties;

import java.util.List;

@Data
public class MotdConfig {
    private List<String> lines;

    private List<String> hoverLines;

    private boolean fakePlayersEnabled;
    private int fakePlayersMin;
    private int fakePlayersMax;
    private int fakePlayersIncrement;

    private int maxPlayers;

    private String versionName;
    private int protocolVersion;

    private List<MotdEntry> randomMotds;

    private boolean rgbSupport;
}