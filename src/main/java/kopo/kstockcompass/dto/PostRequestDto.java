package kopo.kstockcompass.dto;

import kopo.kstockcompass.repository.entity.BoardPostEntity;
import kopo.kstockcompass.repository.entity.UserInfoEntity;
import lombok.Builder;

@Builder
public record PostRequestDto(
        String title,
        String content
) {
    // DTO -> Entity 변환 (Setter 없이 Builder 패턴 사용)
    public BoardPostEntity toEntity(UserInfoEntity user) {
        return BoardPostEntity.builder()
                .title(this.title)
                .content(this.content)
                .user(user)
                .build();
    }
}