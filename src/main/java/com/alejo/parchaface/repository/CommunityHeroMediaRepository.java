package com.alejo.parchaface.repository;

import com.alejo.parchaface.model.CommunityHeroMedia;
import com.alejo.parchaface.model.enums.CommunityHeroSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityHeroMediaRepository extends JpaRepository<CommunityHeroMedia, Integer> {
    Optional<CommunityHeroMedia> findBySlot(CommunityHeroSlot slot);
}