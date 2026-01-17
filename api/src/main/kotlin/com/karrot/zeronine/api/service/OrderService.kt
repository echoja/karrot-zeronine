package com.karrot.zeronine.api.service

import com.karrot.zeronine.api.dto.CreateOrderRequest
import com.karrot.zeronine.api.dto.OrderResponse
import com.karrot.zeronine.api.dto.toResponse
import com.karrot.zeronine.common.util.IdGenerator
import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.domain.order.Order
import com.karrot.zeronine.core.domain.order.OrderRepository
import com.karrot.zeronine.core.domain.order.OrderStatus
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import com.karrot.zeronine.core.infrastructure.event.OrderCancelledEvent
import com.karrot.zeronine.core.infrastructure.event.OrderCreatedEvent
import com.karrot.zeronine.core.infrastructure.event.OrderPaidEvent
import com.karrot.zeronine.core.infrastructure.redis.DistributedLockService
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * 주문 서비스
 * 동시성 제어: Redis Lua Script + 분산 락
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val dealRepository: DealRepository,
    private val stockRedisService: StockRedisService,
    private val distributedLockService: DistributedLockService,
    private val eventPublisher: EventPublisher
) {

    /**
     * 주문 생성 (동시성 제어 적용)
     *
     * 1. 사용자별 중복 주문 방지 락
     * 2. Redis Lua Script로 재고 원자적 차감
     * 3. 주문 생성 및 이벤트 발행
     */
    @Transactional
    fun createOrder(request: CreateOrderRequest, buyerId: Long): OrderResponse {
        val deal = dealRepository.findByUuid(request.dealUuid)
            ?: throw NoSuchElementException("딜을 찾을 수 없습니다: ${request.dealUuid}")

        // 딜 활성 상태 확인
        check(deal.isActive()) { "현재 참여할 수 없는 딜입니다" }

        // 중복 주문 확인
        val alreadyOrdered = orderRepository.existsByDealIdAndBuyerIdAndStatusNot(
            dealId = deal.id,
            buyerId = buyerId,
            status = OrderStatus.CANCELLED
        )
        check(!alreadyOrdered) { "이미 참여한 딜입니다" }

        // 사용자별 동시 주문 방지 락
        return distributedLockService.withUserOrderLock(buyerId, deal.id) {
            // Redis에서 재고 원자적 차감
            val remainingStock = stockRedisService.decreaseStock(deal.id, request.quantity)
            check(remainingStock >= 0) { "재고가 부족합니다" }

            // DB 재고도 차감 (정합성 유지)
            deal.decreaseStock(request.quantity)

            // 주문 생성
            val order = Order.create(
                orderNumber = IdGenerator.orderNumber(),
                dealId = deal.id,
                buyerId = buyerId,
                quantity = request.quantity,
                unitPrice = deal.dealPrice
            )

            val savedOrder = orderRepository.save(order)

            // 이벤트 발행
            eventPublisher.publishOrderEvent(
                OrderCreatedEvent(
                    orderId = savedOrder.id,
                    orderNumber = savedOrder.orderNumber,
                    dealId = deal.id,
                    buyerId = buyerId,
                    quantity = request.quantity,
                    totalAmount = savedOrder.totalAmount
                )
            )

            logger.info { "주문 생성 완료: ${savedOrder.orderNumber}" }
            savedOrder.toResponse()
        } ?: throw IllegalStateException("주문 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
    }

    /**
     * 결제 완료 처리
     */
    @Transactional
    fun markOrderAsPaid(orderUuid: String): OrderResponse {
        val order = findOrderByUuid(orderUuid)
        order.markAsPaid()

        val deal = dealRepository.findById(order.dealId).orElseThrow()

        eventPublisher.publishOrderEvent(
            OrderPaidEvent(
                orderId = order.id,
                orderNumber = order.orderNumber,
                dealId = order.dealId,
                buyerId = order.buyerId,
                totalAmount = order.totalAmount
            )
        )

        logger.info { "결제 완료: ${order.orderNumber}" }
        return order.toResponse()
    }

    /**
     * 주문 취소
     */
    @Transactional
    fun cancelOrder(orderUuid: String, reason: String): OrderResponse {
        val order = findOrderByUuid(orderUuid)
        val quantity = order.quantity

        order.cancel(reason)

        // Redis 재고 복구
        stockRedisService.restoreStock(order.dealId, quantity)

        // DB 재고 복구
        dealRepository.findById(order.dealId).ifPresent {
            it.remainingStock += quantity
        }

        eventPublisher.publishOrderEvent(
            OrderCancelledEvent(
                orderId = order.id,
                orderNumber = order.orderNumber,
                dealId = order.dealId,
                quantity = quantity,
                reason = reason
            )
        )

        logger.info { "주문 취소: ${order.orderNumber}, 사유: $reason" }
        return order.toResponse()
    }

    /**
     * 주문 조회 (단건)
     */
    @Transactional(readOnly = true)
    fun getOrder(orderUuid: String): OrderResponse {
        return findOrderByUuid(orderUuid).toResponse()
    }

    /**
     * 사용자 주문 목록 조회
     */
    @Transactional(readOnly = true)
    fun getOrdersByBuyer(buyerId: Long): List<OrderResponse> {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
            .map { it.toResponse() }
    }

    private fun findOrderByUuid(orderUuid: String): Order =
        orderRepository.findByUuid(orderUuid)
            ?: throw NoSuchElementException("주문을 찾을 수 없습니다: $orderUuid")
}
