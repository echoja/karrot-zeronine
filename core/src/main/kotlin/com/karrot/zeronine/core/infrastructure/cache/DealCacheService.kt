package com.karrot.zeronine.core.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrot.zeronine.core.domain.deal.Deal
import com.karrot.zeronine.core.domain.deal.DealRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

/**
 * Redis 기반 딜 캐싱 서비스
 * README 목표: "인기 상품 캐싱"
 */
@Service
class DealCacheService(
    private val redissonClient: RedissonClient,
    private val dealRepository: DealRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val DEAL_CACHE_PREFIX = "cache:deal:"
        private const val POPULAR_DEALS_KEY = "cache:popular:deals"
        private const val REGION_DEALS_PREFIX = "cache:region:"
        private val DEAL_CACHE_TTL = Duration.ofMinutes(5)
        private val POPULAR_DEALS_TTL = Duration.ofMinutes(1)
        private val REGION_DEALS_TTL = Duration.ofMinutes(2)
    }

    /**
     * 단일 딜 캐시 조회 (Cache-Aside 패턴)
     */
    fun getDeal(dealId: Long): Deal? {
        val cacheKey = "$DEAL_CACHE_PREFIX$dealId"
        val bucket = redissonClient.getBucket<String>(cacheKey)

        // 캐시 히트
        bucket.get()?.let { cached ->
            logger.debug { "캐시 히트: $dealId" }
            return deserializeDeal(cached)
        }

        // 캐시 미스 - DB 조회 후 캐싱
        logger.debug { "캐시 미스: $dealId" }
        return dealRepository.findById(dealId).orElse(null)?.also { deal ->
            cacheDeal(deal)
        }
    }

    /**
     * 딜 캐싱
     */
    fun cacheDeal(deal: Deal) {
        val cacheKey = "$DEAL_CACHE_PREFIX${deal.id}"
        val bucket = redissonClient.getBucket<String>(cacheKey)
        bucket.set(serializeDeal(deal), DEAL_CACHE_TTL)
        logger.debug { "딜 캐싱 완료: ${deal.id}" }
    }

    /**
     * 딜 캐시 무효화
     */
    fun evictDeal(dealId: Long) {
        val cacheKey = "$DEAL_CACHE_PREFIX$dealId"
        redissonClient.getBucket<String>(cacheKey).delete()
        logger.debug { "딜 캐시 무효화: $dealId" }
    }

    /**
     * 인기 딜 목록 캐싱 (Sorted Set 활용)
     * 조회수/주문수 기반 인기도 점수로 정렬
     */
    fun incrementPopularity(dealId: Long, score: Double = 1.0) {
        val sortedSet = redissonClient.getScoredSortedSet<Long>(POPULAR_DEALS_KEY)
        sortedSet.addScore(dealId, score)
        sortedSet.expire(POPULAR_DEALS_TTL)
    }

    /**
     * 인기 딜 TOP N 조회
     */
    fun getPopularDealIds(limit: Int = 10): List<Long> {
        val sortedSet = redissonClient.getScoredSortedSet<Long>(POPULAR_DEALS_KEY)
        return sortedSet.valueRangeReversed(0, limit - 1).toList()
    }

    /**
     * 지역별 활성 딜 캐싱
     */
    fun cacheRegionDeals(regionCode: String, dealIds: List<Long>) {
        val cacheKey = "$REGION_DEALS_PREFIX$regionCode"
        val list = redissonClient.getList<Long>(cacheKey)
        list.clear()
        list.addAll(dealIds)
        list.expire(REGION_DEALS_TTL)
        logger.debug { "지역 딜 캐싱: $regionCode (${dealIds.size}개)" }
    }

    /**
     * 지역별 활성 딜 조회 (캐시 우선)
     */
    fun getRegionDealIds(regionCode: String): List<Long>? {
        val cacheKey = "$REGION_DEALS_PREFIX$regionCode"
        val list = redissonClient.getList<Long>(cacheKey)
        return if (list.isNotEmpty()) {
            logger.debug { "지역 딜 캐시 히트: $regionCode" }
            list.toList()
        } else {
            null
        }
    }

    /**
     * 지역 딜 캐시 무효화
     */
    fun evictRegionDeals(regionCode: String) {
        val cacheKey = "$REGION_DEALS_PREFIX$regionCode"
        redissonClient.getList<Long>(cacheKey).delete()
        logger.debug { "지역 딜 캐시 무효화: $regionCode" }
    }

    /**
     * 모든 캐시 초기화 (테스트용)
     */
    fun clearAllCache() {
        redissonClient.keys.deleteByPattern("$DEAL_CACHE_PREFIX*")
        redissonClient.keys.deleteByPattern("$REGION_DEALS_PREFIX*")
        redissonClient.getBucket<Any>(POPULAR_DEALS_KEY).delete()
        logger.info { "모든 딜 캐시 초기화" }
    }

    private fun serializeDeal(deal: Deal): String =
        objectMapper.writeValueAsString(DealCacheDto.from(deal))

    private fun deserializeDeal(json: String): Deal? = runCatching {
        objectMapper.readValue(json, DealCacheDto::class.java).toDeal()
    }.getOrNull()
}

/**
 * 캐시용 DTO (엔티티 직렬화 문제 방지)
 */
private data class DealCacheDto(
    val id: Long,
    val uuid: String,
    val title: String,
    val description: String,
    val originalPrice: java.math.BigDecimal,
    val dealPrice: java.math.BigDecimal,
    val totalStock: Int,
    val remainingStock: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val regionCode: String,
    val sellerId: Long,
    val status: String
) {
    companion object {
        fun from(deal: Deal) = DealCacheDto(
            id = deal.id,
            uuid = deal.uuid,
            title = deal.title,
            description = deal.description,
            originalPrice = deal.originalPrice,
            dealPrice = deal.dealPrice,
            totalStock = deal.totalStock,
            remainingStock = deal.remainingStock,
            startTime = deal.startTime,
            endTime = deal.endTime,
            regionCode = deal.regionCode,
            sellerId = deal.sellerId,
            status = deal.status.name
        )
    }

    fun toDeal(): Deal = Deal(
        title = title,
        description = description,
        originalPrice = originalPrice,
        dealPrice = dealPrice,
        totalStock = totalStock,
        remainingStock = remainingStock,
        startTime = startTime,
        endTime = endTime,
        regionCode = regionCode,
        sellerId = sellerId
    ).apply {
        status = com.karrot.zeronine.core.domain.deal.DealStatus.valueOf(this@DealCacheDto.status)
    }
}
