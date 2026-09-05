package moe.koseirin.nyanruaineo.services;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.Permission;
import moe.koseirin.nyanruaineo.entity.UserPermissions;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.utils.System.PermissionNodes;
import moe.koseirin.nyanruaineo.repository.PermissionsRepository;
import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于权限节点的权限管理服务。
 * <p>
 * 权限以「节点字符串」表示（见 {@link PermissionNodes}）。用户（{@code uid}，即 NyanID 账号 UID）
 * 的授权记录存放在 {@link UserPermissions}（一条记录 = 一个用户拥有一个节点）。节点支持：
 * <ul>
 *     <li>精确匹配 {@code minecraftproxy.command.server}</li>
 *     <li>通配匹配 {@code minecraftproxy.command.*}</li>
 *     <li>根节点 {@code *}（拥有全部权限）</li>
 * </ul>
 * Minecraft 玩家通过其 Yggdrasil UUID/AccountBind 解析回 NyanID UID 后再做鉴权。
 */
@Slf4j
@Service
public class PermissionService {

    private final UserPermissionsRepository userPermissionsRepository;
    private final PermissionsRepository permissionsRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final AccountsRepository accountsRepository;

    public PermissionService(UserPermissionsRepository userPermissionsRepository, PermissionsRepository permissionsRepository, YggdrasilRepository yggdrasilRepository, AccountsRepository accountsRepository) {
        this.userPermissionsRepository = userPermissionsRepository;
        this.permissionsRepository = permissionsRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.accountsRepository = accountsRepository;
    }

    /**
     * 通过 Minecraft 玩家 UUID 鉴权。UUID 必须能解析到 NyanID 账号（{@code Yggdrasil.uuid}）。
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID mcUuid, String node) {
        if (mcUuid == null || node == null || node.isBlank()) {
            return false;
        }
        String uid = yggdrasilRepository.findNyanUidByUuid(mcUuid.toString());
        if (uid == null){
            uid = accountsRepository.GetUidByBind(String.valueOf(mcUuid).replace("-",""));
        }
        return uid != null && hasPermission(uid, node);
    }

    /** 判断一个 NyanID 账号是否拥有给定权限节点。 */
    @Transactional(readOnly = true)
    public boolean hasPermission(String uid, String node) {
        if (uid == null || uid.isBlank() || node == null || node.isBlank()) {
            return false;
        }
        String normalized = node.trim();
        Set<String> granted = loadNodes(uid);

        // 根节点拥有全部权限。
        if (granted.contains(PermissionNodes.ROOT)) {
            return true;
        }
        // 精确匹配。
        if (granted.contains(normalized)) {
            return true;
        }
        // 前缀通配，例如 minecraftproxy.* 或 minecraftproxy.command.*。
        for (String n : granted) {
            if (n.endsWith(".*") && normalized.startsWith(n.substring(0, n.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /** 返回该账号被授予的全部权限节点。 */
    @Transactional(readOnly = true)
    public Set<String> getPermissions(String uid) {
        if (uid == null || uid.isBlank()) {
            return Collections.emptySet();
        }
        return loadNodes(uid);
    }

    /** 该账号是否拥有根权限（{@code *}）。 */
    @Transactional(readOnly = true)
    public boolean isRoot(String uid) {
        return uid != null && loadNodes(uid).contains(PermissionNodes.ROOT);
    }

    /** 授予权限节点（幂等），并确保节点已登记到 {@link Permission} 目录。 */
    @Transactional
    public void grant(String uid, String node) {
        if (uid == null || uid.isBlank() || node == null || node.isBlank()) {
            return;
        }
        String normalized = node.trim();
        // 禁止授予根节点 `*` 或通配节点（如 `minecraftproxy.*`），防止有限管理员一步提权为全量超管
        if (PermissionNodes.ROOT.equals(normalized) || normalized.endsWith(".*")) {
            log.warn("Blocked attempt to grant wildcard/root permission '{}' to uid {}", normalized, uid);
            return;
        }
        ensureCatalogEntry(normalized);
        if (!userPermissionsRepository.existsByUidAndPermission(uid, normalized)) {
            UserPermissions grant = new UserPermissions();
            grant.setUid(uid);
            grant.setPermission(normalized);
            grant.setGrantedAt(LocalDateTime.now());
            userPermissionsRepository.save(grant);
            log.info("Granted permission '{}' to uid {}", normalized, uid);
        }
    }

    //只能在终端进行root授权
    @Transactional
    public void grantByCMD(String uid, String node) {
        if (uid == null || uid.isBlank() || node == null || node.isBlank()) {
            return;
        }
        String normalized = node.trim();
        ensureCatalogEntry(normalized);
        if (!userPermissionsRepository.existsByUidAndPermission(uid, normalized)) {
            UserPermissions grant = new UserPermissions();
            grant.setUid(uid);
            grant.setPermission(normalized);
            grant.setGrantedAt(LocalDateTime.now());
            userPermissionsRepository.save(grant);
            log.info("Granted permission '{}' to uid {}", normalized, uid);
        }
    }




    /** 回收某个权限节点。 */
    @Transactional
    public void revoke(String uid, String node) {
        if (uid == null || node == null) {
            return;
        }
        userPermissionsRepository.deleteByUidAndPermission(uid, node.trim());
    }

    /** 回收该账号的全部权限节点。 */
    @Transactional
    public void revokeAll(String uid) {
        if (uid == null) {
            return;
        }
        userPermissionsRepository.deleteByUid(uid);
    }

    /** 清空所有用户的权限授权（保留 {@link Permission} 目录）。 */
    @Transactional
    public void clearAll() {
        userPermissionsRepository.deleteAll();
    }

    private Set<String> loadNodes(String uid) {
        return userPermissionsRepository.findByUid(uid).stream()
                .map(UserPermissions::getPermission)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.toSet());
    }

    /** 将节点登记到目录（如果尚未登记）。根节点 {@code *} 使用 {@link Permission#CODE_ROOT}。 */
    private void ensureCatalogEntry(String node) {
        if (permissionsRepository.findFirstByName(node).isPresent()) {
            return;
        }
        Permission permission = new Permission();
        permission.setId(UUID.randomUUID());
        permission.setName(node);
        permission.setCode(PermissionNodes.ROOT.equals(node) ? Permission.CODE_ROOT : Permission.CODE_DEFAULT);
        permission.setCreatedAt(LocalDateTime.now());
        permissionsRepository.save(permission);
    }
}
