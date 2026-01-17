package com.karrot.zeronine.api.dto

import com.karrot.zeronine.core.domain.order.Order
import com.karrot.zeronine.core.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 생성 요청 DTO
 */
data class CreateOrderRequest(
    val dealUuid: String,
    val quantity: Int
) {
    init {
        require(dealUuid.isNotBlank()) { "딜 UUID는 필수입니다" }
        require(quantity > 0) { "수량은 0보다 커야 합니다" }
    }
}

/**
 * 주문 응답 DTO
 */
data class OrderResponse(
    val uuid: String,
    val orderNumber: String,
    val dealId: Long,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val paidAt: LocalDateTime?,
    val cancelledAt: LocalDateTime?,
    val cancelReason: String?,
    val createdAt: LocalDateTime
)

/**
 * Order -> OrderResponse 변환 확장 함수
 */
fun Order.toResponse(): OrderResponse = OrderResponse(
    uuid = uuid,
    orderNumber = orderNumber,
    dealId = dealId,
    quantity = quantity,
    unitPrice = unitPrice,
    totalAmount = totalAmount,
    status = status,
    paidAt = paidAt,
    cancelledAt = cancelledAt,
    cancelReason = cancelReason,
    createdAt = createdAt
)

/**
 * 주문 취소 요청 DTO
 */
data class CancelOrderRequest(
    val reason: String
) {
    init {
        require(reason.isNotBlank()) { "취소 사유는 필수입니다" }
    }
}
