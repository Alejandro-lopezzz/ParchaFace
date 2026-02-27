package com.alejo.parchaface.dto;

public class RatingSummaryResponse {
  public Double average;
  public Long count;
  public Integer myRating; // opcional: rating del usuario actual (si está logueado)

  public RatingSummaryResponse(Double average, Long count, Integer myRating) {
    this.average = average;
    this.count = count;
    this.myRating = myRating;
  }
}
