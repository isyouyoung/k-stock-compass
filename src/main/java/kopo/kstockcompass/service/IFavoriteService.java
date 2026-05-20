package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.FavoriteDTO;
import java.util.List;

public interface IFavoriteService {

    // 관심종목 목록 조회
    List<FavoriteDTO> getFavorites(String userEmail);

    // 관심종목 추가
    void addFavorite(String userEmail, String stockCd);

    // 관심종목 삭제
    void deleteFavorite(String userEmail, String stockCd);

    // 관심종목 여부 확인
    boolean isFavorite(String userEmail, String stockCd);
}