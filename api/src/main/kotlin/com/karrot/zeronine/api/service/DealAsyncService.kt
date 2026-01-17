package com.karrot.zeronine.api.service

import com.karrot.zeronine.api.dto.DealResponse
import com.karrot.zeronine.api.dto.DealSummaryResponse
import com.karrot.zeronine.api.dto.toResponse
import com.karrot.zeronine.api.dto.toSummary
import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Kotlin Coroutines 기반 비동기 딜 서비스
 * Non-blocking I/O로 처리량(Throughput) 극대화
 */
@Service
class DealAsyncService(
    private val dealRepository: DealRepository,
    private val stockRedisService: StockRedisService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 여러 지역의 활성 딜을 병렬로 조회
     * Coroutines async/await 패턴 활용
     */
    suspend fun getActiveDealsInMultipleRegions(
        regionCodes: List<String>
    ): Map<String, List<DealSummaryResponse>> = coroutineScope {
        logger.info { "다중 지역 딜 조회 시작: ${regionCodes.size}개 지역" }

        val now = LocalDateTime.now()

        // 각 지역별로 병렬 조회
        val deferredResults = regionCodes.map { regionCode ->
            async {
                regionCode to dealRepository.findActiveDealsInRegion(regionCode, now)
                    .map { it.toSummary() }
            }
        }

        // 모든 결과 수집
        deferredResults.awaitAll().toMap().also {
            logger.info { "다중 지역 딜 조회 완료: 총 ${it.values.sumOf { deals -> deals.size }}개 딜" }
        }
    }

    /**
     * 딜 상세 정보와 실시간 재고를 병렬로 조회
     */
    suspend fun getDealWithRealTimeStock(dealUuid: String): DealResponse = coroutineScope {
        val deal = dealRepository.findByUuid(dealUuid)
            ?: throw NoSuchElementException("딜을 찾을 수 없습니다: $dealUuid")

        // 딜 정보와 Redis 재고를 병렬로 조회
        val stockDeferred = async {
            stockRedisService.getStock(deal.id)
        }

        val response = deal.toResponse()
        val realTimeStock = stockDeferred.await()

        // 실시간 재고 반영
        if (realTimeStock > 0 && deal.isActive()) {
            response.copy(remainingStock = realTimeStock)
        } else {
            response
        }
    }

    /**
     * 여러 딜의 실시간 재고를 병렬 조회
     */
    suspend fun getRealTimeStocks(dealIds: List<Long>): Map<Long, Int> = coroutineScope {
        dealIds.map { dealId ->
            async {
                dealId to stockRedisService.getStock(dealId)
            }
        }.awaitAll().toMap()
    }

    /**
     * 비동기 딜 활성화 (백그라운드 처리)
     */
    fun activateDealAsync(dealId: Long) {
        scope.launch {
            try {
                val deal = dealRepository.findById(dealId).orElse(null)
                if (deal != null && deal.canActivate()) {
                    deal.activate()
                    stockRedisService.initializeStock(deal.id, deal.remainingStock)
                    logger.info { "딜 비동기 활성화 완료: ${deal.uuid}" }
                }
            } catch (e: Exception) {
                logger.error(e) { "딜 비동기 활성화 실패: $dealId" }
            }
        }
    }

    /**
     * 서비스 종료 시 코루틴 스코프 정리
     */
    fun shutdown() {
        scope.cancel()
    }
}
