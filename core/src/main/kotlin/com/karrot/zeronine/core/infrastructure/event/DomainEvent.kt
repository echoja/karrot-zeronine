package com.karrot.zeronine.core.infrastructure.event

import java.time.LocalDateTime
import java.util.UUID

/**
 * 도메인 이벤트 기반 클래스
 * Kafka를 통한 비동기 이벤트 처리의 기반
 */
sealed interface DomainEvent {
    val eventId: String
    val occurredAt: LocalDateTime
    val eventType: String
}

/**
 * 주문 관련 이벤트
 */
sealed class OrderEvent : DomainEvent {
    abstract val orderId: Long
    abstract val orderNumber: String
}

data class OrderCreatedEvent(
    override val orderId: Long,
    override val orderNumber: String,
    val dealId: Long,
    val buyerId: Long,
    val quantity: Int,
    val totalAmount: java.math.BigDecimal,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "ORDER_CREATED"
) : OrderEvent()

data class OrderPaidEvent(
    override val orderId: Long,
    override val orderNumber: String,
    val dealId: Long,
    val buyerId: Long,
    val totalAmount: java.math.BigDecimal,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "ORDER_PAID"
) : OrderEvent()

data class OrderCancelledEvent(
    override val orderId: Long,
    override val orderNumber: String,
    val dealId: Long,
    val quantity: Int,
    val reason: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "ORDER_CANCELLED"
) : OrderEvent()

/**
 * 딜 관련 이벤트
 */
sealed class DealEvent : DomainEvent {
    abstract val dealId: Long
}

data class DealActivatedEvent(
    override val dealId: Long,
    val title: String,
    val regionCode: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "DEAL_ACTIVATED"
) : DealEvent()

data class DealSoldOutEvent(
    override val dealId: Long,
    val title: String,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "DEAL_SOLD_OUT"
) : DealEvent()

data class DealClosedEvent(
    override val dealId: Long,
    val title: String,
    val totalOrders: Int,
    val totalRevenue: java.math.BigDecimal,
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
    override val eventType: String = "DEAL_CLOSED"
) : DealEvent()
