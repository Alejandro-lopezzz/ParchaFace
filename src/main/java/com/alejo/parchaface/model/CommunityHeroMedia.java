package com.alejo.parchaface.model;

import com.alejo.parchaface.model.enums.CommunityHeroSlot;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_hero_media")
public class CommunityHeroMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private CommunityHeroSlot slot;

    @Column(name = "image_url", nullable = false, length = 700)
    private String imageUrl;

    @Column(name = "image_public_id", nullable = false, length = 500)
    private String imagePublicId;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public CommunityHeroSlot getSlot() {
        return slot;
    }

    public void setSlot(CommunityHeroSlot slot) {
        this.slot = slot;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImagePublicId() {
        return imagePublicId;
    }

    public void setImagePublicId(String imagePublicId) {
        this.imagePublicId = imagePublicId;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}