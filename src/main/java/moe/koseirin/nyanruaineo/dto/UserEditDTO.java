package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */

/** 管理面板编辑用户资料的可选字段，只更新传入的非空字段。 */
@Getter
@Setter
public class UserEditDTO {
    private String nickname;
    private String description;
    private Integer exp;
    private Boolean isDeveloper;
}
