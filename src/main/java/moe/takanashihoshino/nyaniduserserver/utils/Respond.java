package moe.takanashihoshino.nyaniduserserver.utils;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Respond {

    public static ResponseEntity<?> respond(MediaType mediaType, int code, Object... objects) {
        if (objects == null || objects.length == 0) {
            return ResponseEntity.status(code).build();
        }
        if (objects.length % 2 != 0) {
            return ResponseEntity.badRequest().build();
        }
        if (!mediaType.includes(MediaType.APPLICATION_JSON)) {
            return ResponseEntity.status(code)
                    .contentType(mediaType)
                    .body(objects);
        } else {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < objects.length; i += 2) {
                Object key = objects[i];
                Object value = objects[i + 1];
                if (!(key instanceof String)) {
                    Map<String, Object> error = Map.of(
                            "error", "Non-string key: " + key,
                            "timestamp", LocalDateTime.now()
                    );
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(JSONObject.toJSONString(error));
                }
                map.put((String) key, value);
            }
            return ResponseEntity.status(code)
                    .contentType(mediaType)
                    .body(JSONObject.toJSONString(map));
        }


    }

}
