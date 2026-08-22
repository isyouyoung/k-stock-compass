package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.PostRequestDto;
import kopo.kstockcompass.dto.PostResponseDto;
import kopo.kstockcompass.service.IBoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
// @RestController: @Controller + @ResponseBody 합친 것
// 모든 메서드의 반환값을 JSON으로 자동 변환하여 응답
@RequiredArgsConstructor
// final 필드들을 자동으로 생성자 주입 (IoC/DI 원칙)
@RequestMapping("/api/board")
// 이 컨트롤러의 모든 API 앞에 "/api/board"가 붙음
public class BoardController {

    private final IBoardService boardService; // 구현체가 아닌 인터페이스로 주입 (DI 원칙)
    private final JwtProvider jwtProvider;    // JWT 토큰에서 이메일 추출용

    /**
     * [1. 자유게시판 전체 목록 조회 API]
     * 역할: 최신 등록순으로 작성된 모든 자유게시판 글 목록을 조회
     * 리턴: List<PostResponseDto> (JSON)
     */
    @GetMapping
    public ResponseEntity<?> getAllPosts() {
        try {
            List<PostResponseDto> posts = boardService.getAllPosts();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            log.error("게시글 목록 조회 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [2. 게시글 상세 조회 API]
     * @PathVariable: URL의 postId 파라미터를 읽어옴
     * 리턴: 단일 게시글 정보 (PostResponseDto)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(@PathVariable Long postId) {
        try {
            PostResponseDto post = boardService.getPost(postId);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("게시글 상세 조회 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [3. 게시글 작성 API]
     * @RequestHeader("Authorization"): HTTP 헤더에 담긴 JWT 토큰을 읽어옴
     * 과정: Bearer 토큰 파싱 -> JWT에서 작성자 이메일 추출 -> 게시글 저장
     */
    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestHeader("Authorization") String token,
            @RequestBody PostRequestDto dto) {
        try {
            String pureToken = token.replace("Bearer ", "").trim();
            String email = jwtProvider.getEmail(pureToken);

            PostResponseDto createdPost = boardService.createPost(dto, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("게시글 작성 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [4. 게시글 수정 API]
     * 과정: JWT 토큰에서 작성자 이메일 추출 -> 본인 글 여부 검증 후 수정 처리
     */
    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @RequestBody PostRequestDto dto) {
        try {
            String pureToken = token.replace("Bearer ", "").trim();
            String email = jwtProvider.getEmail(pureToken);

            PostResponseDto updatedPost = boardService.updatePost(postId, dto, email);
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("게시글 수정 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }

    /**
     * [5. 게시글 삭제 API]
     * 과정: JWT 토큰에서 작성자 이메일 추출 -> 본인 글 여부 검증 후 DB 삭제
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        try {
            String pureToken = token.replace("Bearer ", "").trim();
            String email = jwtProvider.getEmail(pureToken);

            boardService.deletePost(postId, email);
            return ResponseEntity.ok("게시글이 성공적으로 삭제되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("게시글 삭제 중 에러 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("서버 오류가 발생했습니다.");
        }
    }
}