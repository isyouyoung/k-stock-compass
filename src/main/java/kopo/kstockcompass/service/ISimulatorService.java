package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.SimulatorDTO;

import java.util.List;

public interface ISimulatorService {

    // 시뮬레이터 목록 조회
    List<SimulatorDTO> getSimulator(String userEmail);

    // 시뮬레이터 종목 추가
    void addSimulator(String userEmail, String stockCd, String stockNm,
                      Long avgPrice, Long quantity, Long targetPrice);

    // 시뮬레이터 종목 수정 (평단가, 수량, 목표가)
    void updateSimulator(Long simId, String userEmail,
                         Long avgPrice, Long quantity, Long targetPrice);

    // 시뮬레이터 종목 삭제
    void deleteSimulator(Long simId, String userEmail);
}