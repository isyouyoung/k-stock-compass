package kopo.kstockcompass.controller;

import kopo.kstockcompass.config.JwtProvider;
import kopo.kstockcompass.dto.FavoriteDTO;
import kopo.kstockcompass.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final IFavoriteService favoriteService;
    private final JwtProvider jwtProvider;

    // 관심종목 목록 조회
    @GetMapping
    public ResponseEntity<List<FavoriteDTO>> getFavorites(
            @RequestHeader("Authorization") String token) {
        String userEmail = getEmail(token);
        return ResponseEntity.ok(favoriteService.getFavorites(userEmail));
    }

    // 관심종목 추가
    @PostMapping
    public ResponseEntity<String> addFavorite(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        String userEmail = getEmail(token);
        String stockCd = body.get("stockCd");
        try {
            favoriteService.addFavorite(userEmail, stockCd);
            return ResponseEntity.ok("관심종목이 추가되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 관심종목 삭제
    @DeleteMapping("/{stockCd}")
    public ResponseEntity<String> deleteFavorite(
            @RequestHeader("Authorization") String token,
            @PathVariable String stockCd) {
        String userEmail = getEmail(token);
        favoriteService.deleteFavorite(userEmail, stockCd);
        return ResponseEntity.ok("관심종목이 삭제되었습니다.");
    }

    // 관심종목 여부 확인
    @GetMapping("/{stockCd}")
    public ResponseEntity<Boolean> isFavorite(
            @RequestHeader("Authorization") String token,
            @PathVariable String stockCd) {
        String userEmail = getEmail(token);
        return ResponseEntity.ok(favoriteService.isFavorite(userEmail, stockCd));
    }

    // JWT에서 이메일 추출 (Bearer 제거 후 파싱)
    private String getEmail(String token) {
        return jwtProvider.getEmail(token.replace("Bearer ", "").trim());
    }
}