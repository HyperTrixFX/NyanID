package moe.koseirin.nyanruaineo.services.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.entity.UserTicket;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.repository.UserTicketRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.System.EnumList.TicketStatus;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 工单业务逻辑。V1 用户自助（提交/查自己）与 V3 管理（列表/查看/处理）共用本服务，
 * 权限鉴权由各自的 Controller 负责。
 */
@Service
public class TicketFuncImpl {

    private final UserTicketRepository ticketRepository;
    private final AccountsRepository accountsRepository;
    private final BanUserRepository banUserRepository;
    private final NyanIDuserRepository nyanIDuserRepository;
    private final utilset utilset;
    private final Respond respond;

    public TicketFuncImpl(UserTicketRepository ticketRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository, NyanIDuserRepository nyanIDuserRepository, utilset utilset, Respond respond) {
        this.ticketRepository = ticketRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.utilset = utilset;
        this.respond = respond;
    }

    /** 用户提交工单。 */
    @Transactional
    public ResponseEntity<?> createTicket(String userid, Integer type, String description) {
        if (type == null || !isValidType(type)) {
            return respond.respond(MediaType.APPLICATION_JSON, 400, "message", "Invalid ticket type", "timestamp", LocalDateTime.now());
        }
        if (description == null || description.isBlank()) {
            return respond.respond(MediaType.APPLICATION_JSON, 400, "message", "Description is required", "timestamp", LocalDateTime.now());
        }
        if (description.length() > 155) {
            return respond.respond(MediaType.APPLICATION_JSON, 400, "message", "Description too long (max 155)", "timestamp", LocalDateTime.now());
        }
        if (accountsRepository.GetUser(userid) == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 404, "message", "Not Found Account", "timestamp", LocalDateTime.now());
        }

