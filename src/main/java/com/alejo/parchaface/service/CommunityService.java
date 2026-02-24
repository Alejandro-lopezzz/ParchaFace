package com.alejo.parchaface.service;

import com.alejo.parchaface.dto.*;
import com.alejo.parchaface.model.CommunityComment;
import com.alejo.parchaface.model.CommunityPost;
import com.alejo.parchaface.model.PostRating;

import java.util.List;

public interface CommunityService {
  List<CommunityPost> listPosts(String q, String city, String category, String sort);
  CommunityPost getPost(Integer id);
  List<CommunityComment> getComments(Integer postId);

  CommunityPost createPost(CreateCommunityPostRequest req, String correo);
  CommunityComment addComment(Integer postId, CreateCommunityCommentRequest req, String correo);

  // ⭐ rating del post (por usuario)
  PostRating ratePost(Integer postId, Integer rating, String correo);
  RatingSummaryResponse getPostRatingSummary(Integer postId, String correoOrNull);

  // 👍 likes de comentarios (toggle por usuario)
  LikeSummaryResponse toggleCommentLike(Integer commentId, String correo);
  LikeSummaryResponse getCommentLikes(Integer commentId, String correoOrNull);
}
