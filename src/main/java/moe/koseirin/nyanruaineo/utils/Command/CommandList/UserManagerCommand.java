package moe.koseirin.nyanruaineo.utils.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.Command.Command;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.BanUserService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.logging.Logger;

@Component
public class UserManagerCommand implements Command {



    private final AccountsRepository accountsRepository;


    private final BanUserRepository banUserRepository;


    private final BanUserService banUserService;




    private static Logger logger = Logger.getLogger("NyanID");

    public UserManagerCommand(AccountsRepository accountsRepository, BanUserRepository banUserRepository, BanUserService banUserService) {
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.banUserService = banUserService;
    }

    @Override
    public String getName() {
        return "/ac";
    }

    @Override
    public String getDescription() {
        return "对账号进行管理,使用方法:[/ac unban/ban(arg: reason)/remove/create(args: email password username)/change(args: userdevices/username/more data) uid/email/username]";
    }

    @Override
    public void execute(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        if (args.length > 1){
            String t1 = args[0];
            if (!t1.equals("create") && !t1.equals("change")){
                switch (t1) {
                    case "ban":
                        if (!(accountsRepository.find(args[1]) == null)) {
                            String uid = accountsRepository.find(args[1]).getUid();
                            if (banUserRepository.findBanIDByUid(uid) == null ) {
                                String banid = String.valueOf(UUID.nameUUIDFromBytes((args[1]+LocalDateTime.now()).getBytes(StandardCharsets.UTF_8)));
                                BanUserList banUserList = new BanUserList();
                                banUserList.setBanID(banid);
                                banUserList.setUid(uid);
                                banUserList.setBannedBy("NAC");
                                banUserList.setBanTime(LocalDateTime.now());
                                if (args.length < 3){
                                    banUserList.setReason("This account has been banned for violating our User Agreement. 杂鱼喵~");
                                }else {
                                    banUserList.setReason(args[2]);
                                }
                                banUserList.setActive(true);
                                banUserList.setBannedBy("TakanashiNyaphthalene");
                                banUserService.save(banUserList);
                                logger.info("Banned User ["+uid+"] and BanID:"+banid);
                            }else {
//                                if (!(list == null)) {
//                                //logger.info("User ["+uid+"] is Banned . Reason: ["+ list.getReason()+"], BanID: ["+list.getBanID()+"], BanTime: ["+list.getBanTime()+"], BannedBy: ["+list.getBannedBy()+"].");
//                                }
                            }
                        }else {
                            logger.warning("用户不存在杂鱼喵~");
                        }
                        break;
                    case "remove":

                        break;




                    default:
                        logger.warning("参数错误,请输入ban/remove/create/change ");
                        break;
                }


            }else {




            }




            }else {
            logger.warning("参数错误,请输入ban/remove/create/change ");
            }























        }
    }

