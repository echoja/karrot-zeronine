package com.karrot.zeronine.core.domain.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface OrderRepository : JpaRepository<Order, Long> {

    fun findByUuid(uuid: String): Order?

    fun findByOrderNumber(orderNumber: String): Order?

    fun findByBuyerIdOrderByCreatedAtDesc(buyerId: Long): List<Order>

    fun findByDealIdOrderByCreatedAtDesc(dealId: Long): List<Order>

    /**
     * 특정 딜의 총 주문 수량
     */
    @Query("""
        SELECT COALESCE(SUM(o.quantity), 0)
        FROM Order o
        WHERE o.dealId = :dealId
        AND o.status NOT IN ('CANCELLED', 'REFUNDED')
    """)
    fun sumQuantityByDealId(@Param("dealId") dealId: Long): Int

    /**
     * 사용자의 특정 딜 주문 여부 확인
     */
    fun existsByDealIdAndBuyerIdAndStatusNot(
        dealId: Long,
        buyerId: Long,
        status: OrderStatus
    ): Boolean

    /**
     * 결제 대기 중인 만료된 주문 조회 (자동 취소용)
     */
    @Query("""
        SELECT o FROM Order o
        WHERE o.status = 'PENDING'
        AND o.createdAt < :expireTime
    """)
    fun findExpiredPendingOrders(@Param("expireTime") expireTime: LocalDateTime): List<Order>

    /**
     * 특정 기간 내 완료된 주문 통계
     */
    @Query("""
        SELECT COUNT(o), COALESCE(SUM(o.totalAmount), 0)
        FROM Order o
        WHERE o.status = 'COMPLETED'
        AND o.paidAt BETWEEN :start AND :end
    """)
    fun getCompletedOrderStats(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Array<Any>
}
