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

/**
 * [관심종목(Favorite) 관리 서비스]
 *
 * 설명:
 * 사용자가 특정 종목을 관심종목으로 등록/조회/삭제할 수 있도록
 * 핵심 비즈니스 로직을 담당하는 서비스 계층입니다.
 *
 * 주요 기능:
 * 1. 관심종목 목록 조회
 * 2. 관심종목 등록
 * 3. 관심종목 삭제
 * 4. 특정 종목의 관심종목 등록 여부 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService implements IFavoriteService {

    // 관심종목 테이블 접근 Repository
    private final FavoriteRepository favoriteRepository;

    // 종목 마스터 테이블 접근 Repository
    private final StockRepository stockRepository;

    // 화면 출력용 날짜 포맷 지정
    // 예: 2026-05-28 14:30
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * [관심종목 목록 조회]
     *
     * 역할:
     * 로그인한 사용자의 관심종목 리스트를 조회합니다.
     *
     * 동작 흐름:
     * 1. 이메일 기준으로 관심종목 목록 조회
     * 2. 종목코드(stockCd)를 이용해 종목명(stockNm) 조회
     * 3. DTO(Record) 형태로 변환 후 반환
     *
     * 특징:
     * - Entity를 직접 반환하지 않고 DTO로 변환하여
     *   계층 간 데이터 오염을 방지합니다.
     * - 종목명이 존재하지 않을 경우 종목코드로 대체 출력합니다.
     */
    @Override
    public List<FavoriteDTO> getFavorites(String userEmail) {

        return favoriteRepository.findByUserEmail(userEmail)
                .stream()

                .map(fav -> {

                    // 종목코드 기반으로 종목명 조회
                    String stockNm = stockRepository.findById(fav.getStockCd())
                            .map(StockEntity::getStockNm)

                            // 종목명이 없을 경우 종목코드로 대체
                            .orElse(fav.getStockCd());

                    // DTO(Record)로 변환
                    return new FavoriteDTO(
                            fav.getFavId(),
                            fav.getUserEmail(),
                            fav.getStockCd(),
                            stockNm,
                            fav.getAddDt().format(DATE_FMT)
                    );
                })

                // 읽기 전용 리스트로 반환
                .toList();
    }

    /**
     * [관심종목 추가]
     *
     * 역할:
     * 사용자가 특정 종목을 관심종목으로 등록합니다.
     *
     * 설명:
     * 동일 종목이 이미 등록되어 있는지 먼저 검사하여
     * 중복 데이터 생성을 방지합니다.
     *
     * 특징:
     * - @Setter 없이 Builder 패턴만 사용
     * - 중복 등록 방지 로직 포함
     * - 트랜잭션 처리로 데이터 일관성 보장
     */
    @Override
    @Transactional
    public void addFavorite(String userEmail, String stockCd) {

        // 이미 등록된 관심종목인지 검사
        if (favoriteRepository.findByUserEmailAndStockCd(userEmail, stockCd).isPresent()) {

            // 중복 등록 방지
            throw new IllegalStateException("이미 관심종목에 추가된 종목입니다.");
        }

        // Builder 패턴 기반 엔티티 생성
        FavoriteEntity entity = FavoriteEntity.builder()
                .userEmail(userEmail)
                .stockCd(stockCd)
                .build();

        // DB 저장
        favoriteRepository.save(entity);

        log.info("관심종목 추가: {} - {}", userEmail, stockCd);
    }

    /**
     * [관심종목 삭제]
     *
     * 역할:
     * 사용자의 관심종목을 제거합니다.
     *
     * 설명:
     * 이메일 + 종목코드 조건으로 삭제하여
     * 다른 사용자의 관심종목 데이터가 삭제되지 않도록 보호합니다.
     */
    @Override
    @Transactional
    public void deleteFavorite(String userEmail, String stockCd) {

        // 사용자 이메일 + 종목코드 기준 삭제
        favoriteRepository.deleteByUserEmailAndStockCd(userEmail, stockCd);

        log.info("관심종목 삭제: {} - {}", userEmail, stockCd);
    }

    /**
     * [관심종목 등록 여부 확인]
     *
     * 역할:
     * 특정 종목이 관심종목에 등록되어 있는지 여부를 확인합니다.
     *
     * 사용 예시:
     * - 프론트엔드의 관심종목 별(⭐) 활성화 여부
     * - "이미 추가됨" 버튼 처리
     *
     * 반환값:
     * true  -> 이미 관심종목에 존재
     * false -> 관심종목에 없음
     */
    @Override
    public boolean isFavorite(String userEmail, String stockCd) {

        return favoriteRepository
                .findByUserEmailAndStockCd(userEmail, stockCd)
                .isPresent();
    }
}