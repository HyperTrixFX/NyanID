package moe.koseirin.nyanruaineo.utils.SqlService.impl;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.TexturesListRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.TexturesListService;
import moe.koseirin.nyanruaineo.entity.TexturesList;
import org.springframework.stereotype.Service;


@Service
public class TexturesListImpl implements TexturesListService {

    private final TexturesListRepository texturesListRepository;

    public TexturesListImpl(TexturesListRepository texturesListRepository) {
        this.texturesListRepository = texturesListRepository;
    }


    @Override
    public TexturesList save(TexturesList texturesList) {
        return texturesListRepository.save(texturesList);
    }
}
