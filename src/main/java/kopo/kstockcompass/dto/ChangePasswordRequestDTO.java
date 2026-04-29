package kopo.kstockcompass.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDTO {
    // [현재 비밀번호]
    // 사용자가 진짜 본인이 맞는지 한 번 더 확인하기 위해 입력받는 값임.
    // 서버는 이 값을 받아서 DB에 암호화되어 저장된 값과 일치하는지 대조함.
    private String currentPwd;

    // [새 비밀번호]
    // 사용자가 앞으로 사용하고 싶어 하는 새로운 비밀번호임.
    // @RequestBody가 이 JSON 박스를 뜯어서(언박싱) 이 변수에 값을 넣어주면,
    // 서비스 레이어에서 BCrypt로 암호화해서 DB에 덮어씌우게 됨.
    private String newPwd;
}