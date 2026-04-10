package moe.koseirin.nyanruaineo.utils.SqlService.impl;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.server.web.User.UserJson.BanUserListJson;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.BanUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BanUserServiceImpl implements BanUserService {


    private final BanUserRepository banUserRepository;

    public BanUserServiceImpl(BanUserRepository banUserRepository) {
        this.banUserRepository = banUserRepository;
    }

    @Override
    public BanUserList save(BanUserList banUserList) {
        return banUserRepository.save(banUserList);
    }

    public List<BanUserListJson> GetBanList(String keyword) {
        Pageable pageable = PageRequest.of(0, 50);
        Page<BanUserList> userPage = banUserRepository.searchByUid(keyword,pageable);
        return userPage.getContent().stream()
                .map(list -> new BanUserListJson(list.getBanID(),list.getBanTime(),list.getBannedBy(),list.getReason(),list.isActive(),list.getUid(),list.getType()))
                .collect(Collectors.toList());
    }
}
