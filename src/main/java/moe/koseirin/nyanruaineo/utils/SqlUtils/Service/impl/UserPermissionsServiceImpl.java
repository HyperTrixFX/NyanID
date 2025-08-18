package moe.koseirin.nyanruaineo.utils.SqlUtils.Service.impl;


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.UserPermissionsService;
import moe.koseirin.nyanruaineo.entity.UserPermissions;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionsServiceImpl implements UserPermissionsService {


    private final UserPermissionsRepository userPermissionsRepository;

    public UserPermissionsServiceImpl(UserPermissionsRepository userPermissionsRepository) {
        this.userPermissionsRepository = userPermissionsRepository;
    }


    @Override
    public UserPermissions save(UserPermissions userPermissions) {
        return userPermissionsRepository.save(userPermissions);
    }



}
