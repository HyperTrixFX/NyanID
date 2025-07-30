package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.impl;

import moe.takanashihoshino.nyaniduserserver.repository.TexturesListRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.TexturesListService;
import moe.takanashihoshino.nyaniduserserver.entity.TexturesList;
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
