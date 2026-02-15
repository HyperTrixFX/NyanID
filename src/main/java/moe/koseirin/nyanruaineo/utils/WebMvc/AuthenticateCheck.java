package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.utils.ErrUtils.Error;
import moe.koseirin.nyanruaineo.utils.ErrUtils.ErrorCode;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@Component
public class AuthenticateCheck implements HandlerInterceptor {
    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    private final BanUserRepository banUserRepository;
    private final utilset utilset;
    private final Respond respond;


    private final UserDevicesRepository userDevicesRepository;

    public AuthenticateCheck(BanUserRepository banUserRepository, utilset utilset, Respond respond, UserDevicesRepository userDevicesRepository) {
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
        this.respond = respond;
        this.userDevicesRepository = userDevicesRepository;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        String event = request.getHeader("Event");
        String requestMethod = request.getMethod();
        if (authorization == null || event == null) {
            respond.respond(MediaType.APPLICATION_JSON,401, "message","Zako~Authentication failed, invalid token MiaoWu~ ","timestamp", LocalDateTime.now());

            return false;
        }

        String rawToken = authorization.replace("Bearer ", "");
        if (Objects.equals(rawToken,"undefined")) {
            response.setStatus(401);
            return false;
        }
        String token = utilset.decrypt(rawToken, privateKey);
        String userId = userDevicesRepository.findUidByToken(token);
        if (userId == null) {
            PrintWriter(response,Err(ErrorCode.Unauthorized.getCode(),ErrorCode.Unauthorized.getMessage(),"Zako~Authentication failed, invalid token MiaoWu~ "),401);
            return false;
        }
        if (banUserRepository.LEVE450TRUE(userId) != null) {
            PrintWriter(response,Err(ErrorCode.Unauthorized.getCode(),ErrorCode.Unauthorized.getMessage(),"Zako~account is banned for admin MiaoWu~ "),401);
            return false;
        }
        return validateEventAndMethod(event, requestMethod, response);
    }

    private boolean validateEventAndMethod(String event, String requestMethod, HttpServletResponse response) throws IOException {
        switch (event) {
            case "UA": // Upload Asset - PUT
                return validateMethod(requestMethod, "PUT", response);
            case "UD": // Upload Data - POST
                return validateMethod(requestMethod, "POST", response);
            case "GI": // Get Information - GET
                return validateMethod(requestMethod, "GET", response);
            case "ADMIN": // Admin operation - POST
                return validateMethod(requestMethod, "POST", response);
            case "DE": // Delete - DELETE
                return validateMethod(requestMethod, "DELETE", response);
            default:
                PrintWriter(response, Err(ErrorCode.IllegalRequest.getCode(), ErrorCode.IllegalRequest.getMessage(), "Zako~Unknown action parameters MiaoWu~"), 403);
                return false;
        }
    }

    private boolean validateMethod(String actualMethod, String expectedMethod, HttpServletResponse response) throws IOException {
        if (!Objects.equals(actualMethod, expectedMethod)) {
            PrintWriter(response, Err(ErrorCode.IllegalRequest.getCode(), ErrorCode.IllegalRequest.getMessage(), "Zako~Unknown action parameters MiaoWu~"), 403);
            return false;
        }
        return true;
    }

    public void PrintWriter(HttpServletResponse response, Error error, int code) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setStatus(code);
        out.println(JSONObject.toJSONString(error));
    }
    public Error Err(int CODE ,String MESSAGE,String M) {
        Error error = new Error();
        error.setStatus(CODE);
        error.setError(MESSAGE);
        error.setMessage(M);
        error.setTimestamp(LocalDateTime.now());
        return error;

    }
}
