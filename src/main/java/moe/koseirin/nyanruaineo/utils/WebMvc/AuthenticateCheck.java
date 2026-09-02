package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.utils.ErrorUtils.Error;
import moe.koseirin.nyanruaineo.utils.ErrorUtils.ErrorCode;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@Component
public class AuthenticateCheck implements HandlerInterceptor {
    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    private final BanUserRepository banUserRepository;
    private final utilset utilset;



    private final UserDevicesRepository userDevicesRepository;

    public AuthenticateCheck(BanUserRepository banUserRepository, utilset utilset, UserDevicesRepository userDevicesRepository) {
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
        this.userDevicesRepository = userDevicesRepository;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String authorization = request.getHeader("Authorization");
        String event = request.getHeader("Event");
        String requestMethod = request.getMethod();
        if (authorization == null || event == null) {
            PrintWriter(response,Err(ErrorCode.IllegalRequest.getMessage(),"Zako~Unknown parameters MiaoWu~ "),ErrorCode.IllegalRequest.getCode());
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
            PrintWriter(response,Err(ErrorCode.Unauthorized.getMessage(),"Zako~Authentication failed, invalid token MiaoWu~ "),ErrorCode.Unauthorized.getCode());
            return false;
        }
        if (banUserRepository.LEVE450TRUE(userId) != null) {
            PrintWriter(response,Err(ErrorCode.Unauthorized.getMessage(),"Zako~account is banned for admin MiaoWu~ "),ErrorCode.Unauthorized.getCode());
            return false;
        }
        return validateEventAndMethod(event, requestMethod, response);
    }

    private boolean validateEventAndMethod(String event, String requestMethod, HttpServletResponse response) throws IOException {
        return switch (event) {
            case "0" -> // Get Information - GET
                    validateMethod(requestMethod, "GET", response);
            case "1" -> // Upload Data - POST
                    validateMethod(requestMethod, "POST", response);
            case "2" -> // Upload Asset - PUT
                    validateMethod(requestMethod, "PUT", response);
            case "3" -> // Delete - DELETE
                    validateMethod(requestMethod, "DELETE", response);
            default -> {
                PrintWriter(response, Err(ErrorCode.IllegalRequest.getMessage(), "Zako~Unknown action parameters MiaoWu~"), ErrorCode.IllegalRequest.getCode());
                yield false;
            }
        };
    }

    private boolean validateMethod(String actualMethod, String expectedMethod, HttpServletResponse response) throws IOException {
        if (!Objects.equals(actualMethod, expectedMethod)) {
            PrintWriter(response, Err(ErrorCode.IllegalRequest.getMessage(), "Zako~Unknown action parameters MiaoWu~"), ErrorCode.IllegalRequest.getCode());
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
    public Error Err(String MESSAGE,String M) {
        Error error = new Error();
        error.setError(MESSAGE);
        error.setMessage(M);
        error.setTimestamp(LocalDateTime.now());
        return error;

    }
}
