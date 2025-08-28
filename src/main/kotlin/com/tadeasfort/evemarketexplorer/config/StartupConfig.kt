package com.tadeasfort.evemarketexplorer.config

import com.tadeasfort.evemarketexplorer.repository.RegionRepository
import com.tadeasfort.evemarketexplorer.service.EveMarketService
import com.tadeasfort.evemarketexplorer.service.EveUniverseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class StartupConfig(
    private val eveUniverseService: EveUniverseService,
    private val eveMarketService: EveMarketService,
    private val regionRepository: RegionRepository,
    @Value("\${app.startup.fetch-initial-data:true}") private val fetchInitialData: Boolean,
    @Value("\${app.startup.fetch-initial-market-data:true}") private val fetchInitialMarketData: Boolean
) : ApplicationRunner {
    
    private val logger = LoggerFactory.getLogger(StartupConfig::class.java)
    
    override fun run(args: ApplicationArguments) {
        logger.info("Starting application startup data check...")
        logger.info("Configuration: fetchInitialData={}, fetchInitialMarketData={}", fetchInitialData, fetchInitialMarketData)
        
        if (!fetchInitialData) {
            logger.info("Initial data fetching disabled by configuration. Relying on scheduled jobs only.")
            return
        }
        
        // Check if we have any data in the database
        val regionCount = regionRepository.count()
        
        if (regionCount == 0L) {
            logger.info("Database is empty. Starting initial data population...")
            
            // Start universe data fetch
            eveUniverseService.refreshUniverseData()
                .doOnSuccess {
                    logger.info("Initial universe data fetch completed successfully")
                    if (fetchInitialMarketData) {
                        startInitialMarketDataFetch()
                    } else {
                        logger.info("Initial market data fetch disabled by configuration")
                    }
                }
                .doOnError { error ->
                    logger.error("Failed to fetch initial universe data", error)
                }
                .onErrorResume { error ->
                    logger.warn("Continuing with limited functionality due to universe data fetch failure: {}", error.message)
                    Mono.empty()
                }
                .subscribe()
                
        } else {
            logger.info("Database already contains {} regions. Skipping initial data population.", regionCount)
            logger.info("Scheduled jobs will handle data updates according to their configured schedule:")
            logger.info("  - Universe data refresh: Daily at 2:00 AM")
            logger.info("  - Market data refresh: Every hour")
        }
    }
    
    private fun startInitialMarketDataFetch() {
        logger.info("Starting initial market orders fetch after universe data is ready...")
        
        // Add a small delay to ensure universe data is committed
        Mono.delay(Duration.ofSeconds(10))
            .then(eveMarketService.refreshAllMarketData())
            .doOnSuccess {
                logger.info("Initial market orders fetch completed successfully")
                logger.info("Application is now fully operational with initial data")
            }
            .doOnError { error ->
                logger.error("Failed to fetch initial market orders", error)
                logger.warn("Universe data is available, but market orders fetch failed. Manual refresh may be needed.")
            }
            .onErrorResume { error ->
                logger.warn("Market orders fetch failed: {}. Scheduled job will retry in the next cycle.", error.message)
                Mono.empty()
            }
            .subscribe()
    }
}