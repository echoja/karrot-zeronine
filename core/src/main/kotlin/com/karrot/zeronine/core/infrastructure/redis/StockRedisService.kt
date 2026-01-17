package com.karrot.zeronine.core.infrastructure.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Redis 재고 관리 서비스
 * Lua Script를 사용한 원자적 재고 차감
 */
@Service
class StockRedisService(
    private val redissonClient: RedissonClient
) {
    companion object {
        private const val STOCK_KEY_PREFIX = "deal:stock:"
        private val STOCK_TTL = Duration.ofHours(24)

        /**
         * Lua Script: 재고 차감
         * 원자성 보장으로 Race Condition 방지
         */
        private val DECREASE_STOCK_SCRIPT = """
            local key = KEYS[1]
            local quantity = tonumber(ARGV[1])
            local current = tonumber(redis.call('GET', key) or '0')

            if current < quantity then
                return -1  -- 재고 부족
            end

            local remaining = current - quantity
            redis.call('SET', key, remaining)
            return remaining
        """.trimIndent()

        /**
         * Lua Script: 재고 복구 (주문 취소 시)
         */
        private val RESTORE_STOCK_SCRIPT = """
            local key = KEYS[1]
            local quantity = tonumber(ARGV[1])
            local current = tonumber(redis.call('GET', key) or '0')

            local newStock = current + quantity
            redis.call('SET', key, newStock)
            return newStock
        """.trimIndent()
    }

    private fun stockKey(dealId: Long): String = "$STOCK_KEY_PREFIX$dealId"

    /**
     * 재고 초기화 (딜 활성화 시)
     */
    fun initializeStock(dealId: Long, stock: Int) {
        val bucket = redissonClient.getBucket<Int>(stockKey(dealId))
        bucket.set(stock, STOCK_TTL)
        logger.info { "딜 $dealId 재고 초기화: $stock" }
    }

    /**
     * 현재 재고 조회
     */
    fun getStock(dealId: Long): Int {
        val bucket = redissonClient.getBucket<Int>(stockKey(dealId))
        return bucket.get() ?: 0
    }

    /**
     * 재고 차감 (Lua Script 사용)
     * @return 남은 재고 (-1이면 재고 부족)
     */
    fun decreaseStock(dealId: Long, quantity: Int): Long {
        val script = redissonClient.script
        val result = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            DECREASE_STOCK_SCRIPT,
            RScript.ReturnType.INTEGER,
            listOf(stockKey(dealId)),
            quantity
        )

        if (result >= 0) {
            logger.info { "딜 $dealId 재고 차감: $quantity, 남은 재고: $result" }
        } else {
            logger.warn { "딜 $dealId 재고 부족. 요청: $quantity" }
        }

        return result
    }

    /**
     * 재고 복구 (주문 취소 시)
     */
    fun restoreStock(dealId: Long, quantity: Int): Long {
        val script = redissonClient.script
        val result = script.eval<Long>(
            RScript.Mode.READ_WRITE,
            RESTORE_STOCK_SCRIPT,
            RScript.ReturnType.INTEGER,
            listOf(stockKey(dealId)),
            quantity
        )

        logger.info { "딜 $dealId 재고 복구: $quantity, 현재 재고: $result" }
        return result
    }

    /**
     * 재고 삭제 (딜 종료 시)
     */
    fun deleteStock(dealId: Long) {
        val bucket = redissonClient.getBucket<Int>(stockKey(dealId))
        bucket.delete()
        logger.info { "딜 $dealId 재고 정보 삭제" }
    }
}
