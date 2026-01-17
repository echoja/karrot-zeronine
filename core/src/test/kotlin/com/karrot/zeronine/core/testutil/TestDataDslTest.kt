package com.karrot.zeronine.core.testutil

import com.karrot.zeronine.core.domain.deal.DealStatus
import com.karrot.zeronine.core.domain.order.OrderStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal

/**
 * 테스트 데이터 DSL 테스트
 */
class TestDataDslTest : DescribeSpec({

    describe("Deal DSL") {
        context("기본 빌더 사용") {
            it("기본값으로 Deal을 생성할 수 있다") {
                val deal = deal { }

                deal.title shouldBe "테스트 타임딜"
                deal.originalPrice shouldBe BigDecimal(10000)
                deal.dealPrice shouldBe BigDecimal(7000)
                deal.totalStock shouldBe 100
            }

            it("커스텀 값으로 Deal을 생성할 수 있다") {
                val deal = deal {
                    title = "특가 세일"
                    originalPrice = BigDecimal(50000)
                    dealPrice = BigDecimal(35000)
                    stock = 50
                    regionCode = "BUSAN-01"
                }

                deal.title shouldBe "특가 세일"
                deal.originalPrice shouldBe BigDecimal(50000)
                deal.dealPrice shouldBe BigDecimal(35000)
                deal.totalStock shouldBe 50
                deal.regionCode shouldBe "BUSAN-01"
            }
        }

        context("상태 설정") {
            it("active()로 활성 딜을 생성할 수 있다") {
                val deal = deal {
                    active()
                }

                deal.status shouldBe DealStatus.ACTIVE
                deal.isActive() shouldBe true
            }

            it("pending()으로 대기 딜을 생성할 수 있다") {
                val deal = deal {
                    pending()
                }

                deal.status shouldBe DealStatus.PENDING
            }

            it("soldOut()으로 품절 딜을 생성할 수 있다") {
                val deal = deal {
                    soldOut()
                }

                deal.status shouldBe DealStatus.SOLD_OUT
                deal.remainingStock shouldBe 0
            }
        }

        context("할인율 설정") {
            it("withDiscount로 할인율을 설정할 수 있다") {
                val deal = deal {
                    originalPrice = BigDecimal(10000)
                    withDiscount(30)
                }

                deal.dealPrice shouldBe BigDecimal(7000)
                deal.discountRate shouldBe 30
            }
        }

        context("편의 함수") {
            it("activeDeal()로 활성 딜을 쉽게 생성할 수 있다") {
                val deal = activeDeal("오늘의 특가", 200)

                deal.title shouldBe "오늘의 특가"
                deal.totalStock shouldBe 200
                deal.status shouldBe DealStatus.ACTIVE
            }

            it("soldOutDeal()로 품절 딜을 쉽게 생성할 수 있다") {
                val deal = soldOutDeal()

                deal.status shouldBe DealStatus.SOLD_OUT
                deal.remainingStock shouldBe 0
            }
        }
    }

    describe("Order DSL") {
        context("기본 빌더 사용") {
            it("기본값으로 Order를 생성할 수 있다") {
                val order = order { }

                order.quantity shouldBe 1
                order.unitPrice shouldBe BigDecimal(7000)
                order.status shouldBe OrderStatus.PENDING
            }
        }

        context("상태 설정") {
            it("paid()로 결제 완료 주문을 생성할 수 있다") {
                val order = order { paid() }

                order.status shouldBe OrderStatus.PAID
                order.paidAt shouldNotBe null
            }

            it("delivering()으로 배송 중 주문을 생성할 수 있다") {
                val order = order { delivering() }

                order.status shouldBe OrderStatus.DELIVERING
            }

            it("completed()로 완료 주문을 생성할 수 있다") {
                val order = order { completed() }

                order.status shouldBe OrderStatus.COMPLETED
            }
        }

        context("편의 함수") {
            it("paidOrder()로 결제 완료 주문을 쉽게 생성할 수 있다") {
                val order = paidOrder(dealId = 5L, buyerId = 200L)

                order.dealId shouldBe 5L
                order.buyerId shouldBe 200L
                order.status shouldBe OrderStatus.PAID
            }
        }
    }

    describe("TestScenario DSL") {
        it("복잡한 테스트 시나리오를 쉽게 구성할 수 있다") {
            val scenario = testScenario {
                // 여러 딜 생성
                deal {
                    title = "딜 1"
                    active()
                }
                deal {
                    title = "딜 2"
                    soldOut()
                }

                // 여러 주문 생성
                order {
                    dealId = 1L
                    paid()
                }
                order {
                    dealId = 2L
                    cancelled()
                }
            }

            scenario.deals().size shouldBe 2
            scenario.orders().size shouldBe 2

            scenario.deals()[0].status shouldBe DealStatus.ACTIVE
            scenario.deals()[1].status shouldBe DealStatus.SOLD_OUT

            scenario.orders()[0].status shouldBe OrderStatus.PAID
            scenario.orders()[1].status shouldBe OrderStatus.CANCELLED
        }
    }
})
