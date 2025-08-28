package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.Constellation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ConstellationRepository : JpaRepository<Constellation, Int> {
    
    fun findByName(name: String): Constellation?
    
    fun findByRegionRegionId(regionId: Int): List<Constellation>
    
    @Query("SELECT c FROM Constellation c JOIN FETCH c.region ORDER BY c.name")
    fun findAllWithRegion(): List<Constellation>
}