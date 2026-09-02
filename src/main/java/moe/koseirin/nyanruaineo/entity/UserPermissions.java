package moe.koseirin.nyanruaineo.entity;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * 用户被授予的权限节点（一条记录 = 一个用户拥有一个节点）。
 * <p>
 * 复合主键 {@code (uid, permission)}：一个用户可以同时拥有多个权限节点；{@code permission}
 * 直接存权限节点字符串（与 {@link Permission} 一致，例如 {@code minecraftproxy.command.server}
 * 或 {@code *}）。
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserPermissionsId.class)
public class UserPermissions {

    @Id
    @Column(columnDefinition = "varchar(32)", nullable = false)
    private String uid;

    @Id
    @Column(columnDefinition = "varchar(100)", nullable = false)
    private String permission;

    @Column(nullable = false)
    private LocalDateTime GrantedAt;
}
