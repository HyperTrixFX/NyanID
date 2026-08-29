package moe.koseirin.nyanruaineo.utils.AI;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class ChatMessage {
    private String role;
    private String content;

    @JSONField(name = "tool_calls")
    private List<ToolCall> toolCalls;

    @JSONField(name = "tool_name")
    private String toolName;

    @JSONField(name = "tool_call_id")
    private String toolCallId;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}