package com.tadeasfort.evemarketexplorer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "item_groups")
data class ItemGroup(
    @Id
    @Column(name = "group_id")
    val groupId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ItemCategory,
    
    @Column(name = "published", nullable = false)
    val published: Boolean = true,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "group", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val types: List<ItemType> = emptyList()
)