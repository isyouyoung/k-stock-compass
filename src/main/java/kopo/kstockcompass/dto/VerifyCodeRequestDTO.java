package kopo.kstockcompass.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class VerifyCodeRequestDTO {
    private String userEmail;
    private String verifyCode;
}