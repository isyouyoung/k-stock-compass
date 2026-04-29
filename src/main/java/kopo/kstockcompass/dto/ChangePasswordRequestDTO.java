package kopo.kstockcompass.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDTO {
    private String currentPwd;
    private String newPwd;
}