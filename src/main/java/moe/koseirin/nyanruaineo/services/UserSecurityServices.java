package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    private final utilset utilset;


    public UserSecurityServices(UserDevicesRepository userDevicesRepository, UserPermissionsRepository userPermissionsRepository, AccountsRepository accountsRepository, utilset utilset, Respond respond) {
        this.userDevicesRepository = userDevicesRepository;
        this.userPermissionsRepository = userPermissionsRepository;
        this.accountsRepository = accountsRepository;
        this.utilset = utilset;
        this.respond = respond;
    }

    public ResponseEntity<?> Open2FA(HttpServletRequest request) {
        Accounts accounts = GetUser(request);
        if (accounts.getSecretKey() != null) {
            return ResponseEntity.status(400).build();
        }
        String key = utilset.generateSecretKey();
        String QrCode = utilset.getQRBarcode(accounts.getUid(), key);
        accountsRepository.UpdateSecretKey(key, accounts.getUid());
        return respond.respond(MediaType.APPLICATION_JSON, 200, "url", QrCode, "status", true);
    }

    public ResponseEntity<?> Close2FA(HttpServletRequest request) {
        Accounts accounts = GetUser(request);
        if (accounts.getSecretKey() == null) {
            return ResponseEntity.status(400).build();
        }
        accountsRepository.DeleteSecretKey(accounts.getUid());
        return ResponseEntity.status(200).build();
    }

    private Accounts GetUser(HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String rawToken = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(rawToken, privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        Accounts accounts =  accountsRepository.GetUser(uid);
        return accounts;
    }


}
