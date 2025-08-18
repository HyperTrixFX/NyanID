package moe.koseirin.nyanruaineo.server.web.User;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v1/user/2fa")
public class User2FA {


    private final UserDevicesRepository userDevicesRepository;

    private final AccountsRepository accountsRepository;

    public User2FA(UserDevicesRepository userDevicesRepository, AccountsRepository accountsRepository) {
        this.userDevicesRepository = userDevicesRepository;
        this.accountsRepository = accountsRepository;
    }

    @PostMapping("open2fa")
    public Object  Open2fa(HttpServletRequest request, HttpServletResponse response){
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        Accounts accounts =  accountsRepository.GetUser(uid);
        if (accounts.getSecretKey() == null){
            String key = utilset.generateSecretKey();
            String QrCode =utilset.getQRBarcode(accounts.getUid(),key);
            accountsRepository.UpdateSecretKey(key,uid);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("url",QrCode);
            jsonObject.put("status",true);
            return jsonObject;
        }else {
            response.setStatus(400);
            return null;
        }
    }


    @PostMapping("close2fa")
    public Object  Close2fa(HttpServletRequest request, HttpServletResponse response){
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        Accounts accounts =  accountsRepository.GetUser(uid);
        if (accounts.getSecretKey() != null){
            accountsRepository.DeleteSecretKey(uid);
            response.setStatus(204);
            return null;
        }else {
            response.setStatus(400);
            return null;
        }
    }
}
