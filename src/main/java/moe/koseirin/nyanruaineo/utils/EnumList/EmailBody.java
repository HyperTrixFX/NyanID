package moe.koseirin.nyanruaineo.utils.EnumList;

import lombok.Getter;
import lombok.experimental.Accessors;

/*
 * @author KoseiRin_
 * awa
 */

@Getter
@Accessors(chain=true)
public enum EmailBody {
    VerificationCodeBody("<p>您好喵,非常感谢您使用NyanID,我们注意到你的账户正在进行一些敏感的操作喵~所以我们需要验证你的邮箱以确保是您喵本人在操作,关注小鳥遊ホシノ喵!!!谢谢喵~\n" +
            "verification code is:<font color=\"#ff8c00\">${code}</font>, 杂鱼喵,这个验证码有效期为5分钟哦,请尽快使用喵!~</p>"),
    RegisterBody("<p>您好喵,非常感谢您注册使用NyanID,点击链接确认注册:)<a href=\"${link}\">确认注册</a>\n");

    public final String body;

    EmailBody(String body) {
        this.body = body;
    }
}
