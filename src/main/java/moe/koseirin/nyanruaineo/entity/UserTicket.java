package moe.koseirin.nyanruaineo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.utils.System.EnumList.TicketStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/*
 * @author KoseiRin_
 * awa
 */

@Entity
@Getter
@Setter
public class UserTicket {

    @Id
    private String TicketId;

    @Column(nullable = false)
    private int type; //1禁封申诉，2开发者申请，3账号安全申诉

    @Column(columnDefinition="varchar(155)",nullable = false)
    private int Description;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String userid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String handlerUid;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

}
