package moe.koseirin.nyanruaineo.server.V3Contorller;

import moe.koseirin.nyanruaineo.dto.UserEditDTO;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.services.PermissionService;
import moe.koseirin.nyanruaineo.services.impl.UserManageFuncImpl;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.System.PermissionNodes;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

/** V3 用户管理端点，鉴权沿用 Bearer token + {@code nyanid.admin.user} 权限节点。 */
@RestController
@RequestMapping("/api/zako/v3/users")
public class UserManagerController {

    private final UserManageFuncImpl userManageFunc;
    private final PermissionService permissionService;
    private final utilset utilset;
    private final UserDevicesRepository userDevicesRepository;
    private final BanUserRepository banUserRepository;
    private final Respond respond;

    @Value("${yggdrasil.privateKey}")
    private String privateKey;

    public UserManagerController(UserManageFuncImpl userManageFunc, PermissionService permissionService, utilset utilset, UserDevicesRepository userDevicesRepository, BanUserRepository banUserRepository, Respond respond) {
        this.userManageFunc = userManageFunc;
        this.permissionService = permissionService;
        this.utilset = utilset;
        this.userDevicesRepository = userDevicesRepository;
        this.banUserRepository = banUserRepository;
        this.respond = respond;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.listUsers(keyword, page, size);
    }

    @GetMapping("/banned")
    public ResponseEntity<?> listBanned(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.listBannedUsers(page, size);
    }

    @GetMapping("/developers")
    public ResponseEntity<?> listDevelopers(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.listDevelopers(page, size);
    }

    @GetMapping("/{uid}")
    public ResponseEntity<?> detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String uid) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.getUserDetail(uid);
    }

    @PutMapping("/{uid}")
    public ResponseEntity<?> edit(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable String uid,
                                  @RequestBody UserEditDTO dto) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.editUser(uid, dto);
    }

    @PutMapping("/{uid}/active")
    public ResponseEntity<?> setActive(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable String uid,
                                       @RequestParam boolean active) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.setActive(uid, active);
    }

    @PutMapping("/{uid}/developer")
    public ResponseEntity<?> setDeveloper(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable String uid,
                                          @RequestParam boolean active) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.setDeveloper(uid, active);
    }

    @PostMapping("/{uid}/ban")
    public ResponseEntity<?> ban(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @PathVariable String uid,
                                 @RequestParam(required = false) String reason,
                                 @RequestParam(required = false) Integer type,
                                 @RequestParam(required = false) String expire) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.banUser(uid, reason, type, expire, resolveUid(authorization));
    }

    @DeleteMapping("/{uid}/ban")
    public ResponseEntity<?> unban(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String uid) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return userManageFunc.unbanUser(uid);
    }

    @GetMapping("/{uid}/permissions")
    public ResponseEntity<?> listPermissions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @PathVariable String uid) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        return ResponseEntity.ok(permissionService.getPermissions(uid));
    }

    @PostMapping("/{uid}/permissions")
    public ResponseEntity<?> grant(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable String uid,
                                   @RequestParam String node) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        permissionService.grant(uid, node);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{uid}/permissions")
    public ResponseEntity<?> revoke(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable String uid,
                                    @RequestParam String node) {
        if (!authorized(authorization)) {
            return forbidden();
        }
        permissionService.revoke(uid, node);
        return ResponseEntity.ok().build();
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
                && permissionService.hasPermission(uid, PermissionNodes.USER_ADMIN);
    }

    private ResponseEntity<?> forbidden() {
        return respond.respond(MediaType.APPLICATION_JSON, 403, "message", "Permission denied!", "timestamp", LocalDateTime.now());
    }
}
