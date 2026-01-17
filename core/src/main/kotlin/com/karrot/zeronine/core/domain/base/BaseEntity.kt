package com.karrot.zeronine.core.domain.base

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 모든 엔티티의 기본 클래스 - Immutability 원칙
 * val을 사용하여 불변성 최대화
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L

    @CreatedDate
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BaseEntity
        return id != 0L && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@MappedSuperclass
abstract class BaseEntityWithUuid : BaseEntity() {

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    val uuid: String = java.util.UUID.randomUUID().toString()
}
