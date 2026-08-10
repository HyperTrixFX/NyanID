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

@Entity
@Getter
@Setter
public class UserDevices {

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String ClientId;

    @Column(columnDefinition="varchar(10)",nullable = false)
    private String DeviceName;

    @Column(columnDefinition="varchar(52)",nullable = false)
    private String DeviceID;

    @Column(columnDefinition="varchar(64)",nullable = false)
    private String Token;

    @Column(columnDefinition="varchar(25)",nullable = false)
    private String Ip;

    @Column(columnDefinition="varchar(150)",nullable = false)
    private String Session;

    @Column(nullable = false)
    private boolean IsActive;

    @Column(columnDefinition="varchar(32)")
    private String HardwareID;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    @Column(nullable = false)
    private LocalDateTime CreateTime;
}
