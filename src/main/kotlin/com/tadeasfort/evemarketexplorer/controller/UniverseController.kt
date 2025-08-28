package com.tadeasfort.evemarketexplorer.controller

import com.tadeasfort.evemarketexplorer.model.Region
import com.tadeasfort.evemarketexplorer.model.SolarSystem
import com.tadeasfort.evemarketexplorer.repository.RegionRepository
import com.tadeasfort.evemarketexplorer.repository.SolarSystemRepository
import com.tadeasfort.evemarketexplorer.service.EveUniverseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/universe")
@Tag(name = "Universe", description = "EVE Online universe data operations")
class UniverseController(
    private val regionRepository: RegionRepository,
    private val solarSystemRepository: SolarSystemRepository,
    private val eveUniverseService: EveUniverseService
) {
    
    @GetMapping("/regions")
    @Operation(
        summary = "Get all regions",
        description = "Retrieves a list of all regions in EVE Online"
    )
    fun getAllRegions(): ResponseEntity<List<Region>> {
        val regions = regionRepository.findAllOrderByName()
        return ResponseEntity.ok(regions)
    }
    
    @GetMapping("/regions/{regionId}")
    @Operation(
        summary = "Get region by ID",
        description = "Retrieves detailed information about a specific region"
    )
    fun getRegionById(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int
    ): ResponseEntity<Region> {
        val region = regionRepository.findById(regionId).orElse(null)
        return if (region != null) {
            ResponseEntity.ok(region)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/regions/{regionId}/systems")
    @Operation(
        summary = "Get solar systems in a region",
        description = "Retrieves all solar systems within a specific region"
    )
    fun getSystemsInRegion(
        @Parameter(description = "Region ID", example = "10000002")
        @PathVariable regionId: Int
    ): ResponseEntity<List<SolarSystem>> {
        val systems = solarSystemRepository.findByRegionId(regionId)
        return ResponseEntity.ok(systems)
    }
    
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh universe data",
        description = "Manually triggers a refresh of all universe data (regions, systems, constellations, item types)"
    )
    fun refreshUniverseData(): ResponseEntity<Map<String, String>> {
        return try {
            eveUniverseService.refreshUniverseData().block()
            ResponseEntity.ok(mapOf("status" to "success", "message" to "Universe data refresh initiated"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("status" to "error", "message" to (e.message ?: "Unknown error")))
        }
    }
}