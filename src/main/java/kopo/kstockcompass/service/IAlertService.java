package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.AlertDTO;
import kopo.kstockcompass.dto.AlertLogDTO;

import java.util.List;

public interface IAlertService {

    // 알림 목록 조회
    List<AlertDTO> getAlerts(String userEmail);

    // 알림 등록
    void addAlert(String userEmail, String stockCd, Long targetPrice, String direction);

    // 알림 삭제
    void deleteAlert(Long alertId, String userEmail);

    // 알림 로그 조회 (내 알림 내역)
    List<AlertLogDTO> getAlertLogs(String userEmail);

    // 알림 로그 읽음 처리
    void markAsRead(Long logId, String userEmail);

    // 알림 로그 삭제
    void deleteAlertLog(Long logId, String userEmail);

    // 알림 수정
    void updateAlert(Long alertId, String userEmail, Long targetPrice, String direction);

}