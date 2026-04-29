package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;

public interface IUserService {

    void signUp(SignUpRequestDTO dto);

    String login(LoginRequestDTO dto);

    boolean checkEmail(String email);

    String findEmail(String userName, String userPnum);

    void resetPassword(String userName, String userEmail);

    void changePassword(String email, ChangePasswordRequestDTO dto);
}