#!/usr/bin/env python3
import asyncio
import base64
import os
import binascii
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
import websockets

# -------------------- config --------------------
WS_URL = "ws://localhost/api/zako/v3/websocket/bungee"
HEADERS = {
    "X-SID": "6621",      # 替换为实际的 sid
    "X-KEY": "qwq"           # 替换为实际的 key
}
# SM4 密钥（Base64 解码，需与配置文件中的一致）
SM4_KEY_BASE64 = "AQIDBAUGBwgJCgsMDQ4PEA=="  # 示例密钥
SM4_KEY = base64.b64decode(SM4_KEY_BASE64)

# -------------------- SM4 encrypt tools --------------------
def sm4_cbc_encrypt(data: bytes, key: bytes) -> bytes:
    """SM4-CBC 加密，PKCS7 填充，返回 IV(16) + 密文"""
    iv = os.urandom(16)
    cipher = Cipher(algorithms.SM4(key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()

    pad_len = 16 - (len(data) % 16)
    data += bytes([pad_len]) * pad_len

    encrypted = encryptor.update(data) + encryptor.finalize()
    return iv + encrypted

def sm4_cbc_decrypt(encrypted_data: bytes, key: bytes) -> bytes:
    """SM4-CBC 解密，自动移除填充"""
    if len(encrypted_data) < 16:
        raise ValueError("加密数据长度不足，缺少IV")
    iv = encrypted_data[:16]
    ciphertext = encrypted_data[16:]

    cipher = Cipher(algorithms.SM4(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    decrypted = decryptor.update(ciphertext) + decryptor.finalize()

    pad_len = decrypted[-1]
    if pad_len < 1 or pad_len > 16:
        raise ValueError("无效的填充长度")
    return decrypted[:-pad_len]

# -------------------- VarInt 编码/解码 --------------------
def write_varint(value: int) -> bytes:
    result = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            byte |= 0x80
        result.append(byte)
        if value == 0:
            break
    return bytes(result)

def read_varint(data: bytes, offset: int = 0):
    result = 0
    shift = 0
    pos = offset
    while True:
        if pos >= len(data):
            raise ValueError("数据不足")
        b = data[pos]
        result |= (b & 0x7F) << shift
        shift += 7
        pos += 1
        if not (b & 0x80):
            break
        if shift > 35:
            raise ValueError("VarInt 过大")
    return result, pos

def write_string(s: str) -> bytes:
    utf8_bytes = s.encode('utf-8')
    return write_varint(len(utf8_bytes)) + utf8_bytes

def read_string(data: bytes, offset: int):
    length, pos = read_varint(data, offset)
    end = pos + length
    if end > len(data):
        raise ValueError("字符串长度不足")
    return data[pos:end].decode('utf-8'), end

# -------------------- 数据包构造 --------------------
def build_heartbeat() -> bytes:
    """心跳包 (0x01)"""
    return write_varint(0x01)

def build_update_online(servername: str, online: int) -> bytes:
    """更新在线人数包 (0x02)"""
    packet_id = write_varint(0x02)
    return packet_id + write_string(servername) + write_varint(online)

def build_bind_account(code: str, uuid: str) -> bytes:
    """绑定账号包 (0x03)"""
    packet_id = write_varint(0x03)
    return packet_id + write_string(code) + write_string(uuid)

def build_check_bind(uuid: str) -> bytes:
    """检查绑定包 (0x04)"""
    packet_id = write_varint(0x04)
    return packet_id + write_string(uuid)

# -------------------- 响应解析 --------------------
def parse_response(data: bytes):
    """
    解析任意响应，返回 (packet_id, 解析结果字典)
    如果解析失败，结果字典包含 'error' 信息
    """
    try:
        packet_id, pos = read_varint(data, 0)
    except Exception as e:
        return None, {"error": f"读取包ID失败: {e}"}

    if packet_id == 0x81:
        # 心跳响应，无内容
        return packet_id, {}
    elif packet_id == 0x82:
        # 更新在线/绑定响应，包含一个布尔值
        try:
            if pos >= len(data):
                raise ValueError("数据不足")
            success = bool(data[pos])
            return packet_id, {"success": success}
        except Exception as e:
            return packet_id, {"error": f"解析0x82失败: {e}"}
    elif packet_id == 0x83:
        # 检查绑定响应
        try:
            if pos >= len(data):
                raise ValueError("数据不足")
            bind = bool(data[pos])
            pos += 1
            result = {"bind": bind}
            if bind:
                muid, pos = read_string(data, pos)
                uuid, pos = read_string(data, pos)
                username, pos = read_string(data, pos)
                result.update({"muid": muid, "uuid": uuid, "username": username})
            return packet_id, result
        except Exception as e:
            return packet_id, {"error": f"解析0x83失败: {e}"}
    else:
        return packet_id, {"warning": f"未知包ID: {packet_id}", "raw_data": data[pos:].hex()}

# -------------------- 交互式测试函数 --------------------
async def interactive_test():
    try:
        async with websockets.connect(WS_URL, extra_headers=HEADERS) as websocket:
            print("✅ 已连接到 WebSocket 服务器\n")

            while True:
                print("请选择要测试的包类型:")
                print("1. 心跳包 (0x01)")
                print("2. 更新在线人数包 (0x02)")
                print("3. 绑定账号包 (0x03)")
                print("4. 检查绑定包 (0x04)")
                print("0. 退出")
                choice = input("请输入数字选择: ").strip()

                if choice == "0":
                    break

                # 根据选择构造数据包
                try:
                    if choice == "1":
                        raw_data = build_heartbeat()
                        print("已构造心跳包")
                    elif choice == "2":
                        servername = input("请输入服务器名称 (如 lobby): ").strip()
                        online = int(input("请输入在线人数: ").strip())
                        raw_data = build_update_online(servername, online)
                        print(f"已构造更新在线包: servername={servername}, online={online}")
                    elif choice == "3":
                        code = input("请输入绑定码: ").strip()
                        uuid = input("请输入 UUID: ").strip()
                        raw_data = build_bind_account(code, uuid)
                        print(f"已构造绑定包: code={code}, uuid={uuid}")
                    elif choice == "4":
                        uuid = input("请输入 UUID: ").strip()
                        raw_data = build_check_bind(uuid)
                        print(f"已构造检查绑定包: uuid={uuid}")
                    else:
                        print("无效选择，请重新输入")
                        continue
                except ValueError as e:
                    print(f"输入错误: {e}")
                    continue

                # 加密并发送
                encrypted = sm4_cbc_encrypt(raw_data, SM4_KEY)
                print(f"发送加密数据 (长度 {len(encrypted)} 字节)")
                await websocket.send(encrypted)

                # 接收响应
                try:
                    response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                    print(f"收到响应 (长度 {len(response)} 字节)")
                except asyncio.TimeoutError:
                    print("❌ 等待响应超时")
                    continue

                # 解密响应
                try:
                    decrypted = sm4_cbc_decrypt(response, SM4_KEY)
                    print(f"解密后数据 (长度 {len(decrypted)} 字节): {decrypted.hex()}")
                except Exception as e:
                    print(f"❌ 解密失败: {e}")
                    # 仍然打印原始加密数据
                    print(f"原始加密数据 (hex): {response.hex()}")
                    continue

                # 解析并打印响应
                packet_id, parsed = parse_response(decrypted)
                if packet_id is not None:
                    print(f"数据包ID: {packet_id} (0x{packet_id:02x})")
                print("解析结果:")
                for key, value in parsed.items():
                    print(f"  {key}: {value}")
                print("-" * 50)

    except websockets.exceptions.InvalidStatusCode as e:
        print(f"❌ 连接失败，HTTP 状态码: {e.status_code}")
        if e.status_code == 401:
            print("   -> 认证失败，请检查 X-SID 和 X-KEY 请求头是否正确")
    except Exception as e:
        print(f"❌ 测试过程中出现异常: {e}")

if __name__ == "__main__":
    asyncio.run(interactive_test())