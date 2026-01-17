package com.karrot.zeronine.api.controller

import com.karrot.zeronine.api.dto.ApiResponse
import com.karrot.zeronine.api.dto.DealResponse
import com.karrot.zeronine.api.dto.DealSummaryResponse
import com.karrot.zeronine.api.service.DealAsyncService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Deal Async", description = "비동기 딜 API (Kotlin Coroutines)")
@RestController
@RequestMapping("/api/v1/deals/async")
class DealAsyncController(
    private val dealAsyncService: DealAsyncService
) {

    @Operation(summary = "다중 지역 활성 딜 조회", description = "여러 지역의 활성 딜을 병렬로 조회합니다")
    @GetMapping("/regions")
    suspend fun getActiveDealsInMultipleRegions(
        @RequestParam regionCodes: List<String>
    ): ApiResponse<Map<String, List<DealSummaryResponse>>> {
        val deals = dealAsyncService.getActiveDealsInMultipleRegions(regionCodes)
        return ApiResponse.success(deals)
    }

    @Operation(summary = "딜 실시간 재고 조회", description = "딜 정보와 Redis 실시간 재고를 병렬로 조회합니다")
    @GetMapping("/{dealUuid}/realtime")
    suspend fun getDealWithRealTimeStock(
        @PathVariable dealUuid: String
    ): ApiResponse<DealResponse> {
        val deal = dealAsyncService.getDealWithRealTimeStock(dealUuid)
        return ApiResponse.success(deal)
    }

    @Operation(summary = "다중 딜 실시간 재고 조회", description = "여러 딜의 실시간 재고를 병렬로 조회합니다")
    @GetMapping("/stocks")
    suspend fun getRealTimeStocks(
        @RequestParam dealIds: List<Long>
    ): ApiResponse<Map<Long, Int>> {
        val stocks = dealAsyncService.getRealTimeStocks(dealIds)
        return ApiResponse.success(stocks)
    }
}
