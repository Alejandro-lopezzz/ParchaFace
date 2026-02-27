package com.alejo.parchaface.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_post")
public class CommunityPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_post")
  private Integer idPost;

  @Column(name = "title", nullable = false, length = 120)
  private String title;

  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  @Column(name = "city", length = 120)
  private String city;

  @Column(name = "category", length = 80)
  private String category;

  @Column(name = "event_id")
  private Integer eventId;

  @Column(name = "author_correo", nullable = false, length = 150)
  private String authorCorreo;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "comments_count", nullable = false)
  private Integer commentsCount = 0;

  public Integer getIdPost() { return idPost; }
  public void setIdPost(Integer idPost) { this.idPost = idPost; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }

  public Integer getEventId() { return eventId; }
  public void setEventId(Integer eventId) { this.eventId = eventId; }

  public String getAuthorCorreo() { return authorCorreo; }
  public void setAuthorCorreo(String authorCorreo) { this.authorCorreo = authorCorreo; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public Integer getCommentsCount() { return commentsCount; }
  public void setCommentsCount(Integer commentsCount) { this.commentsCount = commentsCount; }
}
