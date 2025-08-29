package com.tadeasfort.evemarketexplorer.service

import com.tadeasfort.evemarketexplorer.client.EveEsiWebClient
import com.tadeasfort.evemarketexplorer.dto.EsiMarketOrderResponse
import com.tadeasfort.evemarketexplorer.model.DataState
import com.tadeasfort.evemarketexplorer.model.MarketOrder
import com.tadeasfort.evemarketexplorer.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional
class EveMarketService(
    private val eveEsiWebClient: EveEsiWebClient,
    private val regionRepository: RegionRepository,
    private val itemTypeRepository: ItemTypeRepository,
    private val marketOrderRepository: MarketOrderRepository
) {
    private val logger = LoggerFactory.getLogger(EveMarketService::class.java)
    
    
    fun refreshAllMarketData(): Mono<Void> {
        logger.info("Starting complete market data refresh with staging/latest pattern")
        
        return Mono.fromCallable { regionRepository.findAllRegionIds() }
            .flatMapMany { regionIds -> Flux.fromIterable(regionIds) }
            .flatMap({ regionId ->
                refreshMarketOrdersForRegionWithStaging(regionId)
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter { throwable ->
                                throwable.message?.contains("database is locked") == true ||
                                throwable.message?.contains("SQLITE_BUSY") == true
                            }
                            .doBeforeRetry { retrySignal ->
                                logger.warn("Retrying database operation for region {} (attempt {}): {}", 
                                    regionId, retrySignal.totalRetries() + 1, retrySignal.failure().message)
                            }
                    )
                    .onErrorResume { error ->
                        logger.error("Failed to refresh market orders for region {} after retries: {}", regionId, error.message)
                        Mono.empty()
                    }
            }, 3) // Reduced from 10 to 3 regions concurrently to reduce database contention
            .then()
            .then(promoteAllStagingToLatest())
            .doOnSuccess { logger.info("Complete market orders refresh completed successfully") }
            .doOnError { error -> logger.error("Complete market orders refresh failed", error) }
    }
    
    private fun promoteAllStagingToLatest(): Mono<Void> {
        return Mono.fromCallable { regionRepository.findAllRegionIds() }
            .flatMapMany { regionIds -> 
                logger.info("Promoting staging data to latest for {} regions", regionIds.size)
                Flux.fromIterable(regionIds).index()
            }
            .flatMap({ indexedRegionId ->
                val index = indexedRegionId.t1
                val regionId = indexedRegionId.t2
                promoteRegionStagingToLatest(regionId)
                    .doOnSuccess {
                        if ((index + 1) % 10 == 0L || index == 0L) {
                            logger.info("Promoted staging to latest for region {} ({} regions processed)", regionId, index + 1)
                        }
                    }
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10))
                            .filter { throwable ->
                                throwable.message?.contains("database is locked") == true ||
                                throwable.message?.contains("SQLITE_BUSY") == true
                            }
                            .doBeforeRetry { retrySignal ->
                                logger.warn("Retrying promotion for region {} (attempt {}): {}", 
                                    regionId, retrySignal.totalRetries() + 1, retrySignal.failure().message)
                            }
                    )
                    .onErrorResume { error ->
                        logger.error("Failed to promote staging to latest for region {} after retries: {}", regionId, error.message)
                        Mono.empty()
                    }
            }, 5) // Process 5 regions concurrently to balance speed vs database load
            .then()
            .doOnSuccess { logger.info("All regional staging data promoted to latest successfully") }
    }
    
    private fun promoteRegionStagingToLatest(regionId: Int): Mono<Void> {
        return Mono.fromCallable {
            logger.debug("Promoting staging to latest for region {}", regionId)
            
            // Delete LATEST data for this region only
            marketOrderRepository.deleteByRegionIdAndDataState(regionId, DataState.LATEST)
            
            // Promote STAGING to LATEST for this region only
            marketOrderRepository.updateDataStateByRegion(regionId, DataState.STAGING, DataState.LATEST)
            
            logger.debug("Successfully promoted staging to latest for region {}", regionId)
        }.then()
    }
    
    fun refreshMarketOrdersForRegion(regionId: Int): Mono<Void> {
        logger.info("Refreshing market orders for region {} (legacy method)", regionId)
        return refreshMarketOrdersForRegionWithState(regionId, DataState.LATEST)
    }
    
    private fun refreshMarketOrdersForRegionWithStaging(regionId: Int): Mono<Void> {
        logger.info("Refreshing market orders for region {} with staging", regionId)
        return refreshMarketOrdersForRegionWithState(regionId, DataState.STAGING)
    }
    
    private fun refreshMarketOrdersForRegionWithState(regionId: Int, dataState: DataState): Mono<Void> {
        logger.info("Refreshing market orders for region {} with state {}", regionId, dataState)
        
        return eveEsiWebClient.getListPaginated("/latest/markets/$regionId/orders/", EsiMarketOrderResponse::class.java)
            .doOnNext { orders -> 
                logger.info("Received {} market orders for region {} across all pages", orders.size, regionId)
                
                // Clear existing orders for the region with the specified state
                try {
                    if (dataState == DataState.STAGING) {
                        marketOrderRepository.deleteByRegionIdAndDataState(regionId, DataState.STAGING)
                    } else {
                        marketOrderRepository.deleteByRegionId(regionId) // Legacy support
                    }
                } catch (e: Exception) {
                    logger.error("Failed to delete existing orders for region {}: {}", regionId, e.message)
                    throw e
                }
                
                val region = regionRepository.findById(regionId).orElse(null)
                if (region == null) {
                    logger.warn("Region {} not found, skipping market orders", regionId)
                    return@doOnNext
                }
                
                val savedOrders = mutableListOf<MarketOrder>()
                var validOrderCount = 0
                var invalidOrderCount = 0
                
                for (orderData in orders) {
                    try {
                        val itemType = itemTypeRepository.findById(orderData.typeId).orElse(null)
                        if (itemType != null) {
                            val marketOrder = MarketOrder(
                                orderId = orderData.orderId,
                                region = region,
                                itemType = itemType,
                                locationId = orderData.locationId,
                                systemId = orderData.systemId,
                                isBuyOrder = orderData.isBuyOrder,
                                price = orderData.price,
                                volumeTotal = orderData.volumeTotal,
                                volumeRemain = orderData.volumeRemain,
                                minVolume = orderData.minVolume,
                                duration = orderData.duration,
                                issued = orderData.issued,
                                range = orderData.range,
                                updatedAt = LocalDateTime.now(),
                                dataState = dataState
                            )
                            savedOrders.add(marketOrder)
                            validOrderCount++
                            
                            // Batch save every 1000 orders
                            if (savedOrders.size >= 1000) {
                                try {
                                    marketOrderRepository.saveAll(savedOrders)
                                    savedOrders.clear()
                                    logger.debug("Saved batch of market orders for region {} with state {}", regionId, dataState)
                                } catch (e: Exception) {
                                    logger.error("Failed to save batch of market orders for region {}: {}", regionId, e.message)
                                    throw e
                                }
                            }
                        } else {
                            invalidOrderCount++
                            logger.debug("ItemType {} not found for market order {}", orderData.typeId, orderData.orderId)
                        }
                    } catch (e: Exception) {
                        invalidOrderCount++
                        logger.error("Failed to save market order {} for region {}: {}", orderData.orderId, regionId, e.message)
                    }
                }
                
                // Save remaining orders
                if (savedOrders.isNotEmpty()) {
                    try {
                        marketOrderRepository.saveAll(savedOrders)
                    } catch (e: Exception) {
                        logger.error("Failed to save remaining batch of market orders for region {}: {}", regionId, e.message)
                        throw e
                    }
                }
                
                logger.info("Market orders processing summary for region {} with state {}:", regionId, dataState)
                logger.info("  - Total orders from API: {}", orders.size)
                logger.info("  - Valid orders saved: {}", validOrderCount)
                logger.info("  - Invalid/failed orders: {}", invalidOrderCount)
            }
            .then()
            .onErrorResume { error ->
                logger.error("Failed to refresh market orders for region {}: {}", regionId, error.message)
                Mono.empty()
            }
    }
    
    
    fun getMarketSummaryForRegion(regionId: Int): Map<String, Any> {
        val region = regionRepository.findById(regionId).orElse(null) ?: return emptyMap()
        
        val sellOrders = marketOrderRepository.findSellOrdersByRegionOrderByPriceAsc(regionId)
        val buyOrders = marketOrderRepository.findBuyOrdersByRegionOrderByPriceDesc(regionId)
        val totalOrders = sellOrders.size + buyOrders.size
        val uniqueTypes = (sellOrders + buyOrders).map { it.itemType.typeId }.toSet().size
        
        return mapOf(
            "regionId" to regionId,
            "regionName" to region.name,
            "totalOrders" to totalOrders,
            "sellOrders" to sellOrders.size,
            "buyOrders" to buyOrders.size,
            "uniqueTypes" to uniqueTypes,
            "lastUpdate" to LocalDateTime.now()
        )
    }
}