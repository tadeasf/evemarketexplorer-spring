package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.SolarSystem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface SolarSystemRepository : JpaRepository<SolarSystem, Int> {
    
    fun findByName(name: String): SolarSystem?
    
    fun findByConstellationConstellationId(constellationId: Int): List<SolarSystem>
    
    @Query("SELECT s FROM SolarSystem s WHERE s.securityStatus >= :minSecurity")
    fun findBySecurityStatusGreaterThanEqual(minSecurity: BigDecimal): List<SolarSystem>
    
    @Query("SELECT s FROM SolarSystem s JOIN FETCH s.constellation c JOIN FETCH c.region r WHERE r.regionId = :regionId")
    fun findByRegionId(regionId: Int): List<SolarSystem>
}