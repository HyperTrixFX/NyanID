package moe.koseirin.nyanruaineo.server.V3Contorller;

import moe.koseirin.nyanruaineo.dto.TicketHandleDTO;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.services.PermissionService;
import moe.koseirin.nyanruaineo.services.impl.TicketFuncImpl;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.System.EnumList.TicketStatus;
import moe.koseirin.nyanruaineo.utils.System.PermissionNodes;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/*
 * @author KoseiRin_
 * awa
 */

/** V3 工单管理端点（管理员），鉴权沿用 Bearer token + {@code nyanid.admin.ticket} 权限节点。 */
@RestController
@RequestMapping("/api/zako/v3/tickets")
public class TicketController {

    private final TicketFuncImpl ticketFunc;
    private final PermissionService permissionService;
    private final utilset utilset;
    private final UserDevicesRepository userDevicesRepository;
    private final BanUserRepository banUserRepository;
    private final Respond respond;

    @Value("${yggdrasil.privateKey}")
    private String privateKey;

    public TicketController(TicketFuncImpl ticketFunc, PermissionService permissionService, utilset utilset, UserDevicesRepository userDevicesRepository, BanUserRepository banUserRepository, Respond respond) {
        this.ticketFunc = ticketFunc;
        this.permissionService = permissionService;
        this.utilset = utilset;
        this.userDevicesRepository = userDevicesRepository;
        this.banUserRepository = banUserRepository;
        this.respond = respond;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam(required = false) TicketStatus status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return ticketFunc.listTickets(status, page, size);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<?> get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String ticketId) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return ticketFunc.getTicket(ticketId);
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<?> handle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String ticketId,
                                    @RequestBody TicketHandleDTO dto) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return ticketFunc.handleTicket(ticketId, dto.getStatus(), dto.getHandlerUid(), dto.getReply());
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

    private boolean authorized(String authorization) {
        String uid = resolveUid(authorization);
        return uid != null && !banUserRepository.existsByUidAndIsActiveTrue(uid)
                && permissionService.hasPermission(uid, PermissionNodes.TICKET_ADMIN);
    }

    private ResponseEntity<?> forbidden() {
        return respond.respond(MediaType.APPLICATION_JSON, 403, "message", "Permission denied!", "timestamp", LocalDateTime.now());
    }
}
