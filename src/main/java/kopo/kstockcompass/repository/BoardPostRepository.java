package kopo.kstockcompass.repository;

import kopo.kstockcompass.repository.entity.BoardPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardPostRepository extends JpaRepository<BoardPostEntity, Long> {

    // 게시글 목록 최신순 조회 (1차 버전 - Query Method 방식)
    // TODO: 게시글 목록 조회 시 작성자 정보 접근으로 인한 N+1 문제 관찰 후 Fetch Join으로 리팩토링 예정
    List<BoardPostEntity> findAllByOrderByRegDtDesc();
}