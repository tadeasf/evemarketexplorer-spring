package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.ItemGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemGroupRepository : JpaRepository<ItemGroup, Int> {
    
    fun findByName(name: String): ItemGroup?
    
    fun findByCategoryCategoryId(categoryId: Int): List<ItemGroup>
    
    @Query("SELECT g FROM ItemGroup g WHERE g.published = true ORDER BY g.name")
    fun findAllPublished(): List<ItemGroup>
    
    @Query("SELECT g FROM ItemGroup g JOIN FETCH g.category WHERE g.published = true ORDER BY g.name")
    fun findAllPublishedWithCategory(): List<ItemGroup>
}