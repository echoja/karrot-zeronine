package com.karrot.zeronine.core.infrastructure.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Redisson 기반 분산 락 서비스
 * 분산 환경에서 동시성 제어를 위한 핵심 컴포넌트
 */
@Service
class DistributedLockService(
    private val redissonClient: RedissonClient
) {
    companion object {
        private const val LOCK_KEY_PREFIX = "lock:"
        private const val DEFAULT_WAIT_TIME = 5L  // seconds
        private const val DEFAULT_LEASE_TIME = 10L // seconds
    }

    /**
     * 락 획득 후 작업 수행
     * @param key 락 키
     * @param waitTime 락 대기 시간 (초)
     * @param leaseTime 락 유지 시간 (초)
     * @param action 락 획득 후 수행할 작업
     * @return 작업 결과 (락 획득 실패 시 null)
     */
    fun <T> withLock(
        key: String,
        waitTime: Long = DEFAULT_WAIT_TIME,
        leaseTime: Long = DEFAULT_LEASE_TIME,
        action: () -> T
    ): T? {
        val lock = redissonClient.getLock("$LOCK_KEY_PREFIX$key")
        val acquired = try {
            lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn { "락 획득 중 인터럽트 발생: $key" }
            return null
        }

        if (!acquired) {
            logger.warn { "락 획득 실패: $key (대기 시간: ${waitTime}초)" }
            return null
        }

        return try {
            logger.debug { "락 획득 성공: $key" }
            action()
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                logger.debug { "락 해제: $key" }
            }
        }
    }

    /**
     * 주문 처리용 락 (딜 ID 기반)
     */
    fun <T> withOrderLock(
        dealId: Long,
        action: () -> T
    ): T? = withLock("order:deal:$dealId", action = action)

    /**
     * 재고 처리용 락 (딜 ID 기반)
     */
    fun <T> withStockLock(
        dealId: Long,
        action: () -> T
    ): T? = withLock("stock:deal:$dealId", action = action)

    /**
     * 사용자별 동시 주문 방지 락
     */
    fun <T> withUserOrderLock(
        userId: Long,
        dealId: Long,
        action: () -> T
    ): T? = withLock("order:user:$userId:deal:$dealId", waitTime = 1, action = action)
}
