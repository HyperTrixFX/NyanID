package moe.koseirin.nyanruaineo.Minecraft.util;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.MotdConfig;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PlaceholderResolver {
    private final Random random = new Random();
    private final AtomicInteger fakePlayerCounter = new AtomicInteger(0);

    private int realOnline = 0;
    private int fakePlayerCount = 0;
    private int fakePlayerBase = 0;

    public void updateRealOnline(int online) {
        this.realOnline = online;
    }

    public String resolve(String text, MotdConfig config) {
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("%online%", String.valueOf(realOnline));

        if (config.isFakePlayersEnabled()) {
            if (fakePlayerCounter.getAndIncrement() % config.getFakePlayersIncrement() == 0) {
                fakePlayerBase = config.getFakePlayersMin() +
                        random.nextInt(config.getFakePlayersMax() - config.getFakePlayersMin() + 1);
            }
            fakePlayerCount = fakePlayerBase + (int)(Math.sin(System.currentTimeMillis() / 10000.0) * 5);
            placeholders.put("%fake_online%", String.valueOf(fakePlayerCount));
        } else {
            placeholders.put("%fake_online%", "0");
        }

        placeholders.put("%total_online%", String.valueOf(realOnline + fakePlayerCount));

        placeholders.put("%max%", config.getMaxPlayers() > 0 ?
                String.valueOf(config.getMaxPlayers()) : String.valueOf(realOnline + 20));

        placeholders.put("%server%", "Minecraft Proxy");

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
