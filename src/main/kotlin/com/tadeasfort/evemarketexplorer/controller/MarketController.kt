package com.tadeasfort.evemarketexplorer.controller

import com.tadeasfort.evemarketexplorer.model.MarketOrder
import com.tadeasfort.evemarketexplorer.repository.MarketOrderRepository
import com.tadeasfort.evemarketexplorer.repository.RegionRepository
import com.tadeasfort.evemarketexplorer.service.EveMarketService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "EVE Online market data operations")
class MarketController(
    private val marketOrderRepository: MarketOrderRepository,
    private val regionRepository: RegionRepository,
    private val eveMarketService: EveMarketService
) {
    
    @GetMapping("/orders")
    @Operation(
        summary = "Get all market orders",
        description = "Retrieves all current market orders across all regions. WARNING: This can return a very large dataset."
    )
    fun getAllMarketOrders(): ResponseEntity<List<MarketOrder>> {
        val orders = marketOrderRepository.findAllLatest()
        return ResponseEntity.ok(orders)
    }
    
    @GetMapping("/orders/item/{typeId}")
    @Operation(
        summary = "Get market orders for a specific item",
        description = "Retrieves all current market orders for a specific item type across all regions"
    )
    fun getMarketOrdersForItem(
        @Parameter(description = "Item type ID", example = "34")
        @PathVariable typeId: Int
    ): ResponseEntity<List<MarketOrder>> {
        val orders = marketOrderRepository.findByItemTypeTypeId(typeId)
        return ResponseEntity.ok(orders)
    }
    
    @GetMapping("/regions/{regionId}/orders")
    @Operation(
        summary = "Get market orders for a region",
        description = "Retrieves all current market orders for a specific region"
    )
    fun getMarketOrdersForRegion(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int,
        @Parameter(description = "Item type ID (optional)", example = "34")
        @RequestParam(required = false) typeId: Int?,
        @Parameter(description = "Order type filter: 'buy', 'sell', or 'all' (default)", example = "sell")
        @RequestParam(required = false, defaultValue = "all") orderType: String
    ): ResponseEntity<List<MarketOrder>> {
        val orders = when {
            typeId != null -> marketOrderRepository.findByRegionRegionIdAndItemTypeTypeId(regionId, typeId)
            orderType == "sell" -> marketOrderRepository.findSellOrdersByRegionOrderByPriceAsc(regionId)
            orderType == "buy" -> marketOrderRepository.findBuyOrdersByRegionOrderByPriceDesc(regionId)
            else -> marketOrderRepository.findSellOrdersByRegionOrderByPriceAsc(regionId) +
                   marketOrderRepository.findBuyOrdersByRegionOrderByPriceDesc(regionId)
        }
        return ResponseEntity.ok(orders)
    }
    
    @GetMapping("/regions/{regionId}/orders/item/{typeId}")
    @Operation(
        summary = "Get market orders for a specific item in a region",
        description = "Retrieves all current market orders for a specific item type in a specific region"
    )
    fun getMarketOrdersForRegionAndItem(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int,
        @Parameter(description = "Item type ID", example = "34")
        @PathVariable typeId: Int,
        @Parameter(description = "Order type filter: 'buy', 'sell', or 'all' (default)", example = "sell")
        @RequestParam(required = false, defaultValue = "all") orderType: String
    ): ResponseEntity<List<MarketOrder>> {
        val orders = when (orderType) {
            "sell" -> marketOrderRepository.findByRegionAndTypeAndOrderType(regionId, typeId, false)
            "buy" -> marketOrderRepository.findByRegionAndTypeAndOrderType(regionId, typeId, true)
            else -> marketOrderRepository.findByRegionRegionIdAndItemTypeTypeId(regionId, typeId)
        }
        return ResponseEntity.ok(orders)
    }
    
    @GetMapping("/regions/{regionId}/summary")
    @Operation(
        summary = "Get market summary for a region",
        description = "Retrieves a summary of market activity for a specific region"
    )
    fun getMarketSummaryForRegion(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int
    ): ResponseEntity<Map<String, Any>> {
        val summary = eveMarketService.getMarketSummaryForRegion(regionId)
        return ResponseEntity.ok(summary)
    }
    
    @PostMapping("/regions/{regionId}/refresh")
    @Operation(
        summary = "Refresh market data for a region",
        description = "Manually triggers a refresh of market data for a specific region"
    )
    fun refreshMarketDataForRegion(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int
    ): ResponseEntity<Map<String, String>> {
        return try {
            eveMarketService.refreshMarketOrdersForRegion(regionId).block()
            ResponseEntity.ok(mapOf("status" to "success", "message" to "Market data refresh initiated for region $regionId"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
        }
    }
}