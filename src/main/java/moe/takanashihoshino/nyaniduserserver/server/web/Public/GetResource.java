package moe.takanashihoshino.nyaniduserserver.server.web.Public;

import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.BanUserRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.NyanIDuserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("api/zako/res/{type}/{data}")
public class GetResource {

    private final BanUserRepository banUserRepository;


    private final NyanIDuserRepository  nyanIDuserRepository;

    public GetResource(BanUserRepository banUserRepository, NyanIDuserRepository nyanIDuserRepository) {
        this.banUserRepository = banUserRepository;
        this.nyanIDuserRepository = nyanIDuserRepository;
    }


    @GetMapping
    public Object GetImgResource(@PathVariable String type, @PathVariable String data, HttpServletResponse response) throws IOException {
        if (banUserRepository.findBanIDByUid(data) == null) {
            switch (type) {
                case "avatar": {
                    if (data.length() == 32) {
                        Boolean IsGIFAvatar = nyanIDuserRepository.IsGIFAvatar(data);
                        Boolean EnableGIFAvatar = nyanIDuserRepository.EnableGIFAvatar(data);
                        if (IsGIFAvatar && EnableGIFAvatar) {
                            int AvatarID = nyanIDuserRepository.GIFAvatarID(data);
                            //这是我给自己挖的第114514个坑，待填坑。。。。
                            Path UserAvatar = Paths.get("Data/GIFAvatar/" + AvatarID + ".gif");
                            File file = new File(UserAvatar.toString());
                            response.setContentType("image/gif");
                            InputStream in = new FileInputStream(file);
                            byte[] bytes = in.readAllBytes();
                            in.close();
                            return bytes;
                        } else {
                            Path UserAvatar = Paths.get("Data/UserAvatar/UA-" + data + ".png");
                                File file = new File(UserAvatar.toString());
                                if (file.exists()) {
                                    response.setContentType("image/png");
                                    InputStream in = new FileInputStream(file);
                                    byte[] bytes = in.readAllBytes();
                                    in.close();
                                    return bytes;
                                } else {
                                    return ErrRes.NotFoundResourceException("Not Found User Avatar", response);
                                }
                        }
                    } else {
                        return ErrRes.IllegalRequestException("Not a valid universal identifier.", response);
                    }
                }

                case "textures":{
                    if (data.length() > 1) {
                        Path SKINTexture = Paths.get("Data/YggdrasilTexture/hash-" + data);
                            File file = new File(SKINTexture.toString());
                            if (file.exists()) {
                                response.setContentType("image/png");
                                InputStream in = new FileInputStream(file);
                                byte[] bytes = in.readAllBytes();
                                in.close();
                                return bytes;
                            } else {
                                return ErrRes.NotFoundResourceException("Not Found SKINTexture", response);
                            }
                    }else {
                        return ErrRes.IllegalRequestException("Not a valid universal identifier.", response);
                    }
                }



                default:
                    return ErrRes.NotFoundResourceException("Not Found Resource", response);




            }
        } else {
            response.setContentType("application/json");
            return ErrRes.NotFoundResourceException("Not Found User Avatar", response);
        }
    }
}
