package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.PostRequestDto;
import kopo.kstockcompass.dto.PostResponseDto;
import kopo.kstockcompass.repository.BoardPostRepository;
import kopo.kstockcompass.repository.UserInfoRepository;
import kopo.kstockcompass.repository.entity.BoardPostEntity;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import kopo.kstockcompass.service.IBoardService;
import kopo.kstockcompass.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService implements IBoardService {

    private final BoardPostRepository boardPostRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    @Transactional
    public PostResponseDto createPost(PostRequestDto requestDto, String userEmail) throws Exception {
        // JWT의 평문 이메일을 AES128로 암호화하여 DB PK 조회
        // // JWT를 통해 복호화되어 넘어온 평문 이메일(userEmail)을
        String encEmail = EncryptUtil.encAES128CBC(userEmail);

        // DB에 암호화되어 저장된 PK와 비교하기 위해 조회하는 바로 이 순간!
        UserInfoEntity user = userInfoRepository.findById(encEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        BoardPostEntity savedPost = boardPostRepository.save(requestDto.toEntity(user));
        return PostResponseDto.from(savedPost);
    }

    @Override
    public List<PostResponseDto> getAllPosts() {
        return boardPostRepository.findAllByOrderByRegDtDesc().stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    public PostResponseDto getPost(Long postId) {
        BoardPostEntity post = boardPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + postId));
        return PostResponseDto.from(post);
    }

    @Override
    @Transactional
    public PostResponseDto updatePost(Long postId, PostRequestDto requestDto, String userEmail) throws Exception {
        BoardPostEntity post = boardPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + postId));

        String encEmail = EncryptUtil.encAES128CBC(userEmail);

        // DB에 저장된 암호화 이메일과 요청 유저의 암호화 이메일 비교
        if (!post.getUser().getUserEmail().equals(encEmail)) {
            throw new IllegalArgumentException("본인의 게시글만 수정할 수 있습니다.");
        }

        post.updatePost(requestDto.title(), requestDto.content());
        return PostResponseDto.from(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, String userEmail) throws Exception {
        BoardPostEntity post = boardPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다. ID: " + postId));

        String encEmail = EncryptUtil.encAES128CBC(userEmail);

        if (!post.getUser().getUserEmail().equals(encEmail)) {
            throw new IllegalArgumentException("본인의 게시글만 삭제할 수 있습니다.");
        }

        boardPostRepository.delete(post);
    }
}