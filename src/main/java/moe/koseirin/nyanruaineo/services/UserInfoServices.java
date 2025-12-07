package moe.koseirin.nyanruaineo.services;

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author KoseiRin_
 * awa
 */
@Service
public class UserInfoServices {

    private final NyanIDuserRepository nyanIDuserRepository;
    private final AccountsRepository accountsRepository;
    private final BanUserRepository banUserRepository;
    private final utilset utilset;

    public UserInfoServices(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository, utilset utilset) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
    }

    @Transactional
    public ResponseEntity<?> validateMethod(HttpServletRequest request){


            return null;
    }

    @Transactional
    public ResponseEntity<?> getUserInfo(String uuid, HttpServletRequest request){









        return null;
    }
}
