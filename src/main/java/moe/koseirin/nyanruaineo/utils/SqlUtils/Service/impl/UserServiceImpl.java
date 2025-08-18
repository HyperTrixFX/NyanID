package moe.koseirin.nyanruaineo.utils.SqlUtils.Service.impl;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.server.web.Public.UserResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl  implements UserService {


    private final AccountsRepository accountsRepository;

    public UserServiceImpl(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    @Override
    public Accounts save(Accounts accounts) {
        return accountsRepository.save(accounts);
    }

    public List<UserResponse> searchUsers(String keyword) {
        // 创建分页请求（第0页，7条结果，按username升序）
        Pageable pageable = PageRequest.of(0, 7, Sort.by("username").ascending());

        Page<Accounts> userPage = accountsRepository.searchByUsername(keyword, pageable);

        return userPage.getContent().stream()
                .map(user -> new UserResponse(user.getUsername(), user.getUid()))
                .collect(Collectors.toList());
    }
}
