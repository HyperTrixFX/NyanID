package moe.koseirin.nyanruaineo.server.YggdrasilServer;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.Yggdrasil;
import moe.koseirin.nyanruaineo.entity.YggdrasilPlayer;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonLinks;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonMeta;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.YggdrasilServerJson.YggdrasilServerJsonRoot;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.System.EnumList.UUIDtype;
import moe.koseirin.nyanruaineo.utils.SqlService.YggdrasilPlayerService;
import moe.koseirin.nyanruaineo.utils.SqlService.YggdrasilService;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;


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

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    private final UserDevicesRepository userDevicesRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final AccountsRepository accountsRepository;
    private final YggdrasilService yggdrasilService;
    private final YggdrasilPlayerService yggdrasilPlayerService;
    private final utilset utilset;
    private final Respond respond;

    public YggdrasilServer(UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, AccountsRepository accountsRepository, YggdrasilService yggdrasilService, YggdrasilPlayerService yggdrasilPlayerService, utilset utilset, Respond respond) {
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.accountsRepository = accountsRepository;
        this.yggdrasilService = yggdrasilService;
        this.yggdrasilPlayerService = yggdrasilPlayerService;
        this.utilset = utilset;
        this.respond = respond;
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
    public ResponseEntity<?> PostMethod(HttpServletResponse response, HttpServletRequest request){
        String Authorization = request.getHeader("Authorization");
        String rawToken = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(rawToken, privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
        Accounts accounts = accountsRepository.GetUser(uid);
        if (uuid == null) {
            String UUID = utilset.GenerateUUID(UUIDtype.Yggdrasil,false,uid);
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
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","RequestBody is NULL MiaoWu~","timestamp", LocalDateTime.now());

        }
    }
}
