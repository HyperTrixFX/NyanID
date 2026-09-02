package moe.koseirin.nyanruaineo.entity;

/*
 * @author KoseiRin_
 * awa
 */


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;

//mysql
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BanUserList {

    /** 封禁目标：NyanID 账号 UID。 */
    public static final int TARGET_UID = 0;
    /** 封禁目标：Mojang（正版）玩家 UUID。 */
    public static final int TARGET_UUID = 1;

    /** 游戏登录封禁（可登录网站）。 */
    public static final int TYPE_GAME_BAN = 20;
    /** 死封（永久、全站）。 */
    public static final int TYPE_DEAD_BAN = 6;

    @Id
    @Column(columnDefinition = "varchar(13)", nullable = false)
    private String BanID;

    /**
     * 被封禁目标的标识：{@link #TARGET_UID} 时是 NyanID UID（32 位），
     * {@link #TARGET_UUID} 时是 Mojang UUID（去掉连字符的 32 位）。
     */
    @Column(columnDefinition = "varchar(32)", nullable = false)
    private String uid;

    @Column(nullable = false)
    private String Reason;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int Type;

    @Column(nullable = false)
    private LocalDateTime BanTime;

    @Column(nullable = false)
    private String BannedBy;

    /**
     * 封禁目标类型：{@link #TARGET_UID} / {@link #TARGET_UUID}。
     * 可空，空值视为 {@link #TARGET_UID}（兼容旧数据）。
     */
    private Integer TargetType;

    /** 解封时间；{@code null} 表示永久封禁（需要手动或死封）。 */
    private LocalDateTime ExpireTime;

}
