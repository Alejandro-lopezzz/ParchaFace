package com.alejo.parchaface.controller;

import com.alejo.parchaface.dto.*;
import com.alejo.parchaface.model.CommunityComment;
import com.alejo.parchaface.model.CommunityPost;
import com.alejo.parchaface.model.PostRating;
import com.alejo.parchaface.service.CommunityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/community")
public class CommunityController {

  private final CommunityService service;

  public CommunityController(CommunityService service) {
    this.service = service;
  }

  // ✅ PUBLIC: listar posts
  @GetMapping("/posts")
  public List<CommunityPost> listPosts(
    @RequestParam(required = false) String q,
    @RequestParam(required = false) String city,
    @RequestParam(required = false) String category,
    @RequestParam(required = false, defaultValue = "recent") String sort
  ) {
    return service.listPosts(q, city, category, sort);
  }

  // ✅ PUBLIC: get post
  @GetMapping("/posts/{id}")
  public ResponseEntity<CommunityPost> getPost(@PathVariable Integer id) {
    try {
      return ResponseEntity.ok(service.getPost(id));
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  // ✅ PUBLIC: comments por post
  @GetMapping("/posts/{id}/comments")
  public List<CommunityComment> getComments(@PathVariable Integer id) {
    return service.getComments(id);
  }

  // 🔒 PRIVATE: crear post
  @PostMapping("/posts")
  public CommunityPost createPost(@RequestBody CreateCommunityPostRequest req, Authentication auth) {
    return service.createPost(req, auth.getName());
  }

  // 🔒 PRIVATE: comentar
  @PostMapping("/posts/{id}/comments")
  public CommunityComment addComment(@PathVariable Integer id, @RequestBody CreateCommunityCommentRequest req, Authentication auth) {
    return service.addComment(id, req, auth.getName());
  }

  // ✅ PUBLIC: rating summary (si está logueado, devuelve myRating)
  @GetMapping("/posts/{id}/rating")
  public RatingSummaryResponse ratingSummary(@PathVariable Integer id, Authentication auth) {
    String correo = (auth != null ? auth.getName() : null);
    return service.getPostRatingSummary(id, correo);
  }

  // 🔒 PRIVATE: rate post (solo cambia el del usuario logueado)
  @PostMapping("/posts/{id}/rating")
  public PostRating ratePost(@PathVariable Integer id, @RequestBody RatePostRequest req, Authentication auth) {
    return service.ratePost(id, req.rating, auth.getName());
  }

  // ✅ PUBLIC: likes count (si está logueado devuelve likedByMe)
  @GetMapping("/comments/{commentId}/likes")
  public LikeSummaryResponse commentLikes(@PathVariable Integer commentId, Authentication auth) {
    String correo = (auth != null ? auth.getName() : null);
    return service.getCommentLikes(commentId, correo);
  }

  // 🔒 PRIVATE: toggle like
  @PostMapping("/comments/{commentId}/like")
  public LikeSummaryResponse toggleLike(@PathVariable Integer commentId, Authentication auth) {
    return service.toggleCommentLike(commentId, auth.getName());
  }
}
