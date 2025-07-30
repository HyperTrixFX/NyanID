package moe.takanashihoshino.nyaniduserserver.server.web.Public;


import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Accounts;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.NyanIDuser;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.BanUserRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.NyanIDuserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v2/userinfo/{uuid}")
public class GetPublicUserInfo {

    private final NyanIDuserRepository nyanIDuserRepository;
    private final AccountsRepository accountsRepository;
    private final BanUserRepository banUserRepository;

    public GetPublicUserInfo(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
    }


    @GetMapping(produces = "application/json")
    public Object GETMethod(@PathVariable String uuid, HttpServletResponse response) {
        if (banUserRepository.findBanIDByUid(uuid) == null ) {
        if (accountsRepository.GetUser(uuid) != null){
        Accounts accounts = accountsRepository.GetUser(uuid);
        NyanIDuser user = nyanIDuserRepository.getUser(accounts.getUid());
        int exp = user.getExp();
        Boolean isDeveloper = user.isIsDeveloper() ? true : false;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname",user.getNickname());
        jsonObject.put("exp",exp);
        jsonObject.put("description",user.getDescription());
        jsonObject.put("username",accounts.getUsername());
        jsonObject.put("isDeveloper",isDeveloper);
        jsonObject.put("uid",accounts.getUid());
        if (banUserRepository.COUNTByUid(user.getUid()) !=  0){
            jsonObject.put("ViolationHistory",true);
            jsonObject.put("NACCOUNT",banUserRepository.COUNTByUid(user.getUid()));
        }
        return jsonObject;
        }else {
            return ErrRes.NotFoundAccountException("该用户不存在喵！",response);
        }
    }else {
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("nickname","该用户已被禁封");
            jsonObject1.put("exp",-1000);
            jsonObject1.put("description","该用户因为违反我们的服务条款或账号准则已被NAC冻结. ");
            jsonObject1.put("username","Neko-AntiCheat BANNED");
            jsonObject1.put("isDeveloper",false);
            jsonObject1.put("uid",uuid);
            return jsonObject1;

        }
    }
}
