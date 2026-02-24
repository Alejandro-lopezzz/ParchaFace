package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "comment_like",
  uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_correo"})
)
public class CommentLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_like")
  private Integer idLike;

  @Column(name = "comment_id", nullable = false)
  private Integer commentId;

  @Column(name = "user_correo", nullable = false, length = 150)
  private String userCorreo;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Integer getIdLike() { return idLike; }
  public void setIdLike(Integer idLike) { this.idLike = idLike; }

  public Integer getCommentId() { return commentId; }
  public void setCommentId(Integer commentId) { this.commentId = commentId; }

  public String getUserCorreo() { return userCorreo; }
  public void setUserCorreo(String userCorreo) { this.userCorreo = userCorreo; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
