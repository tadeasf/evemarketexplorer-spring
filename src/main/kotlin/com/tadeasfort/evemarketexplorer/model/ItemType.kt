package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "item_types")
data class ItemType(
    @Id
    @Column(name = "type_id")
    val typeId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: ItemGroup,
    
    @Column(name = "published", nullable = false)
    val published: Boolean = true,
    
    @Column(name = "mass", precision = 15, scale = 2)
    val mass: BigDecimal? = null,
    
    @Column(name = "volume", precision = 15, scale = 2)
    val volume: BigDecimal? = null,
    
    @Column(name = "capacity", precision = 15, scale = 2)
    val capacity: BigDecimal? = null,
    
    @Column(name = "portion_size")
    val portionSize: Int? = null,
    
    @Column(name = "base_price", precision = 15, scale = 2)
    val basePrice: BigDecimal? = null,
    
    @Column(name = "market_group_id")
    val marketGroupId: Int? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "itemType", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val marketOrders: List<MarketOrder> = emptyList()
)