package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.impl;


import moe.takanashihoshino.nyaniduserserver.repository.UserPermissionsRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.UserPermissionsService;
import moe.takanashihoshino.nyaniduserserver.entity.UserPermissions;
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
