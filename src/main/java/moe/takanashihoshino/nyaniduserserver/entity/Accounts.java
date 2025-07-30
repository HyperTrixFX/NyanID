package moe.takanashihoshino.nyaniduserserver.entity;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Setter
@Getter
public class Accounts{

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String uid;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column
    private String bind;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Boolean isActive;

    @Column
    private String SecretKey;

}
