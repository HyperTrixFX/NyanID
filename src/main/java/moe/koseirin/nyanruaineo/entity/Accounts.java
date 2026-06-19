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

import java.time.LocalDateTime;



//mysql
@Entity
@Setter
@Getter
public class Accounts{

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String uid;

    @Column
    private String password;

    @Column
    private String email;

    @Column
    private String bind;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Boolean isActive;

    @Column
    private String SecretKey;

    @Column
    private String MicrosoftAccount;

    @Column
    private String GithubAccessToken;

    @Column
    private LocalDateTime RegisterTime;


}
