package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_comment")
public class CommunityComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_comment")
  private Integer idComment;

  @Column(name = "post_id", nullable = false)
  private Integer postId;

  @Column(name = "content", nullable = false, length = 2000)
  private String content;

  @Column(name = "author_correo", nullable = false, length = 150)
  private String authorCorreo;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  public Integer getIdComment() { return idComment; }
  public void setIdComment(Integer idComment) { this.idComment = idComment; }

  public Integer getPostId() { return postId; }
  public void setPostId(Integer postId) { this.postId = postId; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getAuthorCorreo() { return authorCorreo; }
  public void setAuthorCorreo(String authorCorreo) { this.authorCorreo = authorCorreo; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
