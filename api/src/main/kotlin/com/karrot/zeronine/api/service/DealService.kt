package com.karrot.zeronine.api.service

import com.karrot.zeronine.api.dto.CreateDealRequest
import com.karrot.zeronine.api.dto.DealResponse
import com.karrot.zeronine.api.dto.DealSummaryResponse
import com.karrot.zeronine.api.dto.toResponse
import com.karrot.zeronine.api.dto.toSummary
import com.karrot.zeronine.core.domain.deal.Deal
import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.infrastructure.event.DealActivatedEvent
import com.karrot.zeronine.core.infrastructure.event.EventPublisher
import com.karrot.zeronine.core.infrastructure.redis.StockRedisService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
class DealService(
    private val dealRepository: DealRepository,
    private val stockRedisService: StockRedisService,
    private val eventPublisher: EventPublisher
) {

    /**
     * 딜 생성
     */
    @Transactional
    fun createDeal(request: CreateDealRequest, sellerId: Long): DealResponse {
        val deal = Deal(
            title = request.title,
            description = request.description,
            originalPrice = request.originalPrice,
            dealPrice = request.dealPrice,
            totalStock = request.totalStock,
            remainingStock = request.totalStock,
            startTime = request.startTime,
            endTime = request.endTime,
            regionCode = request.regionCode,
            sellerId = sellerId
        )

        val savedDeal = dealRepository.save(deal)
        logger.info { "딜 생성 완료: ${savedDeal.uuid}" }

        return savedDeal.toResponse()
    }

    /**
     * 딜 활성화 (시작)
     */
    @Transactional
    fun activateDeal(dealUuid: String): DealResponse {
        val deal = findDealByUuid(dealUuid)

        deal.activate()

        // Redis에 재고 초기화
        stockRedisService.initializeStock(deal.id, deal.remainingStock)

        // 이벤트 발행
        eventPublisher.publishDealEvent(
            DealActivatedEvent(
                dealId = deal.id,
                title = deal.title,
                regionCode = deal.regionCode,
                startTime = deal.startTime,
                endTime = deal.endTime
            )
        )

        logger.info { "딜 활성화 완료: ${deal.uuid}" }
        return deal.toResponse()
    }

    /**
     * 딜 조회 (단건)
     */
    @Transactional(readOnly = true)
    fun getDeal(dealUuid: String): DealResponse {
        val deal = findDealByUuid(dealUuid)

        // Redis에서 실시간 재고 조회
        val redisStock = stockRedisService.getStock(deal.id)
        if (redisStock > 0 && deal.isActive()) {
            // Redis 재고가 있으면 그 값 사용 (더 정확함)
            return deal.toResponse().copy(remainingStock = redisStock)
        }

        return deal.toResponse()
    }

    /**
     * 지역별 활성 딜 목록 조회
     */
    @Transactional(readOnly = true)
    fun getActiveDealsInRegion(regionCode: String): List<DealSummaryResponse> {
        return dealRepository.findActiveDealsInRegion(regionCode, LocalDateTime.now())
            .map { it.toSummary() }
    }

    /**
     * 판매자의 딜 목록 조회
     */
    @Transactional(readOnly = true)
    fun getDealsBySeller(sellerId: Long): List<DealSummaryResponse> {
        return dealRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
            .map { it.toSummary() }
    }

    /**
     * 딜 엔티티 조회 (내부용)
     */
    internal fun findDealByUuid(dealUuid: String): Deal =
        dealRepository.findByUuid(dealUuid)
            ?: throw NoSuchElementException("딜을 찾을 수 없습니다: $dealUuid")
}
