package com.karrot.zeronine.core.domain.deal

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface DealRepository : JpaRepository<Deal, Long> {

    fun findByUuid(uuid: String): Deal?

    /**
     * 비관적 락을 사용한 딜 조회 (재고 차감 시 사용)
     * 동시성 제어 전략: Pessimistic Lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Deal d WHERE d.id = :id")
    fun findByIdWithPessimisticLock(@Param("id") id: Long): Deal?

    /**
     * 낙관적 락을 사용한 딜 조회
     * 동시성 제어 전략: Optimistic Lock (version 필드 필요)
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT d FROM Deal d WHERE d.id = :id")
    fun findByIdWithOptimisticLock(@Param("id") id: Long): Deal?

    /**
     * 특정 지역의 활성화된 딜 목록 조회
     */
    @Query("""
        SELECT d FROM Deal d
        WHERE d.regionCode = :regionCode
        AND d.status = 'ACTIVE'
        AND d.startTime <= :now
        AND d.endTime > :now
        ORDER BY d.startTime ASC
    """)
    fun findActiveDealsInRegion(
        @Param("regionCode") regionCode: String,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): List<Deal>

    /**
     * 곧 시작하는 딜 목록 (알림 발송용)
     */
    @Query("""
        SELECT d FROM Deal d
        WHERE d.status = 'PENDING'
        AND d.startTime BETWEEN :start AND :end
    """)
    fun findUpcomingDeals(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<Deal>

    /**
     * 판매자별 딜 목록 조회
     */
    fun findBySellerIdOrderByCreatedAtDesc(sellerId: Long): List<Deal>

    /**
     * 만료된 활성 딜 조회 (배치 처리용)
     */
    @Query("""
        SELECT d FROM Deal d
        WHERE d.status = 'ACTIVE'
        AND d.endTime < :now
    """)
    fun findExpiredActiveDeals(@Param("now") now: LocalDateTime): List<Deal>
}
