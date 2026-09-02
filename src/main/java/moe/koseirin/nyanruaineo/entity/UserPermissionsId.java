package moe.koseirin.nyanruaineo.entity;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link UserPermissions} 的复合主键：一个用户（{@code uid}）可以被授予多个权限节点（{@code permission}），
 * 因此主键由 {@code uid} + {@code permission} 两列组成。
 */
@Setter
@Getter
public class UserPermissionsId implements Serializable {

    private String uid;
    private String permission;

    public UserPermissionsId() {
    }

    public UserPermissionsId(String uid, String permission) {
        this.uid = uid;
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserPermissionsId that)) {
            return false;
        }
        return Objects.equals(uid, that.uid) && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, permission);
    }
}
