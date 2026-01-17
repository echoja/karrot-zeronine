package com.karrot.zeronine.core.testutil

import com.karrot.zeronine.core.domain.deal.Deal
import com.karrot.zeronine.core.domain.deal.DealStatus
import com.karrot.zeronine.core.domain.order.Order
import com.karrot.zeronine.core.domain.order.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 테스트 데이터 생성 DSL
 * Kotlin Type-safe Builder 패턴 활용
 *
 * 사용 예시:
 * ```
 * val deal = deal {
 *     title = "테스트 딜"
 *     originalPrice = 10000.toBigDecimal()
 *     dealPrice = 7000.toBigDecimal()
 *     stock = 100
 *     active()
 * }
 * ```
 */
@DslMarker
annotation class TestDataDsl

@TestDataDsl
class DealBuilder {
    var title: String = "테스트 타임딜"
    var description: String = "테스트용 딜 설명입니다"
    var originalPrice: BigDecimal = BigDecimal(10000)
    var dealPrice: BigDecimal = BigDecimal(7000)
    var stock: Int = 100
    var startTime: LocalDateTime = LocalDateTime.now().minusHours(1)
    var endTime: LocalDateTime = LocalDateTime.now().plusHours(23)
    var regionCode: String = "SEOUL-01"
    var sellerId: Long = 1L
    private var status: DealStatus = DealStatus.PENDING

    fun pending() {
        status = DealStatus.PENDING
        startTime = LocalDateTime.now().plusHours(1)
    }

    fun active() {
        status = DealStatus.ACTIVE
        startTime = LocalDateTime.now().minusHours(1)
        endTime = LocalDateTime.now().plusHours(23)
    }

    fun soldOut() {
        status = DealStatus.SOLD_OUT
        stock = 0
    }

    fun closed() {
        status = DealStatus.CLOSED
        endTime = LocalDateTime.now().minusHours(1)
    }

    fun startsIn(hours: Long) {
        startTime = LocalDateTime.now().plusHours(hours)
        endTime = startTime.plusDays(1)
    }

    fun endsIn(hours: Long) {
        endTime = LocalDateTime.now().plusHours(hours)
    }

    fun withDiscount(percent: Int) {
        require(percent in 1..99) { "할인율은 1~99% 사이여야 합니다" }
        dealPrice = originalPrice * BigDecimal(100 - percent) / BigDecimal(100)
    }

    fun build(): Deal {
        require(dealPrice < originalPrice) { "할인가는 원가보다 작아야 합니다" }

        return Deal(
            title = title,
            description = description,
            originalPrice = originalPrice,
            dealPrice = dealPrice,
            totalStock = stock,
            remainingStock = if (status == DealStatus.SOLD_OUT) 0 else stock,
            startTime = startTime,
            endTime = endTime,
            regionCode = regionCode,
            sellerId = sellerId
        ).apply {
            this.status = this@DealBuilder.status
        }
    }
}

@TestDataDsl
class OrderBuilder {
    var orderNumber: String = "TEST-${System.currentTimeMillis()}"
    var dealId: Long = 1L
    var buyerId: Long = 100L
    var quantity: Int = 1
    var unitPrice: BigDecimal = BigDecimal(7000)
    private var status: OrderStatus = OrderStatus.PENDING

    fun pending() { status = OrderStatus.PENDING }
    fun paid() { status = OrderStatus.PAID }
    fun delivering() { status = OrderStatus.DELIVERING }
    fun completed() { status = OrderStatus.COMPLETED }
    fun cancelled() { status = OrderStatus.CANCELLED }
    fun refunded() { status = OrderStatus.REFUNDED }

    fun build(): Order {
        val order = Order.create(
            orderNumber = orderNumber,
            dealId = dealId,
            buyerId = buyerId,
            quantity = quantity,
            unitPrice = unitPrice
        )

        // 상태 전이 적용
        when (status) {
            OrderStatus.PAID -> order.markAsPaid()
            OrderStatus.DELIVERING -> {
                order.markAsPaid()
                order.startDelivery()
            }
            OrderStatus.COMPLETED -> {
                order.markAsPaid()
                order.startDelivery()
                order.completeDelivery()
            }
            OrderStatus.CANCELLED -> order.cancel("테스트 취소")
            OrderStatus.REFUNDED -> {
                order.markAsPaid()
                order.refund()
            }
            else -> { /* PENDING - 기본 상태 */ }
        }

        return order
    }
}

@TestDataDsl
class TestScenarioBuilder {
    private val deals = mutableListOf<Deal>()
    private val orders = mutableListOf<Order>()

    fun deal(block: DealBuilder.() -> Unit): Deal {
        val deal = DealBuilder().apply(block).build()
        deals.add(deal)
        return deal
    }

    fun order(block: OrderBuilder.() -> Unit): Order {
        val order = OrderBuilder().apply(block).build()
        orders.add(order)
        return order
    }

    fun deals(): List<Deal> = deals.toList()
    fun orders(): List<Order> = orders.toList()
}

// DSL 진입점 함수들
fun deal(block: DealBuilder.() -> Unit): Deal = DealBuilder().apply(block).build()

fun order(block: OrderBuilder.() -> Unit): Order = OrderBuilder().apply(block).build()

fun testScenario(block: TestScenarioBuilder.() -> Unit): TestScenarioBuilder =
    TestScenarioBuilder().apply(block)

// 편의 함수들
fun activeDeal(title: String = "활성 딜", stock: Int = 100) = deal {
    this.title = title
    this.stock = stock
    active()
}

fun pendingDeal(title: String = "대기 딜") = deal {
    this.title = title
    pending()
}

fun soldOutDeal(title: String = "품절 딜") = deal {
    this.title = title
    soldOut()
}

fun paidOrder(dealId: Long = 1L, buyerId: Long = 100L) = order {
    this.dealId = dealId
    this.buyerId = buyerId
    paid()
}
