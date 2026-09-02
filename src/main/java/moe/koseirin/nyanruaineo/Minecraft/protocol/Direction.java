package moe.koseirin.nyanruaineo.Minecraft.protocol;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 数据包方向。
 */
public enum Direction {

    /**
     * 从客户端流向服务器的数据包（即在前端通道为入站，在后端通道为出站）。
     */
    TO_SERVER,

    /**
     * 从服务器流向客户端的数据包（即在前端通道为出站，在后端通道为入站）。
     */
    TO_CLIENT
}
