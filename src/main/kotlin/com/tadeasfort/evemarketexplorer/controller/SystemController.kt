package com.tadeasfort.evemarketexplorer.controller

import com.tadeasfort.evemarketexplorer.client.EveEsiWebClient
import com.tadeasfort.evemarketexplorer.repository.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/system")
@Tag(name = "System", description = "System health and monitoring")
class SystemController(
    private val eveEsiWebClient: EveEsiWebClient,
    private val regionRepository: RegionRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
    private val itemGroupRepository: ItemGroupRepository,
    private val itemTypeRepository: ItemTypeRepository,
    private val marketOrderRepository: MarketOrderRepository
) {
    
    @GetMapping("/health")
    @Operation(
        summary = "System health check",
        description = "Returns the current health status of the system"
    )
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "timestamp" to System.currentTimeMillis(),
            "esi" to mapOf(
                "rateLimited" to eveEsiWebClient.isRateLimited(),
                "remainingErrorBudget" to eveEsiWebClient.getRemainingErrorBudget(),
                "availableConnections" to eveEsiWebClient.getAvailableConnections()
            )
        ))
    }
    
    @GetMapping("/status")
    @Operation(
        summary = "Database status check",
        description = "Returns the current status of data population in the database"
    )
    fun status(): ResponseEntity<Map<String, Any>> {
        val regionCount = regionRepository.count()
        val itemCategoryCount = itemCategoryRepository.count()
        val itemGroupCount = itemGroupRepository.count()
        val itemTypeCount = itemTypeRepository.count()
        val marketOrderCount = marketOrderRepository.count()
        
        val isInitialized = regionCount > 0 && itemTypeCount > 0
        val hasMarketData = marketOrderCount > 0
        
        return ResponseEntity.ok(mapOf(
            "initialized" to isInitialized,
            "hasMarketData" to hasMarketData,
            "timestamp" to System.currentTimeMillis(),
            "data" to mapOf(
                "regions" to regionCount,
                "itemCategories" to itemCategoryCount,
                "itemGroups" to itemGroupCount,
                "itemTypes" to itemTypeCount,
                "marketOrders" to marketOrderCount
            ),
            "status" to when {
                !isInitialized -> "INITIALIZING"
                isInitialized && !hasMarketData -> "UNIVERSE_DATA_READY"
                isInitialized && hasMarketData -> "FULLY_OPERATIONAL"
                else -> "UNKNOWN"
            }
        ))
    }
}