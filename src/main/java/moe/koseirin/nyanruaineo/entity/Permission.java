package moe.koseirin.nyanruaineo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 权限节点目录。每行定义一个已知的权限节点（{@code name}）以及一个可选的数值代号 {@code code}。
 * <p>
 * {@code code} 的约定：{@link #CODE_ROOT root=-1}、{@link #CODE_PROXY proxy=1354}、
 * {@link #CODE_DEFAULT d_user=0}、{@link #CODE_ADMIN_USER a_user=1}。其中 {@code -1}（root）是超级管理员标识。
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    public static final int CODE_ROOT = -1;
    public static final int CODE_PROXY = 1354;
    public static final int CODE_DEFAULT = 0;
    public static final int CODE_ADMIN_USER = 1;

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private int code; //root:-1 proxy:1354 d_user:0(Can only access the user's private information but cannot modify it) a_user:1

    @Column(columnDefinition = "varchar(100)")
    private String name;

    @Column(nullable = false)
    private LocalDateTime CreatedAt;

}
