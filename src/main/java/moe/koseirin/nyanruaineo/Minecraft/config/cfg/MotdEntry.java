package moe.koseirin.nyanruaineo.Minecraft.config.cfg;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Data;

import java.util.List;

@Data
public class MotdEntry {
    private List<String> lines;
    private List<String> hoverLines;
    private int weight;
}
