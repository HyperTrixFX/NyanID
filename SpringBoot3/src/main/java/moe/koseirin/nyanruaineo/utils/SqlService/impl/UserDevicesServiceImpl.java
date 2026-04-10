package moe.koseirin.nyanruaineo.utils.SqlService.impl;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.server.web.User.UserJson.UserDevicesJson;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.UserDevicesService;
import moe.koseirin.nyanruaineo.entity.UserDevices;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class UserDevicesServiceImpl implements UserDevicesService {


    private final UserDevicesRepository userDevicesRepository;

    public UserDevicesServiceImpl(UserDevicesRepository userDevicesRepository) {
        this.userDevicesRepository = userDevicesRepository;
    }

    @Override
    public UserDevices save(UserDevices userDevices) {
        return userDevicesRepository.save(userDevices);
    }

    Pageable pageable = PageRequest.of(0, 50);
    public List<UserDevicesJson> GetDevices(String keyword) {

        Page<UserDevices> userPage = userDevicesRepository.searchByUid(keyword,pageable);
        return userPage.getContent().stream()
                .map(user -> new UserDevicesJson(user.getDeviceName(), user.getDeviceID(),user.getIp(),user.getCreateTime(),user.isIsActive(),user.getSession()))
                .collect(Collectors.toList());
    }
}
