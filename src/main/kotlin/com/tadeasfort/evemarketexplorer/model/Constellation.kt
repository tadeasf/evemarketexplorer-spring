package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "constellations")
data class Constellation(
    @Id
    @Column(name = "constellation_id")
    val constellationId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    val region: Region,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "constellation", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val systems: List<SolarSystem> = emptyList()
)