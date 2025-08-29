package com.tadeasfort.evemarketexplorer.repository

import com.tadeasfort.evemarketexplorer.model.ItemType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemTypeRepository : JpaRepository<ItemType, Int> {
    
    fun findByName(name: String): ItemType?
    
    fun findByGroupGroupId(groupId: Int): List<ItemType>
    
    @Query("SELECT t FROM ItemType t WHERE t.published = true AND t.marketGroupId IS NOT NULL ORDER BY t.name")
    fun findAllMarketable(): List<ItemType>
    
    @Query("SELECT t FROM ItemType t JOIN FETCH t.group g JOIN FETCH g.category WHERE t.published = true")
    fun findAllPublishedWithGroupAndCategory(): List<ItemType>
    
    @Query("SELECT t.typeId FROM ItemType t WHERE t.published = true AND t.marketGroupId IS NOT NULL")
    fun findAllMarketableTypeIds(): List<Int>
    
    @Query("SELECT t FROM ItemType t WHERE t.published = true AND t.marketGroupId IS NOT NULL AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.name")
    fun searchByName(query: String, pageable: Pageable): List<ItemType>
    
    @Query("SELECT t FROM ItemType t JOIN FETCH t.group g JOIN FETCH g.category c WHERE t.published = true AND t.marketGroupId IS NOT NULL AND c.categoryId = :categoryId ORDER BY t.name")
    fun findByCategory(categoryId: Int): List<ItemType>
}