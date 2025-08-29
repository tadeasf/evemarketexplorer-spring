package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class DataState {
    LATEST, STAGING
}

@Entity
@Table(
    name = "market_orders",
    indexes = [
        Index(name = "idx_market_orders_region_type", columnList = "region_id,type_id"),
        Index(name = "idx_market_orders_is_buy_order", columnList = "is_buy_order"),
        Index(name = "idx_market_orders_issued", columnList = "issued"),
        Index(name = "idx_market_orders_data_state", columnList = "data_state"),
        Index(name = "idx_market_orders_region_state", columnList = "region_id,data_state"),
        Index(name = "idx_market_orders_region_type_state", columnList = "region_id,type_id,data_state"),
        Index(name = "idx_market_orders_type_buy_price", columnList = "type_id,is_buy_order,price,data_state"),
        Index(name = "idx_market_orders_region_type_buy_price", columnList = "region_id,type_id,is_buy_order,price,data_state")
    ]
)
data class MarketOrder(
    @Id
    @Column(name = "order_id")
    val orderId: Long,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    val region: Region,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    val itemType: ItemType,
    
    @Column(name = "location_id", nullable = false)
    val locationId: Long,
    
    @Column(name = "system_id")
    val systemId: Int? = null,
    
    @Column(name = "is_buy_order", nullable = false)
    val isBuyOrder: Boolean,
    
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    val price: BigDecimal,
    
    @Column(name = "volume_total", nullable = false)
    val volumeTotal: Int,
    
    @Column(name = "volume_remain", nullable = false)
    val volumeRemain: Int,
    
    @Column(name = "min_volume", nullable = false)
    val minVolume: Int = 1,
    
    @Column(name = "duration", nullable = false)
    val duration: Int,
    
    @Column(name = "issued", nullable = false)
    val issued: LocalDate,
    
    @Column(name = "range", length = 50)
    val range: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Enumerated(EnumType.STRING)
    @Column(name = "data_state", nullable = false, length = 20)
    val dataState: DataState = DataState.LATEST
)