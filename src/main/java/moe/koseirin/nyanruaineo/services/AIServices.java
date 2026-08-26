package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.utils.AI.ChatMessage;
import moe.koseirin.nyanruaineo.utils.AI.FunctionCall;
import moe.koseirin.nyanruaineo.utils.AI.SYSTEM_PROMPT;
import moe.koseirin.nyanruaineo.utils.AI.ToolCall;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.NyanidUserServiceImpl;
import okhttp3.*;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AIServices {

    private final NyanidUserServiceImpl nyanidUserService;
    private final OkHttpClient okHttpClient;
    private final StringRedisTemplate redisTemplate;

    @Value("${ai.provider:ollama}")
    private String provider;

    @Value("${ai.enable}")
    private boolean enable;

    @Value("${ollama.url:http://localhost:11434/api/chat}")
    private String ollamaUrl;
    @Value("${ollama.model:qwen3.5:9b}")
    private String ollamaModel;

    @Value("${deepseek.url:https://api.deepseek.com/chat/completions}")
    private String deepseekUrl;
    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;
    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    private static final String KEY_PREFIX = "romyu:chat:history:";
    private static final Duration EXPIRE_DURATION = Duration.ofHours(5);

    private static final String TOOLS_JSON = """
            [
              {
                "type": "function",
                "function": {
                  "name": "get_user_by_uid",
                  "description": "根据用户 UID 查询用户的公开信息（昵称、简介、经验值）",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "uid": {
                        "type": "string",
                        "description": "用户的唯一标识 UID"
                      }
                    },
                    "required": ["uid"]
                  }
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "find_users_by_nickname",
                  "description": "根据昵称模糊查询用户列表，返回最多 10 条公开信息",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "nickname": {
                        "type": "string",
                        "description": "用户昵称的部分或完整内容"
                      }
                    },
                    "required": ["nickname"]
                  }
                }
              }
            ]
            """;

    private static final List<Object> TOOLS = JSON.parseArray(TOOLS_JSON);

    public AIServices(NyanidUserServiceImpl nyanidUserService, OkHttpClient okHttpClient, StringRedisTemplate redisTemplate) {
        this.nyanidUserService = nyanidUserService;
        this.okHttpClient = okHttpClient;
        this.redisTemplate = redisTemplate;
    }

    public String chat(String sessionId, String userMessage,String username) {
        if (enable) {
            List<ChatMessage> history = getHistory(sessionId);

            history.addFirst(new ChatMessage("system", SYSTEM_PROMPT.romyu.getPROMPT()));

            String userInfo = String.format(
                    "当前用户信息：用户名=%s, 昵称=%s, 是否管理员=%s。当前时间：%s。",
                    username,
                    username,
                    isAdmin(),
                    LocalDateTime.now()
            );
            String combinedUserMessage = userInfo + "\n" + userMessage;

            ChatMessage currentUserMsg = new ChatMessage("user", combinedUserMessage);
            history.add(currentUserMsg);

            try {
                JSONObject firstResponse = callAI(history, true);
                ChatMessage assistantMsg = parseAssistantMessage(firstResponse);

                if (assistantMsg.getToolCalls() == null || assistantMsg.getToolCalls().isEmpty()) {
                    saveHistory(sessionId, currentUserMsg, assistantMsg);
                    return assistantMsg.getContent();
                }

                history.add(assistantMsg);

                for (ToolCall toolCall : assistantMsg.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    Map<String, Object> args = convertArgsToMap(toolCall.getFunction().getArguments());

                    String toolResult = executeTool(toolName, args);
                    ChatMessage toolMsg = new ChatMessage("tool", toolResult);
                    toolMsg.setToolCallId(toolCall.getId());
                    if (!"deepseek".equalsIgnoreCase(provider)) {
                        toolMsg.setToolName(toolName);
                    }
                    history.add(toolMsg);
                }

                JSONObject secondResponse = callAI(history, false);
                ChatMessage finalAssistantMsg = parseAssistantMessage(secondResponse);

                saveHistory(sessionId, currentUserMsg, finalAssistantMsg);
                return finalAssistantMsg.getContent();

            } catch (Exception e) {
                ChatMessage errorAssistantMsg = new ChatMessage("assistant", "抱歉，系统出现错误。");
                saveHistory(sessionId, currentUserMsg, errorAssistantMsg);
                e.printStackTrace();
                return "抱歉，系统出现错误。";
            }
        }else return "未启用AIChat服务喵~";

    }

    //统一入口
    private JSONObject callAI(List<ChatMessage> messages, boolean includeTools) throws Exception {
        if ("deepseek".equalsIgnoreCase(provider)) {
            return callDeepSeek(messages, includeTools);
        } else {
            return callOllama(messages, includeTools);
        }
    }

    //Ollama调用
    private JSONObject callOllama(List<ChatMessage> messages, boolean includeTools) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("stream", false);
        requestBody.put("think", true);
        requestBody.put("messages", messages);
        if (includeTools) {
            requestBody.put("tools", TOOLS);
        }

        String json = JSON.toJSONString(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(ollamaUrl)
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Ollama API error: " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            return JSON.parseObject(respBody);
        }
    }

    //DeepSeek调用
    private JSONObject callDeepSeek(List<ChatMessage> messages, boolean includeTools) throws Exception {
        List<ChatMessage> cleanedMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            ChatMessage copy = new ChatMessage();
            copy.setRole(msg.getRole());
            copy.setContent(msg.getContent() == null ? "" : msg.getContent());
            if (msg.getToolCalls() != null) {
                List<ToolCall> copiedToolCalls = new ArrayList<>();
                for (ToolCall tc : msg.getToolCalls()) {
                    ToolCall tcCopy = new ToolCall();
                    tcCopy.setId(tc.getId());
                    tcCopy.setType(tc.getType());
                    FunctionCall fc = tc.getFunction();
                    if (fc != null) {
                        FunctionCall fcCopy = new FunctionCall();
                        fcCopy.setName(fc.getName());
                        if (fc.getArguments() != null) {
                            Object args = fc.getArguments();
                            if (args instanceof String) {
                                fcCopy.setArguments(args);
                            } else {
                                fcCopy.setArguments(JSON.toJSONString(args));
                            }
                        }
                        tcCopy.setFunction(fcCopy);
                    }
                    copiedToolCalls.add(tcCopy);
                }
                copy.setToolCalls(copiedToolCalls);
            }
            copy.setToolCallId(msg.getToolCallId());
            cleanedMessages.add(copy);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepseekModel);
        requestBody.put("stream", false);
        Map<String, String> thinkingConfig = new HashMap<>();
        thinkingConfig.put("type", "disabled");
        requestBody.put("thinking", thinkingConfig);
        requestBody.put("messages", cleanedMessages);
        if (includeTools) {
            requestBody.put("tools", TOOLS);
        }

        String json = JSON.toJSONString(requestBody);

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request.Builder builder = new Request.Builder()
                .url(deepseekUrl)
                .post(body)
                .addHeader("Content-Type", "application/json");
        if (deepseekApiKey != null && !deepseekApiKey.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + deepseekApiKey);
        }

        try (Response response = okHttpClient.newCall(builder.build()).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("DeepSeek API error: " + response.code() + ", body: " + respBody);
            }

            JSONObject respJson = JSON.parseObject(respBody);
            JSONArray choices = respJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                String content = message.getString("content");
                log.info("DeepSeek Response message: {}", message.toJSONString());
            }
            JSONObject usage = respJson.getJSONObject("usage");
            if (usage != null) {
                int promptTokens = usage.getIntValue("prompt_tokens", 0);
                int completionTokens = usage.getIntValue("completion_tokens", 0);
                int totalTokens = usage.getIntValue("total_tokens", 0);
                int cacheHit = usage.getIntValue("prompt_cache_hit_tokens", 0);
                int cacheMiss = usage.getIntValue("prompt_cache_miss_tokens", 0);

                log.info("DeepSeek Token Usage: total={}, prompt={}, completion={}, cacheHit={}, cacheMiss={}",
                        totalTokens, promptTokens, completionTokens, cacheHit, cacheMiss);
            }

            return respJson;
        }
    }

    //解析响应
    private ChatMessage parseAssistantMessage(JSONObject response) {
        JSONObject messageNode;
        if ("deepseek".equalsIgnoreCase(provider)) {
            messageNode = response.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");
        } else {
            messageNode = response.getJSONObject("message");
        }

        ChatMessage msg = new ChatMessage();
        msg.setRole("assistant");
        String content = messageNode.getString("content");
        msg.setContent(content == null ? "" : content);

        if (messageNode.containsKey("tool_calls")) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (Object obj : messageNode.getJSONArray("tool_calls")) {
                ToolCall tc = getToolCall((JSONObject) obj);
                toolCalls.add(tc);
            }
            msg.setToolCalls(toolCalls);
        }
        return msg;
    }

    private static @NonNull ToolCall getToolCall(JSONObject obj) {
        ToolCall tc = new ToolCall();
        tc.setId(obj.getString("id"));
        tc.setType(obj.getString("type"));

        JSONObject funcJson = obj.getJSONObject("function");
        FunctionCall fc = new FunctionCall();
        fc.setName(funcJson.getString("name"));

        Object argsObj = funcJson.get("arguments");
        fc.setArguments(argsObj);
        tc.setFunction(fc);
        return tc;
    }

    private Map<String, Object> convertArgsToMap(Object args) {
        return switch (args) {
            case null -> new HashMap<>();
            case Map ignored -> (Map<String, Object>) args;
            case String s -> JSON.parseObject(s);
            default -> JSON.parseObject(JSON.toJSONString(args));
        };
    }

    private String executeTool(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "get_user_by_uid" -> nyanidUserService.getUserByUid((String) args.get("uid"));
            case "find_users_by_nickname" -> nyanidUserService.findUsersByNickname((String) args.get("nickname"));
            default -> "未知工具";
        };
    }

    public List<ChatMessage> getHistory(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        List<ChatMessage> history = new ArrayList<>();
        if (jsonList != null) {
            for (String json : jsonList) {
                history.add(JSON.parseObject(json, ChatMessage.class));
            }
        }
        return history;
    }

    public void saveHistory(String sessionId, ChatMessage userMsg, ChatMessage assistantMsg) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.opsForList().rightPushAll(key,
                JSON.toJSONString(userMsg),
                JSON.toJSONString(assistantMsg));
        redisTemplate.expire(key, EXPIRE_DURATION);
    }

    public void clearHistory(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

    //用户信息
    private String getCurrentUsername() {
        return "koseirin";
    }

    private String getCurrentNickname() {
        return "koseirin";
    }

    private boolean isAdmin() {
        return true;
    }
}