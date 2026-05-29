package kopo.kstockcompass.service.impl;

import kopo.kstockcompass.dto.AlertDTO;
import kopo.kstockcompass.dto.AlertLogDTO;
import kopo.kstockcompass.repository.AlertLogRepository;
import kopo.kstockcompass.repository.AlertRepository;
import kopo.kstockcompass.repository.StockRepository;
import kopo.kstockcompass.repository.entity.AlertEntity;
import kopo.kstockcompass.repository.entity.AlertLogEntity;
import kopo.kstockcompass.service.IAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * [지정가 알림 서비스]
 * 설명:
 * 사용자가 설정한 목표가 알림을 등록, 조회, 삭제하고
 * 알림 발생 로그(AlertLog)를 관리하는 서비스 계층입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService implements IAlertService {

    private final AlertRepository alertRepository;
    private final AlertLogRepository alertLogRepository;
    private final StockRepository stockRepository;

    // 화면 출력용 날짜 포맷
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * [알림 목록 조회]
     * 역할:
     * 특정 사용자가 등록한 알림 목록을 조회합니다.
     *
     * 설명:
     * AlertEntity에는 종목 코드(stockCd)만 존재하므로
     * 화면에 종목명을 함께 보여주기 위해 Stock 테이블에서 종목명을 조회합니다.
     */
    @Override
    public List<AlertDTO> getAlerts(String userEmail) {

        return alertRepository.findByUserEmail(userEmail)
                .stream()
                .map(alert -> {

                    // 종목 코드로 종목명 조회
                    String stockNm = stockRepository.findById(alert.getStockCd())
                            .map(s -> s.getStockNm())
                            .orElse(alert.getStockCd());

                    // DTO(Record) 구조로 변환
                    return new AlertDTO(
                            alert.getAlertId(),
                            alert.getUserEmail(),
                            alert.getStockCd(),
                            stockNm,
                            alert.getTargetPrice(),
                            alert.getRegDt().format(DATE_FMT),
                            alert.getDirection()
                    );
                })
                .toList();
    }

    /**
     * [알림 등록]
     * 역할:
     * 사용자가 원하는 목표가 조건을 저장합니다.
     *
     * 설명:
     * 엔티티 생성 시 Setter 대신 Builder 패턴을 사용하여
     * 객체 상태를 보다 안전하게 관리합니다.
     *
     * 금액 데이터는 BigDecimal로 변환하여 저장합니다.
     */
    @Override
    @Transactional
    public void addAlert(String userEmail, String stockCd, Long targetPrice, String direction) {

        AlertEntity entity = AlertEntity.builder()
                .userEmail(userEmail)
                .stockCd(stockCd)
                .targetPrice(BigDecimal.valueOf(targetPrice))
                .direction(direction)
                .build();

        alertRepository.save(entity);

        log.info("알림 등록: 유저={} - 종목={} (목표가: {}, 방향: {})",
                userEmail, stockCd, targetPrice, direction);
    }

    /**
     * [알림 삭제]
     * 역할:
     * 사용자가 등록한 알림을 삭제합니다.
     *
     * 설명:
     * alertId와 userEmail을 함께 조건으로 사용하여
     * 본인의 알림만 삭제 가능하도록 처리했습니다.
     */
    @Override
    @Transactional
    public void deleteAlert(Long alertId, String userEmail) {

        alertRepository.deleteByAlertIdAndUserEmail(alertId, userEmail);

        log.info("알림 삭제: alertId={}, 요청유저={}", alertId, userEmail);
    }

    /**
     * [알림 로그 조회]
     * 역할:
     * 목표가 조건이 충족되어 발생한 알림 로그를 조회합니다.
     *
     * 설명:
     * 먼저 사용자의 알림 ID 목록을 조회한 뒤,
     * IN 조건 조회(findByAlertIdIn)를 사용하여
     * 관련 로그를 한 번에 조회합니다.
     */
    @Override
    public List<AlertLogDTO> getAlertLogs(String userEmail) {

        // 사용자의 알림 ID 목록 조회
        List<Long> alertIds = alertRepository.findByUserEmail(userEmail)
                .stream()
                .map(AlertEntity::getAlertId)
                .toList();

        // 알림이 없으면 빈 리스트 반환
        if (alertIds.isEmpty()) {
            return List.of();
        }

        return alertLogRepository.findByAlertIdIn(alertIds)
                .stream()
                .map(log -> {

                    // alertId를 통해 종목 코드 조회
                    String stockCd = alertRepository.findById(log.getAlertId())
                            .map(a -> a.getStockCd())
                            .orElse("");

                    // 종목명 조회
                    String stockNm = stockRepository.findById(stockCd)
                            .map(s -> s.getStockNm())
                            .orElse(stockCd);

                    return new AlertLogDTO(
                            log.getLogId(),
                            log.getAlertId(),
                            stockCd,
                            stockNm,
                            log.getMsg(),
                            log.getIsRead(),
                            log.getSendDt().format(DATE_FMT)
                    );
                })
                .toList();
    }

    /**
     * [알림 읽음 처리]
     * 역할:
     * 알림 메시지를 읽음 상태(Y)로 변경합니다.
     *
     * 설명:
     * Setter 대신 Builder를 사용하여
     * 기존 데이터를 기반으로 수정용 객체를 생성합니다.
     *
     * 또한 현재 로그인 사용자가 실제 알림 소유자인지 검증 후 처리합니다.
     */
    @Override
    @Transactional
    public void markAsRead(Long logId, String userEmail) {

        alertLogRepository.findById(logId).ifPresent(entity -> {

            // 현재 로그인 유저와 알림 소유자 검증
            alertRepository.findById(entity.getAlertId())
                    .filter(alert -> alert.getUserEmail().equals(userEmail))
                    .ifPresent(alert -> {

                        // 기존 PK(logId)를 유지하여 기존 데이터 수정
                        AlertLogEntity updated = AlertLogEntity.builder()
                                .logId(entity.getLogId())
                                .alertId(entity.getAlertId())
                                .msg(entity.getMsg())
                                .sendDt(entity.getSendDt())
                                .isRead("Y")
                                .build();

                        alertLogRepository.save(updated);

                        log.info("알림 읽음 처리 완료: logId={}, 유저={}",
                                logId, userEmail);
                    });
        });
    }
}