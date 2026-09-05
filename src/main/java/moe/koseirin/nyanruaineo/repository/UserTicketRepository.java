package moe.koseirin.nyanruaineo.repository;

import moe.koseirin.nyanruaineo.entity.UserTicket;
import moe.koseirin.nyanruaineo.utils.System.EnumList.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * @author KoseiRin_
 * awa
 */

@Repository
public interface UserTicketRepository extends JpaRepository<UserTicket, String> {

    /** 某用户提交的全部工单，按创建时间倒序。 */
    List<UserTicket> findByUseridOrderByCreatedAtDesc(String userid);

    /** 按状态分页查询（管理面板）。 */
    Page<UserTicket> findByStatus(TicketStatus status, Pageable pageable);

    /** 某用户某类型的所有工单（用于「未结束」查重）。 */
    List<UserTicket> findByUseridAndType(String userid, int type);
}
