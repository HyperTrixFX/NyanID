package moe.koseirin.nyanruaineo.utils.SqlService.impl;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.YggdrasilPlayerRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.YggdrasilPlayerService;
import moe.koseirin.nyanruaineo.entity.YggdrasilPlayer;
import org.springframework.stereotype.Service;

@Service
public class YggdrasilPlayerImpl implements YggdrasilPlayerService {


    private final YggdrasilPlayerRepository yggdrasilPlayerRepository;

    public YggdrasilPlayerImpl(YggdrasilPlayerRepository yggdrasilPlayerRepository) {
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
    }

    @Override
    public YggdrasilPlayer save(YggdrasilPlayer yggdrasilPlayer) {
        return yggdrasilPlayerRepository.save(yggdrasilPlayer);
    }
}
