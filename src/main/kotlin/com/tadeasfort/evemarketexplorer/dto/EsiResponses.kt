package com.tadeasfort.evemarketexplorer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDate

data class EsiRegionResponse(
    @JsonProperty("region_id") val regionId: Int,
    val name: String,
    val description: String? = null,
    val constellations: List<Int> = emptyList()
)

data class EsiConstellationResponse(
    @JsonProperty("constellation_id") val constellationId: Int,
    val name: String,
    @JsonProperty("region_id") val regionId: Int,
    val systems: List<Int> = emptyList(),
    val position: EsiPosition? = null
)

data class EsiSystemResponse(
    @JsonProperty("system_id") val systemId: Int,
    val name: String,
    @JsonProperty("constellation_id") val constellationId: Int,
    @JsonProperty("security_status") val securityStatus: BigDecimal? = null,
    @JsonProperty("star_id") val starId: Int? = null,
    val position: EsiPosition? = null,
    val planets: List<EsiPlanet>? = null,
    @JsonProperty("security_class") val securityClass: String? = null
)

data class EsiPosition(
    val x: BigDecimal,
    val y: BigDecimal,
    val z: BigDecimal
)

data class EsiPlanet(
    @JsonProperty("planet_id") val planetId: Int,
    @JsonProperty("type_id") val typeId: Int? = null
)

data class EsiCategoryResponse(
    @JsonProperty("category_id") val categoryId: Int,
    val name: String,
    val published: Boolean = true,
    val groups: List<Int> = emptyList()
)

data class EsiGroupResponse(
    @JsonProperty("group_id") val groupId: Int,
    val name: String,
    @JsonProperty("category_id") val categoryId: Int,
    val published: Boolean = true,
    val types: List<Int> = emptyList()
)

data class EsiTypeResponse(
    @JsonProperty("type_id") val typeId: Int,
    val name: String,
    val description: String? = null,
    @JsonProperty("group_id") val groupId: Int,
    val published: Boolean = true,
    val mass: BigDecimal? = null,
    val volume: BigDecimal? = null,
    val capacity: BigDecimal? = null,
    @JsonProperty("portion_size") val portionSize: Int? = null,
    @JsonProperty("base_price") val basePrice: BigDecimal? = null,
    @JsonProperty("market_group_id") val marketGroupId: Int? = null,
    @JsonProperty("dogma_attributes") val dogmaAttributes: List<EsiDogmaAttribute>? = null,
    @JsonProperty("dogma_effects") val dogmaEffects: List<EsiDogmaEffect>? = null
)

data class EsiDogmaAttribute(
    @JsonProperty("attribute_id") val attributeId: Int,
    val value: BigDecimal
)

data class EsiDogmaEffect(
    @JsonProperty("effect_id") val effectId: Int,
    @JsonProperty("is_default") val isDefault: Boolean = false
)

data class EsiMarketOrderResponse(
    @JsonProperty("order_id") val orderId: Long,
    @JsonProperty("type_id") val typeId: Int,
    @JsonProperty("location_id") val locationId: Long,
    @JsonProperty("system_id") val systemId: Int? = null,
    @JsonProperty("is_buy_order") val isBuyOrder: Boolean,
    val price: BigDecimal,
    @JsonProperty("volume_total") val volumeTotal: Int,
    @JsonProperty("volume_remain") val volumeRemain: Int,
    @JsonProperty("min_volume") val minVolume: Int = 1,
    val duration: Int,
    val issued: LocalDate,
    val range: String? = null
)

