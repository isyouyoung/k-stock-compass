package kopo.kstockcompass.scheduler;

import kopo.kstockcompass.repository.AlertLogRepository;
import kopo.kstockcompass.repository.AlertRepository;
import kopo.kstockcompass.repository.entity.AlertEntity;
import kopo.kstockcompass.repository.entity.AlertLogEntity;
import kopo.kstockcompass.service.impl.KisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * [주가 알림 스케줄러]
 * 역할: 주기적으로 KIS API에서 현재가를 조회하여
 *       사용자가 설정한 목표가 조건이 충족되면 알림 로그를 생성합니다.
 *
 * 실행 조건:
 * - 평일(월~금) 장 시간(09:00 ~ 15:40)에만 실행
 * - 5분마다 실행
 *
 * API 최적화:
 * - 동일 종목 알림이 여러 개여도 KIS API는 종목당 1번만 호출
 * - distinct()로 중복 종목 제거 후 Map에 현재가 캐싱
 *
 * 알림 중복 방지:
 * - 이미 읽지 않은(N) 동일 알림 로그가 있으면 재발송하지 않음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertRepository alertRepository;
    private final AlertLogRepository alertLogRepository;
    private final KisStockService kisStockService;

    /**
     * 5분마다 전체 알림 조건 체크
     * cron: 초 분 시 일 월 요일
     * "0 0/5 9-15 * * MON-FRI" = 평일 9시~15시 55분까지 5분마다
     * "0 0/5 9-14 * * MON-FRI" + "0 0-40/5 15 * * MON-FRI" 조합으로
     * 정확히 15:40까지만 실행
     */
    @Scheduled(cron = "0 0/1 9-14 * * MON-FRI")
    @Scheduled(cron = "0 0/1 15 * * MON-FRI")
    @Transactional
    public void checkAlerts() {

        // 15:41 이후면 실행 안 함 (정규장 종료 15:30, 여유 10분)
        LocalDateTime now = LocalDateTime.now();
        if (now.getHour() == 15 && now.getMinute() > 40) {
            log.info("장 마감 시간 이후 - 알림 체크 스킵");
            return;
        }

        log.info("주가 알림 스케줄러 실행: {}", now);

        // 전체 알림 목록 조회
        List<AlertEntity> alerts = alertRepository.findAll();
        if (alerts.isEmpty()) return;

        // [API 최적화] 종목코드 중복 제거 후 현재가 일괄 조회
        // 예) 삼성전자 알림 5개 → API는 삼성전자 1번만 호출
        List<String> stockCodes = alerts.stream()
                .map(AlertEntity::getStockCd)
                .distinct()
                .toList();

        // 종목코드 → 현재가 Map으로 캐싱
        Map<String, BigDecimal> priceMap = stockCodes.stream()
                .collect(Collectors.toMap(
                        code -> code,
                        code -> {
                            try {
                                return BigDecimal.valueOf(kisStockService.getCurrentPrice(code));
                            } catch (Exception e) {
                                log.warn("현재가 조회 실패: {}", code);
                                return BigDecimal.ZERO;
                            }
                        }
                ));

        log.info("종목 {}개 현재가 조회 완료 (API {}번 호출)",
                stockCodes.size(), stockCodes.size());

        // 각 알림 조건 체크
        for (AlertEntity alert : alerts) {
            try {
                BigDecimal current = priceMap.get(alert.getStockCd());

                // 현재가 조회 실패한 종목 스킵
                if (current == null || current.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal target = alert.getTargetPrice();
                boolean triggered = false;
                String msg = "";

                // 이상 조건: 현재가 >= 목표가
                if ("이상".equals(alert.getDirection())
                        && current.compareTo(target) >= 0) {
                    triggered = true;
                    msg = String.format("[%s] 현재가 %s원이 목표가 %s원 이상에 도달했습니다.",
                            alert.getStockCd(),
                            String.format("%,d", current.longValue()),
                            String.format("%,d", target.longValue()));

                // 이하 조건: 현재가 <= 목표가
                } else if ("이하".equals(alert.getDirection())
                        && current.compareTo(target) <= 0) {
                    triggered = true;
                    msg = String.format("[%s] 현재가 %s원이 목표가 %s원 이하에 도달했습니다.",
                            alert.getStockCd(),
                            String.format("%,d", current.longValue()),
                            String.format("%,d", target.longValue()));
                }

                if (triggered) {
                    // [알림 중복 방지] 아직 읽지 않은 동일 알림 있으면 재발송 안 함
                    List<AlertLogEntity> unreadLogs =
                            alertLogRepository.findByAlertId(alert.getAlertId())
                                    .stream()
                                    .filter(l -> "N".equals(l.getIsRead()))
                                    .toList();

                    if (!unreadLogs.isEmpty()) {
                        log.debug("읽지 않은 알림 있음 - 재발송 스킵: alertId={}",
                                alert.getAlertId());
                        continue;
                    }

                    // 알림 로그 저장
                    AlertLogEntity logEntity = AlertLogEntity.builder()
                            .alertId(alert.getAlertId())
                            .msg(msg)
                            .sendDt(LocalDateTime.now())
                            .isRead("N")
                            .build();

                    alertLogRepository.save(logEntity);
                    log.info("알림 발생: {}", msg);
                }

            } catch (Exception e) {
                log.warn("알림 체크 실패: alertId={}, 종목={}, 원인={}",
                        alert.getAlertId(), alert.getStockCd(), e.getMessage());
            }
        }
    }
}