package moe.koseirin.nyanruaineo.entity;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//mongodb
@Entity
@Setter
@Getter
public class NyanIDuser {

    @Id
    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    private int exp;

    @Column(nullable = false)
    private boolean IsDeveloper;

    private String Description;

    private String nickname;

    private Boolean IsGIFAvatar;

    private Boolean EnableGIFAvatar;

    private int GIFAvatarID;
}
