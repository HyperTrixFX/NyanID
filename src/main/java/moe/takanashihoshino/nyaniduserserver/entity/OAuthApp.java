package moe.takanashihoshino.nyaniduserserver.entity;


/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Entity
@Setter
@Getter
public class OAuthApp {
    @Id
    @Column(columnDefinition="varchar(64)",nullable = false)
    private String Appid;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String CreateUser;

    private String CallBackUrl;

    @Column(nullable = false)
    private Boolean IsActive;

    @Column(nullable = false)
    private int AppType;

    @Column(nullable = false)
    private int AuthType;//authorization code : 0 ;  implicit: 1 ; resource owner password credentials :2  ;  client credentials :3

    @Column(columnDefinition="varchar(128)",nullable = false)
    private String SecretKey;

    @Column(columnDefinition="varchar(64)",nullable = false)
    private String AppToken;

    @Column(columnDefinition="varchar(150)",nullable = false)
    private String AppName;

    @Column(columnDefinition="text")
    private String AppDesc;


    @Override
    public int hashCode() {
        return UUID.nameUUIDFromBytes((Appid+AppToken).getBytes(StandardCharsets.UTF_8)).hashCode();
    }
}
