package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.ProtocolConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Login success (0x02, login state, server to client). Wire format matches BungeeCord:
 * <ul>
 *   <li>&lt; 1.16 (735): UUID as a dashed string; 1.16+: raw 16-byte UUID;</li>
 *   <li>1.19+ (759): trailing properties list;</li>
 *   <li>1.20.5 &ndash; 1.21.2 (766 &ndash; 767): a trailing boolean;</li>
 *   <li>1.26.2+ (776): a trailing session id UUID.</li>
 * </ul>
 */
@Setter
@Getter
public class LoginSuccess extends DefinedPacket {

    private UUID uuid;
    private String username;
    private List<Property> properties = new ArrayList<>();
    private UUID sessionId;

    public LoginSuccess() {
    }

    public LoginSuccess(UUID uuid, String username, List<Property> properties) {
        this.uuid = uuid;
        this.username = username;
        this.properties = properties == null ? new ArrayList<>() : properties;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_16) {
            this.uuid = readUUID(buf);
        } else {
            this.uuid = UUID.fromString(readString(buf));
        }
        this.username = readString(buf);
        this.properties = new ArrayList<>();
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) {
            int count = readVarInt(buf);
            for (int i = 0; i < count; i++) {
                String name = readString(buf);
                String value = readString(buf);
                boolean signed = buf.readBoolean();
                String signature = signed ? readString(buf) : null;
                this.properties.add(new Property(name, value, signature));
            }
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_5 && protocolVersion < ProtocolConstants.MINECRAFT_1_21_2) {
            buf.readBoolean();
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_26_2) {
            this.sessionId = readUUID(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_16) {
            writeUUID(uuid, buf);
        } else {
            writeString(uuid.toString(), buf);
        }
        writeString(username, buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19) {
            writeVarInt(properties.size(), buf);
            for (Property property : properties) {
                writeString(property.getName(), buf);
                writeString(property.getValue(), buf);
                boolean signed = property.getSignature() != null;
                buf.writeBoolean(signed);
                if (signed) {
                    writeString(property.getSignature(), buf);
                }
            }
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_5 && protocolVersion < ProtocolConstants.MINECRAFT_1_21_2) {
            buf.writeBoolean(true);
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_26_2) {
            writeUUID(sessionId == null ? UUID.randomUUID() : sessionId, buf);
        }
    }

    @Getter
    public static final class Property {
        private final String name;
        private final String value;
        private final String signature;

        public Property(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

    }
}
