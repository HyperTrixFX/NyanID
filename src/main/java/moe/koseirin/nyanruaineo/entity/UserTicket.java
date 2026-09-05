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

    /** 工单类型：禁封申诉。 */
    public static final int TYPE_BAN_APPEAL = 1;
    /** 工单类型：开发者申请。 */
    public static final int TYPE_DEV_APPLY = 2;
    /** 工单类型：账号安全申诉。 */
    public static final int TYPE_ACCOUNT_SECURITY = 3;

    @Id
    private String TicketId;

    /** 工单类型，见 {@link #TYPE_BAN_APPEAL} / {@link #TYPE_DEV_APPLY} / {@link #TYPE_ACCOUNT_SECURITY}。 */
    @Column(nullable = false)
    private int type;

    @Column(columnDefinition="varchar(155)",nullable = false)
    private String Description;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String userid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 处理人 UID；未指派时为 {@code null}。 */
    @Column(columnDefinition="varchar(32)")
    private String handlerUid;

    /** 管理员回复；未回复时为 {@code null}。 */
    @Column(columnDefinition="varchar(512)")
    private String reply;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;

}
