package kopo.kstockcompass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequestDTO {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    private String userEmail;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String userPwd;
}