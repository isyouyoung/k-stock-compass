package kopo.kstockcompass.dto;

import kopo.kstockcompass.repository.entity.BoardPostEntity;
import kopo.kstockcompass.util.EncryptUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {

    private Long postId;
    private String title;
    private String content;
    private String authorName;
    private String authorEmail; // 👈 프론트엔드로 전달되는 이메일
    private String regDt;

    public static PostResponseDto from(BoardPostEntity entity) {
        String decEmail = "";
        try {
            // DB의 암호화된 이메일을 평문으로 복호화
            decEmail = EncryptUtil.decAES128CBC(entity.getUser().getUserEmail());
        } catch (Exception e) {
            decEmail = entity.getUser().getUserEmail();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return PostResponseDto.builder()
                .postId(entity.getPostId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .authorName(entity.getUser().getUserName())
                .authorEmail(decEmail)
                .regDt(entity.getRegDt().format(formatter))
                .build();
    }
}