package kopo.kstockcompass.service;

import kopo.kstockcompass.dto.ChangePasswordRequestDTO;
import kopo.kstockcompass.dto.LoginRequestDTO;
import kopo.kstockcompass.dto.SignUpRequestDTO;

import java.util.Map;

public interface IUserService {

    void signUp(SignUpRequestDTO dto) throws Exception;

    Map<String, String> login(LoginRequestDTO dto) throws Exception;

    boolean checkEmail(String email) throws Exception;

    String findEmail(String userName, String userPnum) throws Exception;

    void resetPassword(String userName, String userEmail) throws Exception;

    void changePassword(String email, ChangePasswordRequestDTO dto) throws Exception;
}