package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.CommunityHeroMediaResponse;
import com.alejo.parchaface.model.enums.CommunityHeroSlot;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommunityHeroMediaService {

    List<CommunityHeroMediaResponse> listar();

    CommunityHeroMediaResponse guardar(CommunityHeroSlot slot, MultipartFile image, String altText);

    void eliminar(CommunityHeroSlot slot);
}