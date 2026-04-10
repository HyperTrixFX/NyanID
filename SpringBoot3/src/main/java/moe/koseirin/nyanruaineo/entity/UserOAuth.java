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

@Setter
@Getter
@Entity
public class UserOAuth {
    @Id
    @Column(columnDefinition="varchar(64)",nullable = false)
    private String Appid;

    @Column(columnDefinition="varchar(150)",nullable = false)
    private String ClientID;

    @Column(nullable = false)
    private Boolean IsActive;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    @Column(nullable = false)
    private int Permission; //1: All , 2: Only uid , 3:email,uid,username,nickname

    @Column(columnDefinition="varchar(128)",nullable = false)
    private String AccessToken;
}
