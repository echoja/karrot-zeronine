package com.karrot.zeronine.batch.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Kafka 주문 이벤트 컨슈머
 * 비동기 이벤트 기반 아키텍처: 주문 이벤트를 수신하여 후속 처리
 */
@Component
class OrderEventConsumer(
    private val objectMapper: ObjectMapper
) {

    /**
     * 주문 이벤트 수신 처리
     * - 알림 발송
     * - 통계 집계
     * - 외부 시스템 연동
     */
    @KafkaListener(
        topics = [EventPublisher.ORDER_TOPIC],
        groupId = "karrot-zeronine-batch"
    )
    fun consumeOrderEvent(message: String) {
        try {
            val event = objectMapper.readTree(message)
            val eventType = event.get("eventType")?.asText()

            when (eventType) {
                "ORDER_CREATED" -> handleOrderCreated(event)
                "ORDER_PAID" -> handleOrderPaid(event)
                "ORDER_CANCELLED" -> handleOrderCancelled(event)
                else -> logger.warn { "알 수 없는 이벤트 타입: $eventType" }
            }
        } catch (e: Exception) {
            logger.error(e) { "주문 이벤트 처리 실패: $message" }
        }
    }

    private fun handleOrderCreated(event: com.fasterxml.jackson.databind.JsonNode) {
        val orderNumber = event.get("orderNumber")?.asText()
        val buyerId = event.get("buyerId")?.asLong()

        logger.info { "주문 생성 이벤트 처리: $orderNumber, 구매자: $buyerId" }

        // TODO: 알림 발송, 통계 집계 등 후속 처리
    }

    private fun handleOrderPaid(event: com.fasterxml.jackson.databind.JsonNode) {
        val orderNumber = event.get("orderNumber")?.asText()
        val totalAmount = event.get("totalAmount")?.asText()

        logger.info { "결제 완료 이벤트 처리: $orderNumber, 금액: $totalAmount" }

        // TODO: 판매자 알림, 정산 처리 등
    }

    private fun handleOrderCancelled(event: com.fasterxml.jackson.databind.JsonNode) {
        val orderNumber = event.get("orderNumber")?.asText()
        val reason = event.get("reason")?.asText()

        logger.info { "주문 취소 이벤트 처리: $orderNumber, 사유: $reason" }

        // TODO: 환불 처리, 알림 발송 등
    }
}
