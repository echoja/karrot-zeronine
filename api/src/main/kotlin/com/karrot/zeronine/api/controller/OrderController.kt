package com.karrot.zeronine.api.controller

import com.karrot.zeronine.api.dto.*
import com.karrot.zeronine.api.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService
) {

    @Operation(summary = "주문 생성", description = "타임딜 주문을 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @RequestBody request: CreateOrderRequest,
        @RequestHeader("X-User-Id") buyerId: Long
    ): ApiResponse<OrderResponse> {
        val order = orderService.createOrder(request, buyerId)
        return ApiResponse.success(order)
    }

    @Operation(summary = "결제 완료", description = "주문 결제를 완료 처리합니다")
    @PostMapping("/{orderUuid}/pay")
    fun payOrder(@PathVariable orderUuid: String): ApiResponse<OrderResponse> {
        val order = orderService.markOrderAsPaid(orderUuid)
        return ApiResponse.success(order)
    }

    @Operation(summary = "주문 취소", description = "주문을 취소합니다")
    @PostMapping("/{orderUuid}/cancel")
    fun cancelOrder(
        @PathVariable orderUuid: String,
        @RequestBody request: CancelOrderRequest
    ): ApiResponse<OrderResponse> {
        val order = orderService.cancelOrder(orderUuid, request.reason)
        return ApiResponse.success(order)
    }

    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 조회합니다")
    @GetMapping("/{orderUuid}")
    fun getOrder(@PathVariable orderUuid: String): ApiResponse<OrderResponse> {
        val order = orderService.getOrder(orderUuid)
        return ApiResponse.success(order)
    }

    @Operation(summary = "내 주문 목록", description = "본인의 주문 목록을 조회합니다")
    @GetMapping("/my")
    fun getMyOrders(
        @RequestHeader("X-User-Id") buyerId: Long
    ): ApiResponse<List<OrderResponse>> {
        val orders = orderService.getOrdersByBuyer(buyerId)
        return ApiResponse.success(orders)
    }
}
