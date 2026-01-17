package com.karrot.zeronine.api.dto

import com.karrot.zeronine.core.domain.deal.Deal
import com.karrot.zeronine.core.domain.deal.DealStatus
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 딜 생성 요청 DTO
 */
data class CreateDealRequest(
    val title: String,
    val description: String,
    val originalPrice: BigDecimal,
    val dealPrice: BigDecimal,
    val totalStock: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val regionCode: String
) {
    init {
        require(title.isNotBlank()) { "제목은 필수입니다" }
        require(originalPrice > BigDecimal.ZERO) { "원가는 0보다 커야 합니다" }
        require(dealPrice > BigDecimal.ZERO) { "할인가는 0보다 커야 합니다" }
        require(dealPrice < originalPrice) { "할인가는 원가보다 작아야 합니다" }
        require(totalStock > 0) { "재고는 0보다 커야 합니다" }
        require(endTime.isAfter(startTime)) { "종료 시간은 시작 시간 이후여야 합니다" }
    }
}

/**
 * 딜 응답 DTO - Extension Function 활용
 */
data class DealResponse(
    val uuid: String,
    val title: String,
    val description: String,
    val originalPrice: BigDecimal,
    val dealPrice: BigDecimal,
    val discountRate: Int,
    val totalStock: Int,
    val remainingStock: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val regionCode: String,
    val status: DealStatus,
    val isActive: Boolean,
    val createdAt: LocalDateTime
)

/**
 * Deal -> DealResponse 변환 확장 함수
 * Kotlin Idiomatic: Extension Function으로 도메인 순수성 유지
 */
fun Deal.toResponse(): DealResponse = DealResponse(
    uuid = uuid,
    title = title,
    description = description,
    originalPrice = originalPrice,
    dealPrice = dealPrice,
    discountRate = discountRate,
    totalStock = totalStock,
    remainingStock = remainingStock,
    startTime = startTime,
    endTime = endTime,
    regionCode = regionCode,
    status = status,
    isActive = isActive(),
    createdAt = createdAt
)

/**
 * 딜 목록 응답 DTO (간략 버전)
 */
data class DealSummaryResponse(
    val uuid: String,
    val title: String,
    val dealPrice: BigDecimal,
    val discountRate: Int,
    val remainingStock: Int,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: DealStatus
)

fun Deal.toSummary(): DealSummaryResponse = DealSummaryResponse(
    uuid = uuid,
    title = title,
    dealPrice = dealPrice,
    discountRate = discountRate,
    remainingStock = remainingStock,
    startTime = startTime,
    endTime = endTime,
    status = status
)
