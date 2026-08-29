package moe.koseirin.nyanruaineo.utils.SqlService.impl;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.NyanidUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NyanidUserServiceImpl implements NyanidUserService {


    private final NyanIDuserRepository nyanIDuserRepository;

    public NyanidUserServiceImpl(NyanIDuserRepository nyanIDuserRepository) {
        this.nyanIDuserRepository = nyanIDuserRepository;
    }

    @Override
    public NyanIDuser save(NyanIDuser nyanIDuser) {
        return nyanIDuserRepository.save(nyanIDuser);
    }

    /**
     * 工具方法：按 UID 查询用户信息
     * 只返回公开信息，不包含开发者标志等敏感字段
     */
    @Transactional(readOnly = true)
    @Override
    public String getUserByUid(String uid) {
        Optional<NyanIDuser> userOpt = nyanIDuserRepository.findById(uid);
        if (userOpt.isEmpty()) {
            return "未找到 UID 为 " + uid + " 的用户";
        }
        NyanIDuser user = userOpt.get();
        return formatUser(user);
    }

    /**
     * 工具方法：按昵称模糊查询用户
     * 返回最多 10 条结果，避免数据过多
     */
    @Transactional(readOnly = true)
    @Override
    public String findUsersByNickname(String nickname) {
        List<NyanIDuser> users = nyanIDuserRepository.findByNicknameContaining(nickname);
        if (users.isEmpty()) {
            return "未找到昵称包含 '" + nickname + "' 的用户";
        }
        return users.stream()
                .limit(10)
                .map(this::formatUser)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * 格式化用户信息（只输出公开字段）
     */
    private String formatUser(NyanIDuser user) {
        return String.format(
                "UID: %s\n昵称: %s\n简介: %s\n经验值: %d",
                user.getUid(),
                user.getNickname() != null ? user.getNickname() : "无",
                user.getDescription() != null ? user.getDescription() : "无",
                user.getExp()
        );
    }

}
