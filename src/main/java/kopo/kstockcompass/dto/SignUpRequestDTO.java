package kopo.kstockcompass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequestDTO {

    /**
     * 이메일 (아이디)
     * - 필수 입력
     * - 이메일 형식 체크
     * - DB 컬럼 길이(100자)와 일치
     */
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
    private String userEmail;

    /**
     * 비밀번호
     * - 필수 입력
     * - 길이: 4 ~ 20자
     * - 영문 + 숫자 + 특수문자(@$!%*#?&) 반드시 포함
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자에서 20자 사이여야 합니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{4,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함하여 4~20자여야 합니다."
    )
    private String userPwd;

    /**
     * 이름
     * - 필수 입력
     * - DB 컬럼 길이(50자)와 일치
     */
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String userName;

    /**
     * 휴대폰 번호
     * - 필수 입력
     * - 숫자만 입력
     * - 10~11자리 제한 (예: 01012345678)
     */
    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(
            regexp = "^[0-9]{10,11}$",
            message = "휴대폰 번호는 숫자만 10~11자리로 입력해주세요."
    )
    private String userPnum;
}