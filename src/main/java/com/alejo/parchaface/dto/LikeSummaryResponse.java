package com.alejo.parchaface.dto;

public class LikeSummaryResponse {
  public Long likes;
  public Boolean likedByMe; // opcional (si está logueado)

  public LikeSummaryResponse(Long likes, Boolean likedByMe) {
    this.likes = likes;
    this.likedByMe = likedByMe;
  }
}