        // 禁封申诉：账户必须存在活跃异常（前置条件）
        if (type == UserTicket.TYPE_BAN_APPEAL && !banUserRepository.existsByUidAndIsActiveTrue(userid)) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, "message", "账户不存在活跃异常，无法申请禁封申诉", "timestamp", LocalDateTime.now());
        }

        // 开发者申请：已是开发者则禁止重复申请
        if (type == UserTicket.TYPE_DEV_APPLY) {
            NyanIDuser profile = nyanIDuserRepository.getUser(userid);
            if (profile != null && profile.isIsDeveloper()) {
                return respond.respond(MediaType.APPLICATION_JSON, 409, "message", "您已是开发者，无需重复申请", "timestamp", LocalDateTime.now());
            }
        }

        // 所有类型：已有未结束的同类型工单时禁止重复申请
        if (hasUnfinishedTicket(userid, type)) {
            return respond.respond(MediaType.APPLICATION_JSON, 409, "message", "已有未结束的" + typeName(type) + "工单，请等待处理完成", "timestamp", LocalDateTime.now());
        }

        UserTicket ticket = new UserTicket();
        ticket.setTicketId(utilset.RandomString(13));
        ticket.setType(type);
        ticket.setDescription(description);
        ticket.setUserid(userid);
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setHandlerUid(null);
        UserTicket saved = ticketRepository.save(ticket);
        return respond.respond(MediaType.APPLICATION_JSON, 200, "message", "Ticket created", "ticketId", saved.getTicketId(), "timestamp", LocalDateTime.now());
    }

    /** 用户查看自己的全部工单。 */
    @Transactional(readOnly = true)
    public ResponseEntity<?> listMyTickets(String userid) {
        List<UserTicket> tickets = ticketRepository.findByUseridOrderByCreatedAtDesc(userid);
        JSONArray items = new JSONArray();
        for (UserTicket t : tickets) {
            items.add(toTicketJson(t));
        }
        JSONObject body = new JSONObject();
        body.put("total", items.size());
        body.put("items", items);
        return respond.respond(MediaType.APPLICATION_JSON, 200, body);
    }

    /** 用户查看自己的一单工单（校验归属）。 */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyTicket(String userid, String ticketId) {
        UserTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null || !userid.equals(ticket.getUserid())) {
            return respond.respond(MediaType.APPLICATION_JSON, 404, "message", "Not Found Ticket", "timestamp", LocalDateTime.now());
        }
        return respond.respond(MediaType.APPLICATION_JSON, 200, toTicketJson(ticket));
    }

    /** 管理面板：分页列出工单，可按状态过滤。 */
    @Transactional(readOnly = true)
    public ResponseEntity<?> listTickets(TicketStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampSize(size), Sort.by("createdAt").descending());
        Page<UserTicket> result = (status == null)
                ? ticketRepository.findAll(pageable)
                : ticketRepository.findByStatus(status, pageable);

        JSONObject body = new JSONObject();
        body.put("total", result.getTotalElements());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        JSONArray items = new JSONArray();
        for (UserTicket t : result.getContent()) {
            items.add(toTicketJson(t));
        }
        body.put("items", items);
        return respond.respond(MediaType.APPLICATION_JSON, 200, body);
    }

    /** 管理面板：查看一单工单。 */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTicket(String ticketId) {
        UserTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 404, "message", "Not Found Ticket", "timestamp", LocalDateTime.now());
        }
        return respond.respond(MediaType.APPLICATION_JSON, 200, toTicketJson(ticket));
    }

    /** 管理面板：更新状态 / 指派处理人 / 回复。PENDING / PROCESSING 可处理，终态锁定。 */
    @Transactional
    public ResponseEntity<?> handleTicket(String ticketId, String status, String handlerUid, String reply) {
        UserTicket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 404, "message", "Not Found Ticket", "timestamp", LocalDateTime.now());
        }
        // 已结束（终态：APPROVED / REJECTED / CLOSED）锁定；PENDING / PROCESSING 允许再次修改
        if (ticket.getStatus() == null || ticket.getStatus().isFinished()) {
            return respond.respond(MediaType.APPLICATION_JSON, 409, "message", "工单已处理，无法再次修改", "timestamp", LocalDateTime.now());
        }
        if (status != null && !status.isBlank()) {
            try {
                ticket.setStatus(TicketStatus.valueOf(status.trim()));
            } catch (IllegalArgumentException e) {
                return respond.respond(MediaType.APPLICATION_JSON, 400, "message", "Invalid ticket status", "timestamp", LocalDateTime.now());
            }
        }
        if (handlerUid != null && !handlerUid.isBlank()) {
            ticket.setHandlerUid(handlerUid);
        }
        if (reply != null && !reply.isBlank()) {
            if (reply.length() > 512) {
                return respond.respond(MediaType.APPLICATION_JSON, 400, "message", "Reply too long (max 512)", "timestamp", LocalDateTime.now());
            }
            ticket.setReply(reply);
        }
        ticketRepository.save(ticket);
        return respond.respond(MediaType.APPLICATION_JSON, 200, toTicketJson(ticket));
    }

    private JSONObject toTicketJson(UserTicket t) {
        JSONObject o = new JSONObject();
        o.put("ticketId", t.getTicketId());
        o.put("type", t.getType());
        o.put("description", t.getDescription());
        o.put("userid", t.getUserid());
        o.put("status", t.getStatus() == null ? null : t.getStatus().name());
        o.put("createdAt", t.getCreatedAt());
        o.put("handlerUid", t.getHandlerUid());
        o.put("reply", t.getReply());
        o.put("updatedAt", t.getUpdatedAt());
        return o;
    }

    private boolean hasUnfinishedTicket(String userid, int type) {
        return ticketRepository.findByUseridAndType(userid, type).stream()
                .anyMatch(t -> t.getStatus() != null && !t.getStatus().isFinished());
    }

    private String typeName(int type) {
        return switch (type) {
            case UserTicket.TYPE_BAN_APPEAL -> "禁封申诉";
            case UserTicket.TYPE_DEV_APPLY -> "开发者申请";
            case UserTicket.TYPE_ACCOUNT_SECURITY -> "账号安全申诉";
            default -> "工单";
        };
    }

    private boolean isValidType(int type) {
        return type == UserTicket.TYPE_BAN_APPEAL || type == UserTicket.TYPE_DEV_APPLY || type == UserTicket.TYPE_ACCOUNT_SECURITY;
    }

    private int clampSize(int size) {
        return Math.clamp(size, 1, 100);
    }
}
