package moe.koseirin.nyanruaineo.server.V1Controller;

import moe.koseirin.nyanruaineo.dto.TicketCreateDTO;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.services.impl.TicketFuncImpl;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * V1 用户自助工单端点（提交/查看自己的工单）。
 * <p>
 * 只做「Bearer token → uid」校验（不校验管理权限节点）；同时刻意不挂 {@code AuthenticateCheck}
 * 拦截器，以便被封禁用户仍能提交禁封申诉（type=1）。
 */
@RestController
@RequestMapping("/api/zako/v1/ticket")
public class UserTicketController {

    private final TicketFuncImpl ticketFunc;
    private final utilset utilset;
    private final UserDevicesRepository userDevicesRepository;
    private final Respond respond;

    @Value("${yggdrasil.privateKey}")
    private String privateKey;

    public UserTicketController(TicketFuncImpl ticketFunc, utilset utilset, UserDevicesRepository userDevicesRepository, Respond respond) {
        this.ticketFunc = ticketFunc;
        this.utilset = utilset;
        this.userDevicesRepository = userDevicesRepository;
        this.respond = respond;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody TicketCreateDTO dto) {
        String uid = resolveUid(authorization);
        if (uid == null) {
            return unauthorized();
        }
        return ticketFunc.createTicket(uid, dto.getType(), dto.getDescription());
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String uid = resolveUid(authorization);
        if (uid == null) {
            return unauthorized();
        }
        return ticketFunc.listMyTickets(uid);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<?> get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String ticketId) {
        String uid = resolveUid(authorization);
        if (uid == null) {
            return unauthorized();
        }
        return ticketFunc.getMyTicket(uid, ticketId);
    }

    private String resolveUid(String authorization) {
        if (authorization == null) {
            return null;
        }
        String raw = authorization.replace("Bearer ", "").trim();
        if (raw.isEmpty() || "undefined".equals(raw)) {
            return null;
        }
        String token = utilset.decrypt(raw, privateKey);
        if (token == null) {
            return null;
        }
        return userDevicesRepository.findUidByToken(token);
    }

    private ResponseEntity<?> unauthorized() {
        return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "Unauthorized", "timestamp", LocalDateTime.now());
    }
}
