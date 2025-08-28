package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.DataState
import com.tadeasfort.evemarketexplorer.model.MarketOrder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Repository
interface MarketOrderRepository : JpaRepository<MarketOrder, Long> {
    
    // Latest data queries (default behavior for API)
    @Query("SELECT o FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.itemType.typeId = :typeId AND o.dataState = 'LATEST'")
    fun findByRegionRegionIdAndItemTypeTypeId(regionId: Int, typeId: Int): List<MarketOrder>
    
    @Query("SELECT o FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.itemType.typeId = :typeId AND o.isBuyOrder = :isBuyOrder AND o.dataState = 'LATEST' ORDER BY o.price")
    fun findByRegionAndTypeAndOrderType(regionId: Int, typeId: Int, isBuyOrder: Boolean): List<MarketOrder>
    
    @Query("SELECT o FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.isBuyOrder = false AND o.dataState = 'LATEST' ORDER BY o.price ASC")
    fun findSellOrdersByRegionOrderByPriceAsc(regionId: Int): List<MarketOrder>
    
    @Query("SELECT o FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.isBuyOrder = true AND o.dataState = 'LATEST' ORDER BY o.price DESC")
    fun findBuyOrdersByRegionOrderByPriceDesc(regionId: Int): List<MarketOrder>
    
    @Query("SELECT o FROM MarketOrder o WHERE o.dataState = 'LATEST'")
    fun findAllLatest(): List<MarketOrder>
    
    @Query("SELECT o FROM MarketOrder o WHERE o.itemType.typeId = :typeId AND o.dataState = 'LATEST'")
    fun findByItemTypeTypeId(typeId: Int): List<MarketOrder>
    
    @Query("SELECT MIN(o.price) FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.itemType.typeId = :typeId AND o.isBuyOrder = false AND o.dataState = 'LATEST'")
    fun findLowestSellPrice(regionId: Int, typeId: Int): BigDecimal?
    
    @Query("SELECT MAX(o.price) FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.itemType.typeId = :typeId AND o.isBuyOrder = true AND o.dataState = 'LATEST'")
    fun findHighestBuyPrice(regionId: Int, typeId: Int): BigDecimal?
    
    // Data management queries for staging/latest pattern
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketOrder o WHERE o.region.regionId = :regionId AND o.dataState = :dataState")
    fun deleteByRegionIdAndDataState(regionId: Int, dataState: DataState)
    
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketOrder o WHERE o.dataState = :dataState")
    fun deleteByDataState(dataState: DataState)
    
    @Modifying
    @Transactional
    @Query("UPDATE MarketOrder o SET o.dataState = :newState WHERE o.dataState = :oldState")
    fun updateDataState(oldState: DataState, newState: DataState)
    
    @Modifying
    @Transactional
    @Query("UPDATE MarketOrder o SET o.dataState = :newState WHERE o.region.regionId = :regionId AND o.dataState = :oldState")
    fun updateDataStateByRegion(regionId: Int, oldState: DataState, newState: DataState)
    
    // Legacy method for backwards compatibility - will be removed later
    @Modifying
    @Transactional
    @Query("DELETE FROM MarketOrder o WHERE o.region.regionId = :regionId")
    fun deleteByRegionId(regionId: Int)
}