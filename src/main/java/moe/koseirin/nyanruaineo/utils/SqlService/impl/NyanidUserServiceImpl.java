package moe.koseirin.nyanruaineo.utils.SqlService.impl;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.NyanidUserService;
import org.springframework.stereotype.Service;

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

}
