package com.karrot.zeronine.batch.job

import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.domain.order.OrderRepository
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import com.karrot.zeronine.core.infrastructure.event.OrderCancelledEvent
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 미결제 주문 자동 취소 배치
 * 30분 이상 결제되지 않은 주문을 자동 취소하고 재고 복구
 */
@Component
class OrderExpirationJob(
    private val orderRepository: OrderRepository,
    private val dealRepository: DealRepository,
    private val stockRedisService: StockRedisService,
    private val eventPublisher: EventPublisher
) {
    companion object {
        private const val PAYMENT_TIMEOUT_MINUTES = 30L
    }

    /**
     * 5분마다 미결제 주문 체크 및 자동 취소
     */
    @Scheduled(cron = "0 */5 * * * *") // 매 5분마다
    @Transactional
    fun cancelExpiredPendingOrders() {
        val expireTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES)
        val expiredOrders = orderRepository.findExpiredPendingOrders(expireTime)

        if (expiredOrders.isEmpty()) {
            return
        }

        logger.info { "미결제 만료 주문 ${expiredOrders.size}개 자동 취소 시작" }

        expiredOrders.forEach { order ->
            try {
                val quantity = order.quantity
                order.cancel("결제 시간 초과로 자동 취소")

                // Redis 재고 복구
                stockRedisService.restoreStock(order.dealId, quantity)

                // DB 재고 복구
                dealRepository.findById(order.dealId).ifPresent {
                    it.remainingStock += quantity
                }

                // 이벤트 발행
                eventPublisher.publishOrderEvent(
                    OrderCancelledEvent(
                        orderId = order.id,
                        orderNumber = order.orderNumber,
                        dealId = order.dealId,
                        quantity = quantity,
                        reason = "결제 시간 초과로 자동 취소"
                    )
                )

                logger.info { "주문 자동 취소 완료: ${order.orderNumber}" }
            } catch (e: Exception) {
                logger.error(e) { "주문 자동 취소 실패: ${order.orderNumber}" }
            }
        }

        logger.info { "미결제 만료 주문 처리 완료: ${expiredOrders.size}개" }
    }
}
