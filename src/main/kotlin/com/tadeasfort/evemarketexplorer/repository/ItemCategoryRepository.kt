package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.ItemCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemCategoryRepository : JpaRepository<ItemCategory, Int> {
    
    fun findByName(name: String): ItemCategory?
    
    @Query("SELECT c FROM ItemCategory c WHERE c.published = true ORDER BY c.name")
    fun findAllPublished(): List<ItemCategory>
}