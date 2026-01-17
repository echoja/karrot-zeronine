package com.karrot.zeronine.core.domain.order

import com.karrot.zeronine.core.domain.base.BaseEntityWithUuid
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 엔티티
 * Immutability: 주문 생성 후 핵심 정보는 변경 불가
 */
@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_order_buyer_id", columnList = "buyerId"),
        Index(name = "idx_order_deal_id", columnList = "dealId"),
        Index(name = "idx_order_status", columnList = "status"),
        Index(name = "idx_order_number", columnList = "orderNumber", unique = true)
    ]
)
class Order(
    @Column(nullable = false, unique = true, length = 20)
    val orderNumber: String,

    @Column(nullable = false)
    val dealId: Long,

    @Column(nullable = false)
    val buyerId: Long,

    @Column(nullable = false)
    val quantity: Int,

    @Column(nullable = false, precision = 12, scale = 2)
    val unitPrice: BigDecimal,

    @Column(nullable = false, precision = 12, scale = 2)
    val totalAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING

) : BaseEntityWithUuid() {

    @Column
    var paidAt: LocalDateTime? = null
        private set

    @Column
    var cancelledAt: LocalDateTime? = null
        private set

    @Column(length = 500)
    var cancelReason: String? = null
        private set

    /**
     * 결제 완료 처리
     */
    fun markAsPaid(): Order {
        check(status == OrderStatus.PENDING) { "결제 대기 상태가 아닙니다: $status" }
        status = OrderStatus.PAID
        paidAt = LocalDateTime.now()
        return this
    }

    /**
     * 주문 취소
     */
    fun cancel(reason: String): Order {
        check(status in listOf(OrderStatus.PENDING, OrderStatus.PAID)) {
            "취소할 수 없는 상태입니다: $status"
        }
        status = OrderStatus.CANCELLED
        cancelledAt = LocalDateTime.now()
        cancelReason = reason
        return this
    }

    /**
     * 배송 시작
     */
    fun startDelivery(): Order {
        check(status == OrderStatus.PAID) { "결제 완료 상태가 아닙니다: $status" }
        status = OrderStatus.DELIVERING
        return this
    }

    /**
     * 배송 완료
     */
    fun completeDelivery(): Order {
        check(status == OrderStatus.DELIVERING) { "배송 중 상태가 아닙니다: $status" }
        status = OrderStatus.COMPLETED
        return this
    }

    /**
     * 환불 처리
     */
    fun refund(): Order {
        check(status in listOf(OrderStatus.PAID, OrderStatus.DELIVERING)) {
            "환불할 수 없는 상태입니다: $status"
        }
        status = OrderStatus.REFUNDED
        return this
    }

    companion object {
        fun create(
            orderNumber: String,
            dealId: Long,
            buyerId: Long,
            quantity: Int,
            unitPrice: BigDecimal
        ): Order {
            require(quantity > 0) { "수량은 0보다 커야 합니다" }
            require(unitPrice > BigDecimal.ZERO) { "단가는 0보다 커야 합니다" }

            return Order(
                orderNumber = orderNumber,
                dealId = dealId,
                buyerId = buyerId,
                quantity = quantity,
                unitPrice = unitPrice,
                totalAmount = unitPrice * BigDecimal(quantity)
            )
        }
    }
}

enum class OrderStatus {
    PENDING,      // 결제 대기
    PAID,         // 결제 완료
    DELIVERING,   // 배송 중
    COMPLETED,    // 배송 완료
    CANCELLED,    // 취소됨
    REFUNDED      // 환불됨
}
