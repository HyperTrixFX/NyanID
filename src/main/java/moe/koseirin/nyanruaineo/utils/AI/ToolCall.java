package moe.koseirin.nyanruaineo.utils.AI;

import lombok.Data;

@Data
public class ToolCall {
    private String id;
    private String type;
    private FunctionCall function;
}