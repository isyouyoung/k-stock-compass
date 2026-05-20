package kopo.kstockcompass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AlertRequestDTO {

    @NotBlank(message = "종목 코드는 필수입니다.")
    private String stockCd;

    @NotNull(message = "목표가는 필수입니다.")
    @Min(value = 1, message = "목표가는 0원보다 커야 합니다.")
    private Long targetPrice;

    @NotBlank(message = "알림 방향은 필수입니다.")
    private String direction;
}