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

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService implements IAlertService {

    private final AlertRepository alertRepository;
    private final AlertLogRepository alertLogRepository;
    private final StockRepository stockRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<AlertDTO> getAlerts(String userEmail) {
        return alertRepository.findByUserEmail(userEmail)
                .stream()
                .map(alert -> {
                    String stockNm = stockRepository.findById(alert.getStockCd())
                            .map(s -> s.getStockNm())
                            .orElse(alert.getStockCd());
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
        log.info("알림 등록: {} - {} (목표가: {}, 방향: {})", userEmail, stockCd, targetPrice, direction);
    }

    @Override
    @Transactional
    public void deleteAlert(Long alertId, String userEmail) {
        alertRepository.deleteByAlertIdAndUserEmail(alertId, userEmail);
        log.info("알림 삭제: alertId={}", alertId);
    }

    @Override
    public List<AlertLogDTO> getAlertLogs(String userEmail) {
        // 내 알림 ID 목록 조회
        List<Long> alertIds = alertRepository.findByUserEmail(userEmail)
                .stream()
                .map(AlertEntity::getAlertId)
                .toList();

        if (alertIds.isEmpty()) return List.of();

        return alertLogRepository.findByAlertIdIn(alertIds)
                .stream()
                .map(log -> {
                    // alertId로 종목코드 찾기
                    String stockCd = alertRepository.findById(log.getAlertId())
                            .map(a -> a.getStockCd())
                            .orElse("");
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

    @Override
    @Transactional
    public void markAsRead(Long logId, String userEmail) {
        alertLogRepository.findById(logId).ifPresent(entity -> {
            alertRepository.findById(entity.getAlertId())
                    .filter(alert -> alert.getUserEmail().equals(userEmail))
                    .ifPresent(alert -> {
                        AlertLogEntity updated = AlertLogEntity.builder()
                                .logId(entity.getLogId())
                                .alertId(entity.getAlertId())
                                .msg(entity.getMsg())
                                .sendDt(entity.getSendDt())
                                .isRead("Y")
                                .build();
                        alertLogRepository.save(updated);
                    });
        });
    }
}