package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.PostRequestDto;
import kopo.kstockcompass.dto.PostResponseDto;

import java.util.List;

public interface IBoardService {
    PostResponseDto createPost(PostRequestDto requestDto, String userEmail) throws Exception;
    List<PostResponseDto> getAllPosts();
    PostResponseDto getPost(Long postId);
    PostResponseDto updatePost(Long postId, PostRequestDto requestDto, String userEmail) throws Exception;
    void deletePost(Long postId, String userEmail) throws Exception;
}