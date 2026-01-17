package com.karrot.zeronine.core.infrastructure.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Kafka 기반 이벤트 발행자
 * 비동기 이벤트 기반 아키텍처의 핵심 컴포넌트
 */
@Component
class EventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    companion object {
        const val ORDER_TOPIC = "karrot.order.events"
        const val DEAL_TOPIC = "karrot.deal.events"
        const val NOTIFICATION_TOPIC = "karrot.notification.events"
    }

    /**
     * 주문 이벤트 발행
     */
    fun publishOrderEvent(event: OrderEvent) {
        publish(ORDER_TOPIC, event.orderNumber, event)
    }

    /**
     * 딜 이벤트 발행
     */
    fun publishDealEvent(event: DealEvent) {
        publish(DEAL_TOPIC, event.dealId.toString(), event)
    }

    /**
     * 범용 이벤트 발행
     */
    private fun publish(topic: String, key: String, event: DomainEvent) {
        val payload = objectMapper.writeValueAsString(event)

        kafkaTemplate.send(topic, key, payload)
            .whenComplete { result, ex ->
                if (ex != null) {
                    logger.error(ex) {
                        "이벤트 발행 실패 - topic: $topic, key: $key, eventId: ${event.eventId}"
                    }
                } else {
                    logger.info {
                        "이벤트 발행 성공 - topic: $topic, key: $key, eventId: ${event.eventId}, " +
                            "partition: ${result.recordMetadata.partition()}, offset: ${result.recordMetadata.offset()}"
                    }
                }
            }
    }
}
