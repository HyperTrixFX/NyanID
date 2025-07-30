package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.impl;

import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.TexturesListRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.TexturesListService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.TexturesList;
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
