package com.example.backend.mappers;

import com.example.backend.dtos.response.PostResponseDTO;
import com.example.backend.dtos.response.PostDettaglioResponseDTO;
import com.example.backend.models.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class PostMapper {

    private final UserMapper userMapper;
    private final CommentMapper commentMapper;

    public PostResponseDTO toPostResponseDTO(Post post, Long currentUserId) {
        if (post == null) return null;

        return PostResponseDTO.builder()
                .id(post.getId())
                .autore(userMapper.toUtenteSummaryDTO(post.getUser()))
                .contenuto(post.getContent())
                .imageUrl(post.getImageUrl())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .hasLiked(post.isLikedByUser(currentUserId))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    public PostDettaglioResponseDTO toPostDettaglioResponseDTO(Post post, Long currentUserId) {
        if (post == null) return null;

        return PostDettaglioResponseDTO.builder()
                .id(post.getId())
                .autore(userMapper.toUtenteSummaryDTO(post.getUser()))
                .contenuto(post.getContent())
                .imageUrl(post.getImageUrl())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .hasLiked(post.isLikedByUser(currentUserId))
                .commenti(post.getComments().stream()
                        .filter(c -> !c.getIsDeletedByAuthor() && !c.isHiddenForUser(currentUserId))
                        .map(commentMapper::toCommentoResponseDTO)
                        .toList())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}