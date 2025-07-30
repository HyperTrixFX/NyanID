package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.impl;


import moe.takanashihoshino.nyaniduserserver.server.web.User.UserJson.BanUserListJson;
import moe.takanashihoshino.nyaniduserserver.entity.BanUserList;
import moe.takanashihoshino.nyaniduserserver.repository.BanUserRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.BanUserService;
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

    Pageable pageable = PageRequest.of(0, 50);
    public List<BanUserListJson> GetBanList(String keyword) {

        Page<BanUserList> userPage = banUserRepository.searchByUid(keyword,pageable);

        return userPage.getContent().stream()
                .map(list -> new BanUserListJson(list.getBanID(),list.getBanTime(),list.getBannedBy(),list.getReason(),list.isActive(),list.getUid(),list.getType()))
                .collect(Collectors.toList());
    }
}
