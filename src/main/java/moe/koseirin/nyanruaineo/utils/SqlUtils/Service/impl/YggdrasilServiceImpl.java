package moe.koseirin.nyanruaineo.utils.SqlUtils.Service.impl;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.YggdrasilRepository;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.YggdrasilService;
import moe.koseirin.nyanruaineo.entity.Yggdrasil;
import org.springframework.stereotype.Service;

@Service
public class YggdrasilServiceImpl implements YggdrasilService {


    private final YggdrasilRepository yggdrasilRepository;

    public YggdrasilServiceImpl(YggdrasilRepository yggdrasilRepository) {
        this.yggdrasilRepository = yggdrasilRepository;
    }

    @Override
    public Yggdrasil save(Yggdrasil yggdrasil) {
        return yggdrasilRepository.save(yggdrasil);
    }
}
