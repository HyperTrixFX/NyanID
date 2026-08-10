package moe.koseirin.nyanruaineo.entity;

/*
 * @author KoseiRin_
 * awa
 */


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

//mysql
@Entity
@Getter
@Setter
public class BanUserList{

    @Id
    @Column(columnDefinition="varchar(13)",nullable = false)
    private String BanID;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    @Column(nullable = false)
    private String Reason;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private int Type;  //1限制修改昵称//2限制从Oauth登录//3限制修改头像及头图//4门户锁//5禁止登录游戏(可登录网站)//6死ban//-1安全警告

    @Column(nullable = false)
    private LocalDateTime BanTime;

    @Column(nullable = false)
    private String BannedBy;


}
