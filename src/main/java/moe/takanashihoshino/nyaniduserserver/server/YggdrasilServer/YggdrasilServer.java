package moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonLinks;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonMeta;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonRoot;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.Reload.Refreshable;
import moe.takanashihoshino.nyaniduserserver.entity.Accounts;
import moe.takanashihoshino.nyaniduserserver.repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.repository.UserDevicesRepository;
import moe.takanashihoshino.nyaniduserserver.repository.YggdrasilRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.YggdrasilPlayerService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.YggdrasilService;
import moe.takanashihoshino.nyaniduserserver.entity.Yggdrasil;
import moe.takanashihoshino.nyaniduserserver.entity.YggdrasilPlayer;
import moe.takanashihoshino.nyaniduserserver.utils.UUIDUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Refreshable
@RestController
@RequestMapping("api/yggdrasil")
public class YggdrasilServer {

    @Value("${yggdrasil.serverName}")
    private String serverName;

    @Value("${yggdrasil.implementationName}")
    private String implementationName;

    @Value("${yggdrasil.implementationVersion}")
    private String implementationVersion;

    @Value("${yggdrasil.links-homepage}")
    private String links_homepage;

    @Value("${yggdrasil.links-register}")
    private String links_register;

    @Value("${yggdrasil.feature-non_email_login}")
    private boolean feature_non_email_login;

    @Value("${yggdrasil.SkinDomains}")
    private String  SkinDomains;

    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    private final UserDevicesRepository userDevicesRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final AccountsRepository accountsRepository;
    private final YggdrasilService yggdrasilService;
    private final YggdrasilPlayerService yggdrasilPlayerService;

    public YggdrasilServer(UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, AccountsRepository accountsRepository, YggdrasilService yggdrasilService, YggdrasilPlayerService yggdrasilPlayerService) {
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.accountsRepository = accountsRepository;
        this.yggdrasilService = yggdrasilService;
        this.yggdrasilPlayerService = yggdrasilPlayerService;
    }

    @GetMapping({"","/"})
    public Object GetMethod(HttpServletResponse response, HttpServletRequest request){
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String requestURL = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        String fullURL = requestURL + (queryString != null ? "?" + queryString : "");
        response.setHeader("X-Authlib-Injector-API-Location", fullURL);
        //
        YggdrasilServerJsonLinks links = new YggdrasilServerJsonLinks();
        links.setHomepage(links_homepage);
        links.setRegister(links_register);
        //
        YggdrasilServerJsonMeta meta = new YggdrasilServerJsonMeta();
        meta.setImplementationName(implementationName);
        meta.setImplementationVersion(implementationVersion);
        meta.setServerName(serverName);
        meta.setLinks(links);
        meta.setFeature_non_email_login(feature_non_email_login);
        //
        YggdrasilServerJsonRoot root = new YggdrasilServerJsonRoot();
        root.setMeta(meta);
        root.setSkinDomains(new String[]{SkinDomains});
        root.setSignaturePublickey(publicKey);
        return root;
    }

    @PostMapping("open/account")
    public  Object PostMethod(HttpServletResponse response, HttpServletRequest request){
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
        Accounts accounts = accountsRepository.GetUser(uid);
        if (uuid == null) {
            String UUID = UUIDUtil.generateUUIDbutIsOfflineMethod(uid);
            Yggdrasil yggdrasil = new Yggdrasil();
            yggdrasil.setUseSkin(false);
            yggdrasil.setUseCAPE(false);
            yggdrasil.setPlayername(accounts.getUsername());
            yggdrasil.setNyanuid(uid);
            yggdrasil.setUuid(UUID);
            yggdrasil.setType(1);
            yggdrasilService.save(yggdrasil);
            YggdrasilPlayer yggdrasilPlayer = new YggdrasilPlayer();
            yggdrasilPlayer.setUuid(UUID);
            yggdrasilPlayer.setSkinTexturesType(1);
            yggdrasilPlayer.setSkinTexturesHash(null);
            yggdrasilPlayer.setCAPETexturesHash(null);
            yggdrasilPlayerService.save(yggdrasilPlayer);
            response.setStatus(204);
            return  null;
        }else {
            return  ErrRes.IllegalRequestException("RequestBody  is NULL  MiaoWu~",response);
        }
    }
}
