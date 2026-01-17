package com.karrot.zeronine.core.domain.deal

import com.karrot.zeronine.core.domain.base.BaseEntityWithUuid
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 타임딜 엔티티 - 핵심 도메인 모델
 * Kotlin Idiomatic: data class 대신 Entity, val 최대 활용
 */
@Entity
@Table(
    name = "deals",
    indexes = [
        Index(name = "idx_deal_status", columnList = "status"),
        Index(name = "idx_deal_start_time", columnList = "startTime"),
        Index(name = "idx_deal_region_code", columnList = "regionCode")
    ]
)
class Deal(
    @Column(nullable = false, length = 200)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, precision = 12, scale = 2)
    val originalPrice: BigDecimal,

    @Column(nullable = false, precision = 12, scale = 2)
    val dealPrice: BigDecimal,

    @Column(nullable = false)
    val totalStock: Int,

    @Column(nullable = false)
    var remainingStock: Int,

    @Column(nullable = false)
    val startTime: LocalDateTime,

    @Column(nullable = false)
    val endTime: LocalDateTime,

    @Column(nullable = false, length = 10)
    val regionCode: String,

    @Column(nullable = false)
    val sellerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DealStatus = DealStatus.PENDING

) : BaseEntityWithUuid() {

    /**
     * 할인율 계산 - Extension Function 스타일
     */
    val discountRate: Int
        get() = ((originalPrice - dealPrice) * BigDecimal(100) / originalPrice).toInt()

    /**
     * 딜 활성화 가능 여부 확인
     */
    fun canActivate(): Boolean =
        status == DealStatus.PENDING && LocalDateTime.now().isBefore(startTime)

    /**
     * 딜 활성화
     */
    fun activate(): Deal {
        check(canActivate()) { "딜을 활성화할 수 없는 상태입니다: $status" }
        status = DealStatus.ACTIVE
        return this
    }

    /**
     * 재고 차감 - 동시성 제어는 Redis에서 처리
     * 이 메서드는 Redis에서 재고 확인 후 호출됨
     */
    fun decreaseStock(quantity: Int): Deal {
        require(quantity > 0) { "차감 수량은 0보다 커야 합니다" }
        check(remainingStock >= quantity) { "재고가 부족합니다. 현재 재고: $remainingStock" }
        remainingStock -= quantity
        if (remainingStock == 0) {
            status = DealStatus.SOLD_OUT
        }
        return this
    }

    /**
     * 딜 종료
     */
    fun close(): Deal {
        status = DealStatus.CLOSED
        return this
    }

    /**
     * 진행 중인 딜인지 확인
     */
    fun isActive(): Boolean {
        val now = LocalDateTime.now()
        return status == DealStatus.ACTIVE &&
            now.isAfter(startTime) &&
            now.isBefore(endTime) &&
            remainingStock > 0
    }
}

enum class DealStatus {
    PENDING,    // 대기 중
    ACTIVE,     // 진행 중
    SOLD_OUT,   // 품절
    CLOSED,     // 종료
    CANCELLED   // 취소
}
