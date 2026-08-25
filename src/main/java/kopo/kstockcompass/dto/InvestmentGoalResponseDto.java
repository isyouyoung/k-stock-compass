package kopo.kstockcompass.dto;

import kopo.kstockcompass.repository.entity.UserInvestmentGoalEntity;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter; // 💡 포맷터 import 추가

@Builder
public record InvestmentGoalResponseDto(
        BigDecimal targetProfitRate,
        String updatedAt // 💡 프론트엔드 편의를 위해 String으로 변경
) {
    public static InvestmentGoalResponseDto from(UserInvestmentGoalEntity entity) {
        // 💡 게시판(PostResponseDto)과 완벽히 동일한 포맷 양식 적용
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 데이터가 없을 경우(null)를 대비한 안전한 방어 코드
        String formattedDate = (entity.getUpdatedAt() != null)
                ? entity.getUpdatedAt().format(formatter)
                : "";

        return InvestmentGoalResponseDto.builder()
                .targetProfitRate(entity.getTargetProfitRate())
                .updatedAt(formattedDate) // 💡 변환된 문자열 삽입
                .build();
    }
}