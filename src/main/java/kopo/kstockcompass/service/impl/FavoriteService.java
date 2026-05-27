package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.FavoriteDTO;
import kopo.kstockcompass.repository.FavoriteRepository;
import kopo.kstockcompass.repository.StockRepository;
import kopo.kstockcompass.repository.entity.FavoriteEntity;
import kopo.kstockcompass.repository.entity.StockEntity;
import kopo.kstockcompass.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StockRepository stockRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<FavoriteDTO> getFavorites(String userEmail) {
        return favoriteRepository.findByUserEmail(userEmail)
                .stream()
                .map(fav -> {
                    String stockNm = stockRepository.findById(fav.getStockCd())
                            .map(StockEntity::getStockNm)
                            .orElse(fav.getStockCd());

                    return new FavoriteDTO(
                            fav.getFavId(),
                            fav.getUserEmail(),
                            fav.getStockCd(),
                            stockNm,
                            fav.getAddDt().format(DATE_FMT)
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public void addFavorite(String userEmail, String stockCd) {
        if (favoriteRepository.findByUserEmailAndStockCd(userEmail, stockCd).isPresent()) {
            throw new IllegalStateException("이미 관심종목에 추가된 종목입니다.");
        }

        FavoriteEntity entity = FavoriteEntity.builder()
                .userEmail(userEmail)
                .stockCd(stockCd)
                .build();
        favoriteRepository.save(entity);
        log.info("관심종목 추가: {} - {}", userEmail, stockCd);
    }

    @Override
    @Transactional
    public void deleteFavorite(String userEmail, String stockCd) {
        favoriteRepository.deleteByUserEmailAndStockCd(userEmail, stockCd);
        log.info("관심종목 삭제: {} - {}", userEmail, stockCd);
    }

    @Override
    public boolean isFavorite(String userEmail, String stockCd) {
        return favoriteRepository.findByUserEmailAndStockCd(userEmail, stockCd).isPresent();
    }
}