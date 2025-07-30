package moe.takanashihoshino.nyaniduserserver.entity;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class ServerList {

    @Id
    @Column(columnDefinition="varchar(32)",nullable = false)
    private String ServerUid;

    private String ServerName;

    private String Token;

    @Column(columnDefinition="varchar(150)",nullable = false)
    private String SecretKey;

    private String SessionID;
}
