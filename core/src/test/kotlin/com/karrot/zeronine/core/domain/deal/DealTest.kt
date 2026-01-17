package com.karrot.zeronine.core.domain.deal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Deal 엔티티 단위 테스트
 * Kotest DescribeSpec 스타일 사용
 */
class DealTest : DescribeSpec({

    describe("Deal 생성") {
        context("유효한 데이터로 생성할 때") {
            it("딜이 정상적으로 생성되어야 한다") {
                val deal = createTestDeal()

                deal.title shouldBe "테스트 딜"
                deal.status shouldBe DealStatus.PENDING
                deal.remainingStock shouldBe 100
                deal.uuid shouldNotBe null
            }

            it("할인율이 올바르게 계산되어야 한다") {
                val deal = createTestDeal(
                    originalPrice = BigDecimal(10000),
                    dealPrice = BigDecimal(7000)
                )

                deal.discountRate shouldBe 30
            }
        }
    }

    describe("딜 활성화") {
        context("PENDING 상태일 때") {
            it("ACTIVE 상태로 변경되어야 한다") {
                val deal = createTestDeal(
                    startTime = LocalDateTime.now().plusHours(1)
                )

                deal.activate()

                deal.status shouldBe DealStatus.ACTIVE
            }
        }

        context("이미 ACTIVE 상태일 때") {
            it("예외가 발생해야 한다") {
                val deal = createTestDeal(
                    startTime = LocalDateTime.now().plusHours(1)
                )
                deal.activate()

                shouldThrow<IllegalStateException> {
                    deal.activate()
                }
            }
        }
    }

    describe("재고 차감") {
        context("충분한 재고가 있을 때") {
            it("재고가 정상적으로 차감되어야 한다") {
                val deal = createTestDeal(totalStock = 100)

                deal.decreaseStock(10)

                deal.remainingStock shouldBe 90
            }
        }

        context("재고가 부족할 때") {
            it("예외가 발생해야 한다") {
                val deal = createTestDeal(totalStock = 5)

                shouldThrow<IllegalStateException> {
                    deal.decreaseStock(10)
                }
            }
        }

        context("재고가 모두 소진될 때") {
            it("SOLD_OUT 상태로 변경되어야 한다") {
                val deal = createTestDeal(totalStock = 10)

                deal.decreaseStock(10)

                deal.remainingStock shouldBe 0
                deal.status shouldBe DealStatus.SOLD_OUT
            }
        }

        context("차감 수량이 0 이하일 때") {
            it("예외가 발생해야 한다") {
                val deal = createTestDeal()

                shouldThrow<IllegalArgumentException> {
                    deal.decreaseStock(0)
                }

                shouldThrow<IllegalArgumentException> {
                    deal.decreaseStock(-1)
                }
            }
        }
    }

    describe("딜 활성 여부 확인") {
        context("활성 상태이고 시간 범위 내이며 재고가 있을 때") {
            it("true를 반환해야 한다") {
                val deal = createTestDeal(
                    startTime = LocalDateTime.now().minusHours(1),
                    endTime = LocalDateTime.now().plusHours(1),
                    totalStock = 10
                ).apply { status = DealStatus.ACTIVE }

                deal.isActive() shouldBe true
            }
        }

        context("PENDING 상태일 때") {
            it("false를 반환해야 한다") {
                val deal = createTestDeal()

                deal.isActive() shouldBe false
            }
        }

        context("재고가 없을 때") {
            it("false를 반환해야 한다") {
                val deal = createTestDeal(totalStock = 0)
                    .apply { status = DealStatus.ACTIVE }

                deal.isActive() shouldBe false
            }
        }
    }
})

/**
 * 테스트용 Deal 생성 헬퍼 함수
 * Type-safe Builder 패턴 활용
 */
private fun createTestDeal(
    title: String = "테스트 딜",
    description: String = "테스트 설명",
    originalPrice: BigDecimal = BigDecimal(10000),
    dealPrice: BigDecimal = BigDecimal(7000),
    totalStock: Int = 100,
    startTime: LocalDateTime = LocalDateTime.now().plusHours(1),
    endTime: LocalDateTime = LocalDateTime.now().plusDays(1),
    regionCode: String = "SEOUL-01",
    sellerId: Long = 1L
): Deal = Deal(
    title = title,
    description = description,
    originalPrice = originalPrice,
    dealPrice = dealPrice,
    totalStock = totalStock,
    remainingStock = totalStock,
    startTime = startTime,
    endTime = endTime,
    regionCode = regionCode,
    sellerId = sellerId
)
