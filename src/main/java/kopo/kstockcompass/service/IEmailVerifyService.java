package kopo.kstockcompass.service;

public interface IEmailVerifyService {

    void sendCode(String email);

    boolean verifyCode(String email, String code);
}