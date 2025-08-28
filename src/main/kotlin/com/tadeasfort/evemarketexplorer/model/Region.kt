package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "regions")
data class Region(
    @Id
    @Column(name = "region_id")
    val regionId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "region", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val constellations: List<Constellation> = emptyList()
)