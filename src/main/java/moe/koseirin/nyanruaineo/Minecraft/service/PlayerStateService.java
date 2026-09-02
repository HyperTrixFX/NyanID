package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.BossBar;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.ScoreboardObjective;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.ScoreboardScore;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.Team;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 代理端为每个玩家跟踪的服务端状态：
 * 客户端/服务端实体 ID、当前维度以及服务端发送的计分板目标、分数、队伍和 Boss 条。
 * 在切换服务器时，{@link #clearServerState} 会发出移除数据包，
 * 使客户端不会将旧服务器的状态带入新世界。
 */
@Slf4j
@Service
public class PlayerStateService {

    private final Map<UserConnection, State> states = new ConcurrentHashMap<>();

    private static final class State {

        int clientEntityId;
        int serverEntityId;
        Object dimension;
        boolean dimensionChange;
        final Set<UUID> sentBossBars = ConcurrentHashMap.newKeySet();
        final Set<String> objectives = ConcurrentHashMap.newKeySet();
        final Set<Score> scores = ConcurrentHashMap.newKeySet();
        final Set<String> teams = ConcurrentHashMap.newKeySet();
    }

    private record Score(String itemName, String scoreName) {
    }

    private State state(UserConnection user) {
        return states.computeIfAbsent(user, key -> new State());
    }

    /** Releases the state of a disconnected player. */
    public void remove(UserConnection user) {
        states.remove(user);
    }

    public int getClientEntityId(UserConnection user) {
        return state(user).clientEntityId;
    }

    public void setClientEntityId(UserConnection user, int entityId) {
        state(user).clientEntityId = entityId;
    }

    public int getServerEntityId(UserConnection user) {
        return state(user).serverEntityId;
    }

    public void setServerEntityId(UserConnection user, int entityId) {
        state(user).serverEntityId = entityId;
    }

    public Object getDimension(UserConnection user) {
        return state(user).dimension;
    }

    public void setDimension(UserConnection user, Object dimension) {
        state(user).dimension = dimension;
    }

    public boolean isDimensionChange(UserConnection user) {
        return state(user).dimensionChange;
    }

    public void setDimensionChange(UserConnection user, boolean dimensionChange) {
        state(user).dimensionChange = dimensionChange;
    }

    /** Tracks a boss bar add/remove sent by the backend. */
    public void trackBossBar(UserConnection user, UUID uuid, boolean add) {
        if (add) {
            state(user).sentBossBars.add(uuid);
        } else {
            state(user).sentBossBars.remove(uuid);
        }
    }

    /** Tracks a scoreboard objective create/remove sent by the backend. */
    public void trackObjective(UserConnection user, String name, boolean add) {
        if (add) {
            state(user).objectives.add(name);
        } else {
            state(user).objectives.remove(name);
        }
    }

    /** Tracks a scoreboard score create/remove sent by the backend. */
    public void trackScore(UserConnection user, String itemName, String scoreName, boolean add) {
        if (add) {
            state(user).scores.add(new Score(itemName, scoreName));
        } else {
            state(user).scores.remove(new Score(itemName, scoreName));
        }
    }

    /** Tracks a scoreboard team create/remove sent by the backend. */
    public void trackTeam(UserConnection user, String name, boolean add) {
        if (add) {
            state(user).teams.add(name);
        } else {
            state(user).teams.remove(name);
        }
    }

    /**
     * 通过给定的接收器（通常是 {@code user::sendPacket}）发出针对旧服务器发送的所有内容
     * （计分板目标/分数、队伍、Boss 条）的移除数据包，并清除跟踪的状态。
     */
    public void clearServerState(UserConnection user, Consumer<DefinedPacket> sink) {
        State state = state(user);
        for (String objective : Set.copyOf(state.objectives)) {
            sink.accept(new ScoreboardObjective(objective, "", "integer", 0, (byte) 1));
            state.objectives.remove(objective);
        }
        for (Score score : Set.copyOf(state.scores)) {
            sink.accept(new ScoreboardScore(score.itemName(), (byte) 1, score.scoreName(), 0));
            state.scores.remove(score);
        }
        for (String team : Set.copyOf(state.teams)) {
            sink.accept(new Team(team));
            state.teams.remove(team);
        }
        for (UUID bossBar : Set.copyOf(state.sentBossBars)) {
            sink.accept(new BossBar(bossBar, 1));
            state.sentBossBars.remove(bossBar);
        }
        state.objectives.clear();
        state.scores.clear();
        state.teams.clear();
        state.sentBossBars.clear();
    }
}
