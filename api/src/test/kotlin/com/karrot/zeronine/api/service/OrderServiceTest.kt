package com.karrot.zeronine.api.service

import com.karrot.zeronine.api.dto.CreateOrderRequest
import com.karrot.zeronine.core.domain.deal.Deal
import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.domain.deal.DealStatus
import com.karrot.zeronine.core.domain.order.Order
import com.karrot.zeronine.core.domain.order.OrderRepository
import com.karrot.zeronine.core.domain.order.OrderStatus
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import com.karrot.zeronine.core.infrastructure.redis.DistributedLockService
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * OrderService 단위 테스트
 * MockK를 사용한 의존성 모킹
 */
class OrderServiceTest : DescribeSpec({

    val orderRepository = mockk<OrderRepository>()
    val dealRepository = mockk<DealRepository>()
    val stockRedisService = mockk<StockRedisService>()
    val distributedLockService = mockk<DistributedLockService>()
    val eventPublisher = mockk<EventPublisher>(relaxed = true)

    val orderService = OrderService(
        orderRepository = orderRepository,
        dealRepository = dealRepository,
        stockRedisService = stockRedisService,
        distributedLockService = distributedLockService,
        eventPublisher = eventPublisher
    )

    beforeTest {
        clearAllMocks()
    }

    describe("주문 생성") {
        val dealUuid = "test-deal-uuid"
        val buyerId = 100L
        val request = CreateOrderRequest(dealUuid = dealUuid, quantity = 2)

        context("유효한 요청일 때") {
            val deal = createTestDeal()
            val savedOrder = createTestOrder()

            beforeTest {
                every { dealRepository.findByUuid(dealUuid) } returns deal
                every { orderRepository.existsByDealIdAndBuyerIdAndStatusNot(any(), any(), any()) } returns false
                every { distributedLockService.withUserOrderLock<Any>(any(), any(), any()) } answers {
                    val action = thirdArg<() -> Any>()
                    action()
                }
                every { stockRedisService.decreaseStock(any(), any()) } returns 98L
                every { orderRepository.save(any()) } returns savedOrder
            }

            it("주문이 정상적으로 생성되어야 한다") {
                val result = orderService.createOrder(request, buyerId)

                result.orderNumber shouldBe savedOrder.orderNumber
                result.quantity shouldBe 2
                verify { stockRedisService.decreaseStock(deal.id, 2) }
                verify { eventPublisher.publishOrderEvent(any()) }
            }
        }

        context("딜이 존재하지 않을 때") {
            beforeTest {
                every { dealRepository.findByUuid(dealUuid) } returns null
            }

            it("예외가 발생해야 한다") {
                shouldThrow<NoSuchElementException> {
                    orderService.createOrder(request, buyerId)
                }
            }
        }

        context("이미 주문한 딜일 때") {
            val deal = createTestDeal()

            beforeTest {
                every { dealRepository.findByUuid(dealUuid) } returns deal
                every { orderRepository.existsByDealIdAndBuyerIdAndStatusNot(any(), any(), any()) } returns true
            }

            it("예외가 발생해야 한다") {
                shouldThrow<IllegalStateException> {
                    orderService.createOrder(request, buyerId)
                }
            }
        }

        context("재고가 부족할 때") {
            val deal = createTestDeal()

            beforeTest {
                every { dealRepository.findByUuid(dealUuid) } returns deal
                every { orderRepository.existsByDealIdAndBuyerIdAndStatusNot(any(), any(), any()) } returns false
                every { distributedLockService.withUserOrderLock<Any>(any(), any(), any()) } answers {
                    val action = thirdArg<() -> Any>()
                    action()
                }
                every { stockRedisService.decreaseStock(any(), any()) } returns -1L
            }

            it("예외가 발생해야 한다") {
                shouldThrow<IllegalStateException> {
                    orderService.createOrder(request, buyerId)
                }
            }
        }
    }

    describe("주문 취소") {
        val orderUuid = "test-order-uuid"
        val reason = "단순 변심"

        context("취소 가능한 상태일 때") {
            val order = createTestOrder()
            val dealId = order.dealId

            beforeTest {
                every { orderRepository.findByUuid(orderUuid) } returns order
                every { stockRedisService.restoreStock(any(), any()) } returns 100L
                every { dealRepository.findById(dealId) } returns java.util.Optional.of(createTestDeal())
            }

            it("주문이 취소되고 재고가 복구되어야 한다") {
                val result = orderService.cancelOrder(orderUuid, reason)

                result.status shouldBe OrderStatus.CANCELLED
                result.cancelReason shouldBe reason
                verify { stockRedisService.restoreStock(dealId, order.quantity) }
                verify { eventPublisher.publishOrderEvent(any()) }
            }
        }
    }
})

private fun createTestDeal(): Deal {
    val deal = Deal(
        title = "테스트 딜",
        description = "테스트 설명",
        originalPrice = BigDecimal(10000),
        dealPrice = BigDecimal(7000),
        totalStock = 100,
        remainingStock = 100,
        startTime = LocalDateTime.now().minusHours(1),
        endTime = LocalDateTime.now().plusHours(1),
        regionCode = "SEOUL-01",
        sellerId = 1L
    )
    deal.status = DealStatus.ACTIVE
    return deal
}

private fun createTestOrder(): Order = Order.create(
    orderNumber = "20250117-123456",
    dealId = 1L,
    buyerId = 100L,
    quantity = 2,
    unitPrice = BigDecimal(7000)
)
