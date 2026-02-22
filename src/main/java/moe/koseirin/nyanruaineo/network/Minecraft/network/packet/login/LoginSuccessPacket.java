package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class LoginSuccessPacket implements Packet {
    private String uuid;
    private String username;
    private List<Property> properties = new ArrayList<>();

    public LoginSuccessPacket(String uuid, String username) {
        this(uuid, username, new ArrayList<>());
    }

    public LoginSuccessPacket(String uuid, String username, List<Property> properties) {
        this.uuid = uuid;
        this.username = username;
        this.properties = properties;
    }

    @Override
    public int packetId() {
        return 0x02;
    }

    @Override
    public void encode(ByteBuf buf) {
        // UUID 转为无连字符字符串
        String uuidStr = uuid.toString().replace("-", "");
        VarIntCodec.writeString(buf, uuidStr);
        VarIntCodec.writeString(buf, username);

        // 写入属性数组
//        VarIntCodec.writeVarInt(buf, properties.size());
//        for (Property prop : properties) {
//            VarIntCodec.writeString(buf, prop.getName());
//            VarIntCodec.writeString(buf, prop.getValue());
//            buf.writeBoolean(prop.isSigned());
//            if (prop.isSigned()) {
//                VarIntCodec.writeString(buf, prop.getSignature());
//            }
//        }
    }

    @Override
    public void decode(ByteBuf buf) {
        String uuidStr = VarIntCodec.readString(buf);
        String withHyphens = uuidStr.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5");
//        this.uuid = UUID.fromString(withHyphens);
        this.uuid = withHyphens;

        this.username = VarIntCodec.readString(buf);

//        if (buf.isReadable()) {
//            int propCount = VarIntCodec.readVarInt(buf);
//            properties = new ArrayList<>(propCount);
//            for (int i = 0; i < propCount; i++) {
//                String name = VarIntCodec.readString(buf);
//                String value = VarIntCodec.readString(buf);
//                boolean signed = buf.readBoolean();
//                String signature = null;
//                if (signed) {
//                    signature = VarIntCodec.readString(buf);
//                }
//                properties.add(new Property(name, value, signed, signature));
//            }
//        } else {
//            properties = new ArrayList<>();
//        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        private String name;
        private String value;
        private boolean signed;
        private String signature; // 仅在 signed 为 true 时有效
    }
}
