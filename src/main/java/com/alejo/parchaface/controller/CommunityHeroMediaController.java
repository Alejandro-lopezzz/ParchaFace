package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.CommunityHeroMediaResponse;
import com.alejo.parchaface.model.enums.CommunityHeroSlot;
import com.alejo.parchaface.service.CommunityHeroMediaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/community/hero-media")
public class CommunityHeroMediaController {

    private final CommunityHeroMediaService service;

    public CommunityHeroMediaController(CommunityHeroMediaService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommunityHeroMediaResponse> listar() {
        return service.listar();
    }

    @PostMapping(value = "/{slot}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommunityHeroMediaResponse> guardar(
            @PathVariable CommunityHeroSlot slot,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "altText", required = false) String altText
    ) {
        return ResponseEntity.ok(service.guardar(slot, image, altText));
    }

    @DeleteMapping("/{slot}")
    public ResponseEntity<Void> eliminar(@PathVariable CommunityHeroSlot slot) {
        service.eliminar(slot);
        return ResponseEntity.noContent().build();
    }
}