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

    /**
     * [이메일 필드]
     * @Email: "youngsang@test.com" 같은 형식이 아니면 아예 입구에서 컷함.
     * @NotBlank: 스페이스바만 치거나 아예 비워두는 것을 방지함.
     */
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    private String userEmail;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String userPwd;
}