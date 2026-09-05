package moe.koseirin.nyanruaineo.services;

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import moe.koseirin.nyanruaineo.dto.UserDevicesJson;
import moe.koseirin.nyanruaineo.utils.PasswordHasher;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.UserDevicesServiceImpl;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/*
 * @author KoseiRin_
 * awa
 */
@Service
public class UserSecurityServices {


    @Value("${yggdrasil.privateKey}")
    public String privateKey;

    private final UserDevicesRepository userDevicesRepository;
    private final Respond respond;
    private final UserPermissionsRepository userPermissionsRepository;
    private final AccountsRepository accountsRepository;
    private final BanUserRepository banUserRepository;
    private final UserDevicesServiceImpl userDevicesServiceImpl;
    private final utilset utilset;
    private final PasswordHasher passwordHasher;


    public UserSecurityServices(UserDevicesRepository userDevicesRepository, UserPermissionsRepository userPermissionsRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository, utilset utilset, Respond respond, UserDevicesServiceImpl userDevicesServiceImpl, PasswordHasher passwordHasher) {
        this.userDevicesRepository = userDevicesRepository;
        this.userPermissionsRepository = userPermissionsRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
        this.respond = respond;
        this.userDevicesServiceImpl = userDevicesServiceImpl;
        this.passwordHasher = passwordHasher;
    }

    public ResponseEntity<?> Open2FA(HttpServletRequest request, String password) {
        Accounts accounts = GetUser(request);
        if (accounts == null) {
            return respond.respond(MediaType.APPLICATION_JSON,500, "message","未知登录绕过喵！！！","timestamp", LocalDateTime.now());
        }
        if (banUserRepository.existsByUidAndIsActiveTrue(accounts.getUid())) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","账户状态异常，资料为只读，无法修改喵!","timestamp", LocalDateTime.now());
        }
        if (accounts.getSecretKey() != null) {
            return ResponseEntity.status(400).build();
        }
        // 二次确认：开启 2FA 前校验当前密码，防止仅凭 token 就给他人的账号开启 2FA
        if (password == null || password.isBlank() || !passwordHasher.matches(password, accounts.getPassword())) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","密码错误","timestamp", LocalDateTime.now());
        }
        String key = utilset.generateSecretKey();
        String QrCode = utilset.getQRBarcode(accounts.getUid(), key);
        accountsRepository.UpdateSecretKey(key, accounts.getUid());
        return respond.respond(MediaType.APPLICATION_JSON, 200, "url", QrCode, "status", true);
    }

    public ResponseEntity<?> Close2FA(HttpServletRequest request, String code) {
        Accounts accounts = GetUser(request);
        if (accounts == null) {
            return respond.respond(MediaType.APPLICATION_JSON,500, "message","未知登录绕过喵！！！","timestamp", LocalDateTime.now());
        }
        if (banUserRepository.existsByUidAndIsActiveTrue(accounts.getUid())) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","账户状态异常，资料为只读，无法修改喵!","timestamp", LocalDateTime.now());
        }
        if (accounts.getSecretKey() == null) {
            return ResponseEntity.status(400).build();
        }
        // 二次确认：关闭 2FA 前校验当前 TOTP 码，防止仅凭 token 就关闭他人 2FA
        if (code == null || !code.matches("[0-9]{3,7}") || !utilset.checkCode(accounts.getSecretKey(), Integer.parseInt(code))) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","2FA验证码错误","timestamp", LocalDateTime.now());
        }
        accountsRepository.DeleteSecretKey(accounts.getUid());
        return ResponseEntity.status(200).build();
    }

    private Accounts GetUser(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String rawToken = authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(rawToken, privateKey);
        if (Token == null) {
            return null;
        }
        String uid = userDevicesRepository.findUidByToken(Token);
        return accountsRepository.GetUser(uid);
    }

    public ResponseEntity<?> GetDevices(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","Unauthorized","timestamp", LocalDateTime.now());
        }
        String raw = authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw,privateKey);
        if (Token == null) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","Unauthorized","timestamp", LocalDateTime.now());
        }
        String uid = userDevicesRepository.findUidByToken(Token);
        List<UserDevicesJson> result = userDevicesServiceImpl.GetDevices(uid);
        if (result.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404,"error","NotFound","message","Not Found Account","timestamp", LocalDateTime.now());
        }else {
            return respond.respond(MediaType.APPLICATION_JSON,200,result);
        }
    }
    public ResponseEntity<?> DeleteDevices(String value, HttpServletRequest request) {
        if (value == null || value.isBlank()) {
            return respond.respond(MediaType.APPLICATION_JSON,404,"error","NotFound","message","Not Found Device","timestamp", LocalDateTime.now());
        }
        // 归属校验：仅允许删除当前登录账号名下的会话，防止删除他人设备（IDOR）
        Accounts accounts = GetUser(request);
        if (accounts == null) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","Unauthorized","timestamp", LocalDateTime.now());
        }
        String sessionOwner = userDevicesRepository.findUidBySession(value);
        if (sessionOwner == null || !sessionOwner.equals(accounts.getUid())) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","无权删除该设备","timestamp", LocalDateTime.now());
        }
        userDevicesRepository.deleteBySession(value);
        return ResponseEntity.ok().build();
    }
}
