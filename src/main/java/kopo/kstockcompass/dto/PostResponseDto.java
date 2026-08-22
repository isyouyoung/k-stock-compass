package kopo.kstockcompass.dto;

import kopo.kstockcompass.repository.entity.BoardPostEntity;
import lombok.Builder;

import java.time.format.DateTimeFormatter;

@Builder
public record PostResponseDto(
        Long postId,
        String title,
        String content,
        String authorEmail,
        String authorName,
        String regDt
) {
    // Entity -> DTO 변환 (Builder 패턴 기반)
    public static PostResponseDto from(BoardPostEntity entity) {
        return PostResponseDto.builder()
                .postId(entity.getPostId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .authorEmail(entity.getUser().getUserEmail())
                .authorName(entity.getUser().getUserName())
                .regDt(entity.getRegDt() != null
                        ? entity.getRegDt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "")
                .build();
    }
}