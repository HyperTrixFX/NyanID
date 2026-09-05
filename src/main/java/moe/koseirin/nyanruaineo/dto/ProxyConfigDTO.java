package moe.koseirin.nyanruaineo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * @author KoseiRin_
 * awa
 */

/** 一条代理配置项：键、原始值（字符串）、以及用于前端渲染的类型提示。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyConfigDTO {
    private String key;
    private String value;
    /** 取值：int / boolean / string / json。 */
    private String type;
}
