package com.tadeasfort.evemarketexplorer.controller

import com.tadeasfort.evemarketexplorer.dto.MarketOrderDto
import com.tadeasfort.evemarketexplorer.model.MarketOrder
import com.tadeasfort.evemarketexplorer.repository.MarketOrderRepository
import com.tadeasfort.evemarketexplorer.repository.RegionRepository
import com.tadeasfort.evemarketexplorer.service.EveMarketService
import com.tadeasfort.evemarketexplorer.service.EveUniverseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://194.5.152.243:3000", "http://194.5.152.243:3001"], allowCredentials = "true")
@Tag(name = "Market", description = "EVE Online market data operations")
class MarketController(
    private val marketOrderRepository: MarketOrderRepository,
    private val regionRepository: RegionRepository,
    private val eveMarketService: EveMarketService,
    private val eveUniverseService: EveUniverseService
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
        description = "Retrieves all current market orders for a specific item type across all regions. Buy orders sorted by highest price, sell orders by lowest price."
    )
    fun getMarketOrdersForItem(
        @Parameter(description = "Item type ID", example = "34")
        @PathVariable typeId: Int,
        @Parameter(description = "Order type filter: 'buy', 'sell', or 'all' (default)", example = "sell")
        @RequestParam(required = false, defaultValue = "all") orderType: String,
        @Parameter(description = "Maximum number of orders to return per type", example = "200")
        @RequestParam(required = false, defaultValue = "200") limit: Int
    ): ResponseEntity<Map<String, List<MarketOrderDto>>> {
        val result = mutableMapOf<String, List<MarketOrderDto>>()
        
        val pageable = PageRequest.of(0, limit)
        
        when (orderType.lowercase()) {
            "sell" -> {
                val sellOrders = marketOrderRepository.findSellOrdersByType(typeId, pageable)
                result["sell_orders"] = sellOrders.map { convertToDto(it) }
            }
            "buy" -> {
                val buyOrders = marketOrderRepository.findBuyOrdersByType(typeId, pageable)
                result["buy_orders"] = buyOrders.map { convertToDto(it) }
            }
            else -> {
                val sellOrders = marketOrderRepository.findSellOrdersByType(typeId, pageable)
                val buyOrders = marketOrderRepository.findBuyOrdersByType(typeId, pageable)
                result["sell_orders"] = sellOrders.map { convertToDto(it) }
                result["buy_orders"] = buyOrders.map { convertToDto(it) }
            }
        }
        
        return ResponseEntity.ok(result)
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
        description = "Retrieves all current market orders for a specific item type in a specific region. Buy orders are sorted by highest price first, sell orders by lowest price first."
    )
    fun getMarketOrdersForRegionAndItem(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int,
        @Parameter(description = "Item type ID", example = "34")
        @PathVariable typeId: Int,
        @Parameter(description = "Order type filter: 'buy', 'sell', or 'all' (default)", example = "sell")
        @RequestParam(required = false, defaultValue = "all") orderType: String,
        @Parameter(description = "Maximum number of orders to return per type", example = "100")
        @RequestParam(required = false, defaultValue = "100") limit: Int
    ): ResponseEntity<Map<String, List<MarketOrder>>> {
        val result = mutableMapOf<String, List<MarketOrder>>()
        
        val pageable = PageRequest.of(0, limit)
        
        when (orderType.lowercase()) {
            "sell" -> {
                val sellOrders = marketOrderRepository.findSellOrdersByRegionAndType(regionId, typeId, pageable)
                result["sell_orders"] = sellOrders
            }
            "buy" -> {
                val buyOrders = marketOrderRepository.findBuyOrdersByRegionAndType(regionId, typeId, pageable)
                result["buy_orders"] = buyOrders
            }
            else -> {
                val sellOrders = marketOrderRepository.findSellOrdersByRegionAndType(regionId, typeId, pageable)
                val buyOrders = marketOrderRepository.findBuyOrdersByRegionAndType(regionId, typeId, pageable)
                result["sell_orders"] = sellOrders
                result["buy_orders"] = buyOrders
            }
        }
        
        return ResponseEntity.ok(result)
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
    
    private fun convertToDto(marketOrder: MarketOrder): MarketOrderDto {
        val locationName = eveUniverseService.resolveLocationName(marketOrder.locationId, marketOrder.systemId)
        val systemName = marketOrder.systemId?.let { systemId ->
            eveUniverseService.resolveLocationName(systemId.toLong(), systemId).takeIf { !it.startsWith("Unknown") }
        }
        
        return MarketOrderDto(
            orderId = marketOrder.orderId,
            region = marketOrder.region,
            itemType = marketOrder.itemType,
            locationId = marketOrder.locationId,
            locationName = locationName,
            systemId = marketOrder.systemId,
            systemName = systemName,
            isBuyOrder = marketOrder.isBuyOrder,
            price = marketOrder.price,
            volumeTotal = marketOrder.volumeTotal,
            volumeRemain = marketOrder.volumeRemain,
            minVolume = marketOrder.minVolume,
            duration = marketOrder.duration,
            issued = marketOrder.issued,
            createdAt = marketOrder.createdAt,
            updatedAt = marketOrder.updatedAt,
            dataState = marketOrder.dataState
        )
    }
}