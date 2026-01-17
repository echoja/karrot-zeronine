package com.karrot.zeronine.api.controller

import com.karrot.zeronine.api.dto.*
import com.karrot.zeronine.api.service.DealService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Deal", description = "타임딜 API")
@RestController
@RequestMapping("/api/v1/deals")
class DealController(
    private val dealService: DealService
) {

    @Operation(summary = "딜 생성", description = "새로운 타임딜을 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createDeal(
        @RequestBody request: CreateDealRequest,
        @RequestHeader("X-User-Id") sellerId: Long
    ): ApiResponse<DealResponse> {
        val deal = dealService.createDeal(request, sellerId)
        return ApiResponse.success(deal)
    }

    @Operation(summary = "딜 활성화", description = "딜을 활성화합니다")
    @PostMapping("/{dealUuid}/activate")
    fun activateDeal(@PathVariable dealUuid: String): ApiResponse<DealResponse> {
        val deal = dealService.activateDeal(dealUuid)
        return ApiResponse.success(deal)
    }

    @Operation(summary = "딜 상세 조회", description = "딜 상세 정보를 조회합니다")
    @GetMapping("/{dealUuid}")
    fun getDeal(@PathVariable dealUuid: String): ApiResponse<DealResponse> {
        val deal = dealService.getDeal(dealUuid)
        return ApiResponse.success(deal)
    }

    @Operation(summary = "지역별 활성 딜 목록", description = "특정 지역의 활성화된 딜 목록을 조회합니다")
    @GetMapping("/region/{regionCode}")
    fun getActiveDealsInRegion(
        @PathVariable regionCode: String
    ): ApiResponse<List<DealSummaryResponse>> {
        val deals = dealService.getActiveDealsInRegion(regionCode)
        return ApiResponse.success(deals)
    }

    @Operation(summary = "내 딜 목록", description = "판매자 본인의 딜 목록을 조회합니다")
    @GetMapping("/my")
    fun getMyDeals(
        @RequestHeader("X-User-Id") sellerId: Long
    ): ApiResponse<List<DealSummaryResponse>> {
        val deals = dealService.getDealsBySeller(sellerId)
        return ApiResponse.success(deals)
    }
}
