package com.karrot.zeronine.core.domain.order

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal

/**
 * Order 엔티티 단위 테스트
 * Kotest BehaviorSpec (Given-When-Then) 스타일 사용
 */
class OrderTest : BehaviorSpec({

    Given("유효한 주문 데이터가 있을 때") {
        val orderNumber = "20250117-123456"
        val dealId = 1L
        val buyerId = 100L
        val quantity = 2
        val unitPrice = BigDecimal(7000)

        When("주문을 생성하면") {
            val order = Order.create(
                orderNumber = orderNumber,
                dealId = dealId,
                buyerId = buyerId,
                quantity = quantity,
                unitPrice = unitPrice
            )

            Then("주문이 정상적으로 생성되어야 한다") {
                order.orderNumber shouldBe orderNumber
                order.dealId shouldBe dealId
                order.buyerId shouldBe buyerId
                order.quantity shouldBe quantity
                order.unitPrice shouldBe unitPrice
                order.totalAmount shouldBe BigDecimal(14000)
                order.status shouldBe OrderStatus.PENDING
                order.uuid shouldNotBe null
            }
        }
    }

    Given("수량이 0 이하일 때") {
        When("주문을 생성하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalArgumentException> {
                    Order.create(
                        orderNumber = "20250117-123456",
                        dealId = 1L,
                        buyerId = 100L,
                        quantity = 0,
                        unitPrice = BigDecimal(7000)
                    )
                }
            }
        }
    }

    Given("PENDING 상태의 주문이 있을 때") {
        val order = createTestOrder()

        When("결제 완료 처리하면") {
            order.markAsPaid()

            Then("PAID 상태로 변경되어야 한다") {
                order.status shouldBe OrderStatus.PAID
                order.paidAt shouldNotBe null
            }
        }
    }

    Given("PAID 상태의 주문이 있을 때") {
        val order = createTestOrder().apply { markAsPaid() }

        When("다시 결제 완료 처리하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalStateException> {
                    order.markAsPaid()
                }
            }
        }

        When("배송을 시작하면") {
            order.startDelivery()

            Then("DELIVERING 상태로 변경되어야 한다") {
                order.status shouldBe OrderStatus.DELIVERING
            }
        }

        When("취소 요청을 하면") {
            val cancelledOrder = createTestOrder().apply { markAsPaid() }
            cancelledOrder.cancel("단순 변심")

            Then("CANCELLED 상태로 변경되어야 한다") {
                cancelledOrder.status shouldBe OrderStatus.CANCELLED
                cancelledOrder.cancelReason shouldBe "단순 변심"
                cancelledOrder.cancelledAt shouldNotBe null
            }
        }
    }

    Given("DELIVERING 상태의 주문이 있을 때") {
        val order = createTestOrder().apply {
            markAsPaid()
            startDelivery()
        }

        When("배송 완료 처리하면") {
            order.completeDelivery()

            Then("COMPLETED 상태로 변경되어야 한다") {
                order.status shouldBe OrderStatus.COMPLETED
            }
        }

        When("환불 처리하면") {
            val refundOrder = createTestOrder().apply {
                markAsPaid()
                startDelivery()
            }
            refundOrder.refund()

            Then("REFUNDED 상태로 변경되어야 한다") {
                refundOrder.status shouldBe OrderStatus.REFUNDED
            }
        }
    }

    Given("COMPLETED 상태의 주문이 있을 때") {
        val order = createTestOrder().apply {
            markAsPaid()
            startDelivery()
            completeDelivery()
        }

        When("취소 요청을 하면") {
            Then("예외가 발생해야 한다") {
                shouldThrow<IllegalStateException> {
                    order.cancel("취소 시도")
                }
            }
        }
    }
})

private fun createTestOrder(): Order = Order.create(
    orderNumber = "20250117-123456",
    dealId = 1L,
    buyerId = 100L,
    quantity = 2,
    unitPrice = BigDecimal(7000)
)
