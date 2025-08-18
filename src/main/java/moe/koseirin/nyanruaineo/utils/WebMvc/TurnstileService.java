package moe.koseirin.nyanruaineo.utils.WebMvc;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/*
 * @author KoseiRin_
 * awa
 */
@Slf4j
@Service
public class TurnstileService {
    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final RestTemplate restTemplate = new RestTemplate();


    public static TurnstileResponse validateToken(String token,String TurnstileSecretKey, String remoteip) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", TurnstileSecretKey);
        params.add("response", token);
        if (remoteip != null) {
            params.add("remoteip", remoteip);
        }
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<TurnstileResponse> response = restTemplate.postForEntity(
                    SITEVERIFY_URL, request, TurnstileResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
        }
        TurnstileResponse errorResponse = new TurnstileResponse();
        errorResponse.setSuccess(false);
        return errorResponse;
    }

}