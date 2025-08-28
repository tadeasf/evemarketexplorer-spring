package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.Region
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RegionRepository : JpaRepository<Region, Int> {
    
    fun findByName(name: String): Region?
    
    @Query("SELECT r FROM Region r ORDER BY r.name")
    fun findAllOrderByName(): List<Region>
    
    @Query("SELECT r.regionId FROM Region r")
    fun findAllRegionIds(): List<Int>
}