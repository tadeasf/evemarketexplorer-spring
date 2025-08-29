package com.tadeasfort.evemarketexplorer.dto

import com.tadeasfort.evemarketexplorer.model.DataState
import com.tadeasfort.evemarketexplorer.model.ItemType
import com.tadeasfort.evemarketexplorer.model.Region
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MarketOrderDto(
    val orderId: Long,
    val region: Region,
    val itemType: ItemType,
    val locationId: Long,
    val locationName: String, // Resolved location name
    val systemId: Int?,
    val systemName: String?, // Resolved system name for additional context
    val isBuyOrder: Boolean,
    val price: BigDecimal,
    val volumeTotal: Int,
    val volumeRemain: Int,
    val minVolume: Int,
    val duration: Int,
    val issued: LocalDate,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dataState: DataState
)