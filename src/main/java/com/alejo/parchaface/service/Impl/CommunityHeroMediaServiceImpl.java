package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.CommunityHeroMediaResponse;
import com.alejo.parchaface.model.CommunityHeroMedia;
import com.alejo.parchaface.model.enums.CommunityHeroSlot;
import com.alejo.parchaface.repository.CommunityHeroMediaRepository;
import com.alejo.parchaface.service.CommunityHeroMediaService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class CommunityHeroMediaServiceImpl implements CommunityHeroMediaService {

    private static final String CLOUDINARY_FOLDER = "parchaface/community/hero";

    private final CommunityHeroMediaRepository repository;
    private final Cloudinary cloudinary;

    public CommunityHeroMediaServiceImpl(
            CommunityHeroMediaRepository repository,
            Cloudinary cloudinary
    ) {
        this.repository = repository;
        this.cloudinary = cloudinary;
    }

    @Override
    public List<CommunityHeroMediaResponse> listar() {
        return Arrays.stream(CommunityHeroSlot.values())
                .map(slot -> repository.findBySlot(slot).orElse(null))
                .filter(item -> item != null)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommunityHeroMediaResponse guardar(CommunityHeroSlot slot, MultipartFile image, String altText) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar una imagen");
        }

        validarTipoImagen(image);

        CommunityHeroMedia media = repository.findBySlot(slot).orElse(null);
        String oldPublicId = media != null ? media.getImagePublicId() : null;
        String newPublicId = null;

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap(
                            "folder", CLOUDINARY_FOLDER,
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            newPublicId = publicId;

            if (!StringUtils.hasText(secureUrl) || !StringUtils.hasText(publicId)) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cloudinary no devolvió una URL o public_id válidos"
                );
            }

            if (media == null) {
                media = new CommunityHeroMedia();
                media.setSlot(slot);
            }

            media.setImageUrl(secureUrl);
            media.setImagePublicId(publicId);
            media.setAltText(StringUtils.hasText(altText) ? altText.trim() : getDefaultAltText(slot));

            CommunityHeroMedia saved = repository.save(media);

            if (StringUtils.hasText(oldPublicId) && !oldPublicId.equals(publicId)) {
                eliminarDeCloudinary(oldPublicId);
            }

            return toResponse(saved);

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo subir la imagen del hero"
            );
        } catch (RuntimeException e) {
            if (StringUtils.hasText(newPublicId)) {
                eliminarDeCloudinary(newPublicId);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void eliminar(CommunityHeroSlot slot) {
        CommunityHeroMedia media = repository.findBySlot(slot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe imagen para ese slot"));

        String publicId = media.getImagePublicId();
        repository.delete(media);

        if (StringUtils.hasText(publicId)) {
            eliminarDeCloudinary(publicId);
        }
    }

    private void validarTipoImagen(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase().trim();

        boolean permitido =
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/webp");

        if (!permitido) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de imagen no permitido: " + contentType
            );
        }
    }

    private void eliminarDeCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception ignored) {
        }
    }

    private String getDefaultAltText(CommunityHeroSlot slot) {
        return switch (slot) {
            case PARTY -> "Personas disfrutando una fiesta y ambiente festivo";
            case ORGANIZE -> "Organización y promoción de eventos";
            case CONNECT -> "Personas conectando y compartiendo experiencias";
        };
    }

    private CommunityHeroMediaResponse toResponse(CommunityHeroMedia media) {
        return new CommunityHeroMediaResponse(
                media.getSlot().name(),
                media.getImageUrl(),
                media.getAltText()
        );
    }
}