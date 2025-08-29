package com.tadeasfort.evemarketexplorer.service

import com.tadeasfort.evemarketexplorer.client.EveEsiWebClient
import com.tadeasfort.evemarketexplorer.dto.*
import com.tadeasfort.evemarketexplorer.model.*
import com.tadeasfort.evemarketexplorer.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
@Transactional
class EveUniverseService(
    private val eveEsiWebClient: EveEsiWebClient,
    private val regionRepository: RegionRepository,
    private val constellationRepository: ConstellationRepository,
    private val solarSystemRepository: SolarSystemRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
    private val itemGroupRepository: ItemGroupRepository,
    private val itemTypeRepository: ItemTypeRepository
) {
    private val logger = LoggerFactory.getLogger(EveUniverseService::class.java)
    
    fun refreshUniverseData(): Mono<Void> {
        logger.info("Starting universe data refresh")
        
        return refreshRegions()
            .then(refreshConstellations())
            .then(refreshSolarSystems())
            .then(refreshItemCategories())
            .then(refreshItemGroups())
            .then(refreshItemTypes())
            .doOnSuccess { logger.info("Universe data refresh completed successfully") }
            .doOnError { error -> logger.error("Universe data refresh failed", error) }
    }
    
    private fun refreshRegions(): Mono<Void> {
        logger.info("Refreshing regions data")
        
        return eveEsiWebClient.getList("/latest/universe/regions/", Int::class.java)
            .flatMapMany { regionIds -> 
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/regions/{id}/", regionIds, EsiRegionResponse::class.java)
            }
            .doOnNext { (regionId, regionData) ->
                try {
                    val region = Region(
                        regionId = regionData.regionId,
                        name = regionData.name,
                        description = regionData.description,
                        updatedAt = LocalDateTime.now()
                    )
                    regionRepository.save(region)
                    logger.debug("Saved region: {} ({})", region.name, region.regionId)
                } catch (e: Exception) {
                    logger.error("Failed to save region {}: {}", regionId, e.message)
                }
            }
            .doOnComplete { logger.info("Regions refresh completed") }
            .then()
    }
    
    private fun refreshConstellations(): Mono<Void> {
        logger.info("Refreshing constellations data")
        
        return eveEsiWebClient.getList("/latest/universe/constellations/", Int::class.java)
            .flatMapMany { constellationIds ->
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/constellations/{id}/", constellationIds, EsiConstellationResponse::class.java)
            }
            .doOnNext { (constellationId, constellationData) ->
                try {
                    val region = regionRepository.findById(constellationData.regionId).orElse(null)
                    if (region != null) {
                        val constellation = Constellation(
                            constellationId = constellationData.constellationId,
                            name = constellationData.name,
                            region = region,
                            updatedAt = LocalDateTime.now()
                        )
                        constellationRepository.save(constellation)
                        logger.debug("Saved constellation: {} ({})", constellation.name, constellation.constellationId)
                    } else {
                        logger.warn("Region {} not found for constellation {}", constellationData.regionId, constellationId)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to save constellation {}: {}", constellationId, e.message)
                }
            }
            .doOnComplete { logger.info("Constellations refresh completed") }
            .then()
    }
    
    private fun refreshSolarSystems(): Mono<Void> {
        logger.info("Refreshing solar systems data")
        
        return eveEsiWebClient.getList("/latest/universe/systems/", Int::class.java)
            .flatMapMany { systemIds ->
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/systems/{id}/", systemIds, EsiSystemResponse::class.java)
            }
            .doOnNext { (systemId, systemData) ->
                try {
                    val constellation = constellationRepository.findById(systemData.constellationId).orElse(null)
                    if (constellation != null) {
                        val solarSystem = SolarSystem(
                            systemId = systemData.systemId,
                            name = systemData.name,
                            constellation = constellation,
                            securityStatus = systemData.securityStatus,
                            starId = systemData.starId,
                            positionX = systemData.position?.x,
                            positionY = systemData.position?.y,
                            positionZ = systemData.position?.z,
                            updatedAt = LocalDateTime.now()
                        )
                        solarSystemRepository.save(solarSystem)
                        logger.debug("Saved solar system: {} ({})", solarSystem.name, solarSystem.systemId)
                    } else {
                        logger.warn("Constellation {} not found for system {}", systemData.constellationId, systemId)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to save solar system {}: {}", systemId, e.message)
                }
            }
            .doOnComplete { logger.info("Solar systems refresh completed") }
            .then()
    }
    
    private fun refreshItemCategories(): Mono<Void> {
        logger.info("Refreshing item categories data")
        
        return eveEsiWebClient.getList("/latest/universe/categories/", Int::class.java)
            .flatMapMany { categoryIds ->
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/categories/{id}/", categoryIds, EsiCategoryResponse::class.java)
            }
            .doOnNext { (categoryId, categoryData) ->
                try {
                    val category = ItemCategory(
                        categoryId = categoryData.categoryId,
                        name = categoryData.name,
                        published = categoryData.published,
                        updatedAt = LocalDateTime.now()
                    )
                    itemCategoryRepository.save(category)
                    logger.debug("Saved item category: {} ({})", category.name, category.categoryId)
                } catch (e: Exception) {
                    logger.error("Failed to save item category {}: {}", categoryId, e.message)
                }
            }
            .doOnComplete { logger.info("Item categories refresh completed") }
            .then()
    }
    
    private fun refreshItemGroups(): Mono<Void> {
        logger.info("Refreshing item groups data")
        
        return eveEsiWebClient.getListPaginated("/latest/universe/groups/", Int::class.java)
            .doOnNext { groupIds -> 
                logger.info("Found {} total item groups to process across all pages", groupIds.size) 
            }
            .flatMapMany { groupIds ->
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/groups/{id}/", groupIds, EsiGroupResponse::class.java)
            }
            .collectList()
            .doOnNext { groupDataList ->
                logger.info("Received {} item group responses from API", groupDataList.size)
                
                val savedGroups = mutableListOf<ItemGroup>()
                var missingCategoryCount = 0
                var errorCount = 0
                
                for ((groupId, groupData) in groupDataList) {
                    try {
                        val category = itemCategoryRepository.findById(groupData.categoryId).orElse(null)
                        if (category != null) {
                            val group = ItemGroup(
                                groupId = groupData.groupId,
                                name = groupData.name,
                                category = category,
                                published = groupData.published,
                                updatedAt = LocalDateTime.now()
                            )
                            savedGroups.add(group)
                            
                            // Batch save every 100 groups
                            if (savedGroups.size >= 100) {
                                itemGroupRepository.saveAll(savedGroups)
                                logger.debug("Saved batch of {} item groups", savedGroups.size)
                                savedGroups.clear()
                            }
                        } else {
                            missingCategoryCount++
                            logger.warn("Category {} not found for group {}", groupData.categoryId, groupId)
                        }
                    } catch (e: Exception) {
                        errorCount++
                        logger.error("Failed to process item group {}: {}", groupId, e.message)
                    }
                }
                
                // Save remaining groups
                if (savedGroups.isNotEmpty()) {
                    itemGroupRepository.saveAll(savedGroups)
                    logger.debug("Saved final batch of {} item groups", savedGroups.size)
                }
                
                val finalCount = itemGroupRepository.count()
                logger.info("Item groups processing summary:")
                logger.info("  - API responses received: {}", groupDataList.size)
                logger.info("  - Missing categories: {}", missingCategoryCount)
                logger.info("  - Processing errors: {}", errorCount)
                logger.info("  - Total item groups in database: {}", finalCount)
            }
            .then()
            .doOnSuccess { 
                logger.info("Item groups refresh completed") 
            }
    }
    
    private fun refreshItemTypes(): Mono<Void> {
        logger.info("Refreshing item types data")
        
        return eveEsiWebClient.getListPaginated("/latest/universe/types/", Int::class.java)
            .doOnNext { typeIds -> 
                logger.info("Found {} total item types to process across all pages", typeIds.size) 
            }
            .flatMapMany { typeIds ->
                eveEsiWebClient.getConcurrentlyFromIds("/latest/universe/types/{id}/", typeIds, EsiTypeResponse::class.java, maxConcurrency = 20)
            }
            .collectList()
            .doOnNext { typeDataList ->
                logger.info("Received {} item type responses from API", typeDataList.size)
                
                val savedTypes = mutableListOf<ItemType>()
                var missingGroupCount = 0
                var errorCount = 0
                
                for ((typeId, typeData) in typeDataList) {
                    try {
                        val group = itemGroupRepository.findById(typeData.groupId).orElse(null)
                        if (group != null) {
                            val itemType = ItemType(
                                typeId = typeData.typeId,
                                name = typeData.name,
                                description = typeData.description,
                                group = group,
                                published = typeData.published,
                                mass = typeData.mass,
                                volume = typeData.volume,
                                capacity = typeData.capacity,
                                portionSize = typeData.portionSize,
                                basePrice = typeData.basePrice,
                                marketGroupId = typeData.marketGroupId,
                                updatedAt = LocalDateTime.now()
                            )
                            savedTypes.add(itemType)
                            
                            // Batch save every 100 types
                            if (savedTypes.size >= 100) {
                                itemTypeRepository.saveAll(savedTypes)
                                logger.debug("Saved batch of {} item types", savedTypes.size)
                                savedTypes.clear()
                            }
                        } else {
                            missingGroupCount++
                            logger.warn("Group {} not found for type {}", typeData.groupId, typeId)
                        }
                    } catch (e: Exception) {
                        errorCount++
                        logger.error("Failed to process item type {}: {}", typeId, e.message)
                    }
                }
                
                // Save remaining types
                if (savedTypes.isNotEmpty()) {
                    itemTypeRepository.saveAll(savedTypes)
                    logger.debug("Saved final batch of {} item types", savedTypes.size)
                }
                
                val finalCount = itemTypeRepository.count()
                logger.info("Item types processing summary:")
                logger.info("  - API responses received: {}", typeDataList.size)
                logger.info("  - Missing groups: {}", missingGroupCount)
                logger.info("  - Processing errors: {}", errorCount)
                logger.info("  - Total item types in database: {}", finalCount)
            }
            .then()
            .doOnSuccess { 
                logger.info("Item types refresh completed") 
            }
    }
    
    /**
     * Resolves a location name based on location ID
     * In EVE Online:
     * - System IDs: 30000000+ range -> Solar System names
     * - Station IDs: 60000000+ range -> Station names (fallback to system)
     * - Structure IDs: Various ranges -> Structure names (fallback to system)
     */
    fun resolveLocationName(locationId: Long, systemId: Int?): String {
        // If it's a system ID (30000000+ range), try to find the system directly
        if (locationId >= 30000000L && locationId < 40000000L) {
            val system = solarSystemRepository.findById(locationId.toInt()).orElse(null)
            return system?.name ?: "Unknown System ($locationId)"
        }
        
        // For stations/structures, try to resolve using the provided systemId
        if (systemId != null) {
            val system = solarSystemRepository.findById(systemId).orElse(null)
            return system?.name ?: "Unknown System ($systemId)"
        }
        
        // Fallback for unknown location types
        return "Unknown Location ($locationId)"
    }
}