package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {

    void deleteByUserEmail(String userEmail); // PK가 userEmail이라 deleteById도 가능
}