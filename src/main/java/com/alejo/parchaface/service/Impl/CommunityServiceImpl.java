package com.alejo.parchaface.service.Impl;

import com.alejo.parchaface.dto.*;
import com.alejo.parchaface.model.*;
import com.alejo.parchaface.repository.*;
import com.alejo.parchaface.service.CommunityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class CommunityServiceImpl implements CommunityService {

  private final CommunityPostRepository postRepo;
  private final CommunityCommentRepository commentRepo;
  private final PostRatingRepository postRatingRepo;
  private final CommentLikeRepository commentLikeRepo;

  public CommunityServiceImpl(
    CommunityPostRepository postRepo,
    CommunityCommentRepository commentRepo,
    PostRatingRepository postRatingRepo,
    CommentLikeRepository commentLikeRepo
  ) {
    this.postRepo = postRepo;
    this.commentRepo = commentRepo;
    this.postRatingRepo = postRatingRepo;
    this.commentLikeRepo = commentLikeRepo;
  }

  @Override
  public List<CommunityPost> listPosts(String q, String city, String category, String sort) {
    List<CommunityPost> list = postRepo.findAll();

    if (q != null && !q.isBlank()) {
      String qq = q.trim().toLowerCase();
      list = list.stream().filter(p ->
        (p.getTitle() != null && p.getTitle().toLowerCase().contains(qq)) ||
          (p.getContent() != null && p.getContent().toLowerCase().contains(qq)) ||
          (p.getAuthorCorreo() != null && p.getAuthorCorreo().toLowerCase().contains(qq))
      ).toList();
    }

    if (city != null && !city.isBlank() && !"Todas".equalsIgnoreCase(city)) {
      list = list.stream().filter(p -> city.equals(p.getCity())).toList();
    }

    if (category != null && !category.isBlank() && !"Todas".equalsIgnoreCase(category)) {
      list = list.stream().filter(p -> category.equals(p.getCategory())).toList();
    }

    String s = (sort == null ? "recent" : sort);

    if ("trending".equalsIgnoreCase(s)) {
      list = list.stream()
        .sorted((a, b) -> Integer.compare(
          b.getCommentsCount() == null ? 0 : b.getCommentsCount(),
          a.getCommentsCount() == null ? 0 : a.getCommentsCount()
        ))
        .toList();
    } else if ("unanswered".equalsIgnoreCase(s)) {
      list = list.stream()
        .sorted(Comparator.comparingInt(p -> p.getCommentsCount() == null ? 0 : p.getCommentsCount()))
        .toList();
    } else {
      list = list.stream()
        .sorted((a, b) -> (b.getCreatedAt() == null ? LocalDateTime.MIN : b.getCreatedAt())
          .compareTo(a.getCreatedAt() == null ? LocalDateTime.MIN : a.getCreatedAt()))
        .toList();
    }

    return list;
  }

  @Override
  public CommunityPost getPost(Integer id) {
    return postRepo.findById(id).orElseThrow(() -> new RuntimeException("Post no encontrado"));
  }

  @Override
  public List<CommunityComment> getComments(Integer postId) {
    return commentRepo.findByPostIdOrderByCreatedAtAsc(postId);
  }

  @Override
  @Transactional
  public CommunityPost createPost(CreateCommunityPostRequest req, String correo) {
    if (req == null) throw new RuntimeException("Body requerido");
    if (req.title == null || req.title.trim().length() < 5) throw new RuntimeException("Título muy corto");
    if (req.content == null || req.content.trim().length() < 10) throw new RuntimeException("Contenido muy corto");

    CommunityPost p = new CommunityPost();
    p.setTitle(req.title.trim());
    p.setContent(req.content.trim());
    p.setCity(req.city);
    p.setCategory(req.category);
    p.setEventId(req.eventId);
    p.setAuthorCorreo(correo);
    p.setCreatedAt(LocalDateTime.now());
    p.setCommentsCount(0);

    return postRepo.save(p);
  }

  @Override
  @Transactional
  public CommunityComment addComment(Integer postId, CreateCommunityCommentRequest req, String correo) {
    if (req == null) throw new RuntimeException("Body requerido");
    if (req.content == null || req.content.trim().length() < 2) throw new RuntimeException("Comentario muy corto");

    CommunityPost post = getPost(postId);

    CommunityComment c = new CommunityComment();
    c.setPostId(postId);
    c.setContent(req.content.trim());
    c.setAuthorCorreo(correo);
    c.setCreatedAt(LocalDateTime.now());

    CommunityComment saved = commentRepo.save(c);

    long count = commentRepo.countByPostId(postId);
    post.setCommentsCount((int) count);
    postRepo.save(post);

    return saved;
  }

  // ======================
  // ⭐ Rating del POST
  // ======================
  @Override
  @Transactional
  public PostRating ratePost(Integer postId, Integer rating, String correo) {
    if (rating == null) throw new RuntimeException("rating requerido");

    int safe = Math.max(1, Math.min(5, rating));

    // valida post existe
    getPost(postId);

    return postRatingRepo.findByPostIdAndUserCorreo(postId, correo).map(existing -> {
      existing.setRating(safe);
      existing.setUpdatedAt(LocalDateTime.now());
      return postRatingRepo.save(existing);
    }).orElseGet(() -> {
      PostRating r = new PostRating();
      r.setPostId(postId);
      r.setUserCorreo(correo);
      r.setRating(safe);
      r.setCreatedAt(LocalDateTime.now());
      r.setUpdatedAt(LocalDateTime.now());
      return postRatingRepo.save(r);
    });
  }

  @Override
  public RatingSummaryResponse getPostRatingSummary(Integer postId, String correoOrNull) {
    List<PostRating> list = postRatingRepo.findByPostId(postId);
    if (list.isEmpty()) {
      return new RatingSummaryResponse(null, 0L, null);
    }

    double avg = list.stream().mapToInt(PostRating::getRating).average().orElse(0);
    Double rounded = Math.round(avg * 10.0) / 10.0;
    Long count = (long) list.size();

    Integer my = null;
    if (correoOrNull != null) {
      my = postRatingRepo.findByPostIdAndUserCorreo(postId, correoOrNull)
        .map(PostRating::getRating)
        .orElse(null);
    }

    return new RatingSummaryResponse(rounded, count, my);
  }

  // ======================
  // 👍 Like de COMENTARIO
  // ======================
  @Override
  @Transactional
  public LikeSummaryResponse toggleCommentLike(Integer commentId, String correo) {
    CommunityComment comment = commentRepo.findById(commentId)
      .orElseThrow(() -> new RuntimeException("Comentario no encontrado"));

    var existing = commentLikeRepo.findByCommentIdAndUserCorreo(comment.getIdComment(), correo);

    boolean likedByMe;
    if (existing.isPresent()) {
      commentLikeRepo.delete(existing.get());
      likedByMe = false;
    } else {
      CommentLike l = new CommentLike();
      l.setCommentId(comment.getIdComment());
      l.setUserCorreo(correo);
      l.setCreatedAt(LocalDateTime.now());
      commentLikeRepo.save(l);
      likedByMe = true;
    }

    long count = commentLikeRepo.countByCommentId(comment.getIdComment());
    return new LikeSummaryResponse(count, likedByMe);
  }

  @Override
  public LikeSummaryResponse getCommentLikes(Integer commentId, String correoOrNull) {
    long count = commentLikeRepo.countByCommentId(commentId);

    Boolean likedByMe = null;
    if (correoOrNull != null) {
      likedByMe = commentLikeRepo.findByCommentIdAndUserCorreo(commentId, correoOrNull).isPresent();
    }

    return new LikeSummaryResponse(count, likedByMe);
  }
}
