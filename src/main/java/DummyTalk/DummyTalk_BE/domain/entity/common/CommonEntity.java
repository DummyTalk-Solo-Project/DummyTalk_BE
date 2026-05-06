package DummyTalk.DummyTalk_BE.domain.entity.common;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class CommonEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // deletedAt 인덱스 필터용 (PostgreSQL partial index: WHERE is_deleted = false)
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 소프트 딜리트: isDeleted + deletedAt 동시 기록 (자식 엔티티에서 호출)
    protected void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

a    // 탈퇴 여부 확인 유틸 (isDeleted 우선, deletedAt 보조)
    public boolean isWithdrawn() {
        return Boolean.TRUE.equals(this.isDeleted);
    }
}
