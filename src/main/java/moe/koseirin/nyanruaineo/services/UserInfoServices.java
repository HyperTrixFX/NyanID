package moe.koseirin.nyanruaineo.services;

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.dto.UserResponse;
import moe.koseirin.nyanruaineo.dto.UserResponseDTO;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.UserServiceImpl;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final UserServiceImpl userService;
    private final Respond respond;

    public UserInfoServices(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository, utilset utilset, UserServiceImpl userService1, Respond respond) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
        this.userService = userService1;
        this.respond = respond;
    }

    @Transactional
    public ResponseEntity<?> validateMethod(HttpServletRequest request){

            return null;
    }

    @Transactional
    public ResponseEntity<?> getUserInfo(String uuid, HttpServletRequest request){

        return null;
    }


    @Transactional
    public ResponseEntity<?> SearchUserApi(UserResponseDTO userResponseDTO){
        if (userResponseDTO == null){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        String value = userResponseDTO.getValue();
        if (value == null) {
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        if (value.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        List<UserResponse> result = userService.searchUsers(value);
        if (result.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        return ResponseEntity.ok().body(result);

    }
}
