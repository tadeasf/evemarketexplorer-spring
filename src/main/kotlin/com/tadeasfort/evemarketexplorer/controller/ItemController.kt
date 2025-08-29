package com.tadeasfort.evemarketexplorer.controller

import com.tadeasfort.evemarketexplorer.model.ItemType
import com.tadeasfort.evemarketexplorer.repository.ItemTypeRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:3001", "http://194.5.152.243:3000", "http://194.5.152.243:3001"], allowCredentials = "true")
@Tag(name = "Items", description = "EVE Online item type operations")
class ItemController(
    private val itemTypeRepository: ItemTypeRepository
) {
    
    @GetMapping("/types")
    @Operation(
        summary = "Get all marketable item types",
        description = "Retrieves all published item types that have market groups (can be traded)"
    )
    fun getAllMarketableItemTypes(): ResponseEntity<List<ItemType>> {
        val itemTypes = itemTypeRepository.findAllMarketable()
        return ResponseEntity.ok(itemTypes)
    }
    
    @GetMapping("/types/search")
    @Operation(
        summary = "Search item types by name",
        description = "Search for item types by name (case-insensitive partial match)"
    )
    fun searchItemTypes(
        @Parameter(description = "Search query for item name", example = "Tritanium")
        @RequestParam query: String,
        @Parameter(description = "Maximum number of results to return", example = "50")
        @RequestParam(defaultValue = "50") limit: Int
    ): ResponseEntity<List<ItemType>> {
        val pageable = PageRequest.of(0, limit)
        val itemTypes = itemTypeRepository.searchByName(query, pageable)
        return ResponseEntity.ok(itemTypes)
    }
    
    @GetMapping("/types/{typeId}")
    @Operation(
        summary = "Get item type by ID",
        description = "Retrieves detailed information about a specific item type"
    )
    fun getItemTypeById(
        @Parameter(description = "Item type ID", example = "34")
        @PathVariable typeId: Int
    ): ResponseEntity<ItemType> {
        val itemType = itemTypeRepository.findById(typeId).orElse(null)
        return if (itemType != null) {
            ResponseEntity.ok(itemType)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/categories/{categoryId}/types")
    @Operation(
        summary = "Get item types by category",
        description = "Retrieves all marketable item types in a specific category"
    )
    fun getItemTypesByCategory(
        @Parameter(description = "Category ID", example = "25")
        @PathVariable categoryId: Int
    ): ResponseEntity<List<ItemType>> {
        val itemTypes = itemTypeRepository.findByCategory(categoryId)
        return ResponseEntity.ok(itemTypes)
    }
    
    @GetMapping("/groups/{groupId}/types")
    @Operation(
        summary = "Get item types by group",
        description = "Retrieves all item types in a specific group"
    )
    fun getItemTypesByGroup(
        @Parameter(description = "Group ID", example = "18")
        @PathVariable groupId: Int
    ): ResponseEntity<List<ItemType>> {
        val itemTypes = itemTypeRepository.findByGroupGroupId(groupId)
        return ResponseEntity.ok(itemTypes)
    }
}