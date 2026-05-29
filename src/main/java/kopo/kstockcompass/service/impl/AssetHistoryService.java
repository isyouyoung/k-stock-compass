package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.AssetHistoryDTO;
import kopo.kstockcompass.repository.AssetHistoryRepository;
import kopo.kstockcompass.repository.entity.AssetHistoryEntity;
import kopo.kstockcompass.service.IAssetHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * [자산 히스토리 서비스]
 * 설명:
 * 사용자의 총자산 변동 내역을 저장하고 조회하는 서비스 계층입니다.
 *
 * 저장된 데이터는 React 차트(Line Chart) 등에서
 * 시간 흐름에 따른 자산 변화 그래프로 활용됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetHistoryService implements IAssetHistoryService {

    private final AssetHistoryRepository assetHistoryRepository;

    // 화면 출력용 날짜 포맷 (초 단위까지 표시)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * [자산 히스토리 저장]
     * 역할:
     * 현재 시점의 총자산 데이터를 DB에 저장합니다.
     *
     * 설명:
     * 실시간 자산 그래프를 만들기 위해
     * 일정 주기(예: 3초)마다 호출될 수 있습니다.
     *
     * 엔티티 생성 시 Setter 대신 Builder 패턴을 사용하여
     * 객체 상태를 안전하게 관리합니다.
     */
    @Override
    @Transactional
    public void saveAssetHistory(String userEmail, Long totalAsset) {

        AssetHistoryEntity entity = AssetHistoryEntity.builder()
                .userEmail(userEmail)
                .totalAsset(totalAsset)
                .build();

        assetHistoryRepository.save(entity);

        log.info("자산 히스토리 저장: {} - {}원", userEmail, totalAsset);
    }

    /**
     * [자산 히스토리 조회]
     * 역할:
     * 특정 사용자의 자산 변동 데이터를 시간 순서대로 조회합니다.
     *
     * 설명:
     * 차트 데이터는 과거 → 현재 순서로 정렬되어야 하므로
     * OrderByRegDtAsc 메서드를 사용해 오름차순으로 조회합니다.
     *
     * 조회된 Entity는 DTO(Record) 형태로 변환하여 반환합니다.
     */
    @Override
    public List<AssetHistoryDTO> getAssetHistory(String userEmail) {

        return assetHistoryRepository.findByUserEmailOrderByHistIdAsc(userEmail)
                .stream()
                .map(h -> new AssetHistoryDTO(
                        h.getHistId(),
                        h.getUserEmail(),
                        h.getTotalAsset(),
                        h.getRegDt().format(DATE_FMT)
                ))
                .toList();
    }
}