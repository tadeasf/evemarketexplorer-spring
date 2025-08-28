package com.tadeasfort.evemarketexplorer.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "solar_systems")
data class SolarSystem(
    @Id
    @Column(name = "system_id")
    val systemId: Int,
    
    @Column(name = "name", nullable = false, length = 255)
    val name: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constellation_id", nullable = false)
    val constellation: Constellation,
    
    @Column(name = "security_status", precision = 4, scale = 3)
    val securityStatus: BigDecimal? = null,
    
    @Column(name = "star_id")
    val starId: Int? = null,
    
    @Column(name = "position_x", precision = 20, scale = 2)
    val positionX: BigDecimal? = null,
    
    @Column(name = "position_y", precision = 20, scale = 2)
    val positionY: BigDecimal? = null,
    
    @Column(name = "position_z", precision = 20, scale = 2)
    val positionZ: BigDecimal? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)