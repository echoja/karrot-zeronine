package com.karrot.zeronine.batch.job

import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.domain.deal.DealStatus
import com.karrot.zeronine.core.infrastructure.event.DealClosedEvent
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * 딜 만료 처리 배치
 * 종료 시간이 지난 활성 딜을 자동으로 종료 처리
 */
@Component
class DealExpirationJob(
    private val dealRepository: DealRepository,
    private val stockRedisService: StockRedisService,
    private val eventPublisher: EventPublisher
) {

    /**
     * 매 분마다 만료된 딜 체크 및 종료 처리
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초
    @Transactional
    fun expireDeals() {
        val now = LocalDateTime.now()
        val expiredDeals = dealRepository.findExpiredActiveDeals(now)

        if (expiredDeals.isEmpty()) {
            return
        }

        logger.info { "만료된 딜 ${expiredDeals.size}개 처리 시작" }

        expiredDeals.forEach { deal ->
            try {
                deal.close()

                // Redis 재고 정보 삭제
                stockRedisService.deleteStock(deal.id)

                // 이벤트 발행 (TODO: 실제 통계 계산 필요)
                eventPublisher.publishDealEvent(
                    DealClosedEvent(
                        dealId = deal.id,
                        title = deal.title,
                        totalOrders = deal.totalStock - deal.remainingStock,
                        totalRevenue = BigDecimal.ZERO // 실제로는 주문 합계 계산 필요
                    )
                )

                logger.info { "딜 종료 완료: ${deal.uuid} - ${deal.title}" }
            } catch (e: Exception) {
                logger.error(e) { "딜 종료 처리 실패: ${deal.uuid}" }
            }
        }

        logger.info { "만료된 딜 처리 완료: ${expiredDeals.size}개" }
    }
}
