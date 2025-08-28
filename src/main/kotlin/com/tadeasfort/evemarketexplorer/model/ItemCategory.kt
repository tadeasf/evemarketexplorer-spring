package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "item_categories")
data class ItemCategory(
    @Id
    @Column(name = "category_id")
    val categoryId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @Column(name = "published", nullable = false)
    val published: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val groups: List<ItemGroup> = emptyList()
)