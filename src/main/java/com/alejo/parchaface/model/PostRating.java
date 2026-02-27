package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "post_rating",
  uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_correo"})
)
public class PostRating {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_rating")
  private Integer idRating;

  @Column(name = "post_id", nullable = false)
  private Integer postId;

  @Column(name = "user_correo", nullable = false, length = 150)
  private String userCorreo;

  @Column(name = "rating", nullable = false)
  private Integer rating; // 1..5

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  public Integer getIdRating() { return idRating; }
  public void setIdRating(Integer idRating) { this.idRating = idRating; }

  public Integer getPostId() { return postId; }
  public void setPostId(Integer postId) { this.postId = postId; }

  public String getUserCorreo() { return userCorreo; }
  public void setUserCorreo(String userCorreo) { this.userCorreo = userCorreo; }

  public Integer getRating() { return rating; }
  public void setRating(Integer rating) { this.rating = rating; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
