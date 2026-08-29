package moe.koseirin.nyanruaineo.utils.AI;

import lombok.Data;

@Data
public class FunctionCall {
    private String name;
    private Object arguments;   // 改为 Object，兼容字符串和 Map
}