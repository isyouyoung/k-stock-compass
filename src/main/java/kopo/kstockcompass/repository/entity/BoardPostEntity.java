package kopo.kstockcompass.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "BOARD_POST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BoardPostEntity {

    /**
     * [PK 생성 전략: IDENTITY]
     * - 데이터베이스에 기본키 생성을 위임하는 방식 (예: MySQL의 Auto Increment)
     * - JPA가 영속성 컨텍스트에 저장할 때 INSERT 쿼리를 즉시 날려 DB에서 PK를 받아옴
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "POST_ID")
    private Long postId;

    /**
     * [외래키(FK) 및 연관관계 매핑: LAZY 로딩]
     * - UserInfoEntity와 N:1 다대일 관계
     * - @JoinColumn: 외래키 컬럼명을 "USER_EMAIL"로 지정
     * - FetchType.LAZY: 게시글 조회 시 연관된 유저 정보를 당장 가져오지 않고,
     *   실제로 유저 객체에 접근(예: post.getUser().getName())하는 시점에 쿼리를 날려 성능 최적화(N+1 문제 방어)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_EMAIL", nullable = false)
    private UserInfoEntity user;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    /**
     * [컬럼 타입 정의: TEXT]
     * - 게시글 본문은 글자 수가 많을 수 있으므로 기본 VARCHAR(255) 대신
     *   DB의 대용량 텍스트 타입(TEXT)으로 생성되도록 columnDefinition 설정
     */
    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "REG_DT", nullable = false, updatable = false)
    private LocalDateTime regDt;

    @Column(name = "CHG_DT")
    private LocalDateTime chgDt;

    // 게시글 수정 비즈니스 메서드
    public void updatePost(String title, String content) {
        this.title = title;
        this.content = content;
        this.chgDt = LocalDateTime.now();
    }
}