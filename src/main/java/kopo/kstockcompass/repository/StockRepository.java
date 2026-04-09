package kopo.kstockcompass.repository;

// 역할: 자바 코드와 실제 DB(MariaDB) 사이를 연결하는 **'전화기'**입니다.
//     만든 것: StockRepository (인터페이스)
//     특징: JpaRepository를 상속받음으로써, 직접 SQL을 짜지 않아도 데이터를 저장(save), 조회(findById)할 수 있는 강력한 무기를 장착했습니다.
//     스타일: 요즘 트렌드에 맞춰 @Repository 없이도 작동하는 깔끔한 구조를 선택했습니다.

import kopo.kstockcompass.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}