package com.tadeasfort.evemarketexplorer.job

import com.tadeasfort.evemarketexplorer.service.EveMarketService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class MarketDataRefreshJob : Job {
    
    @Autowired
    private lateinit var eveMarketService: EveMarketService
    
    private val logger = LoggerFactory.getLogger(MarketDataRefreshJob::class.java)
    
    override fun execute(context: JobExecutionContext) {
        val jobKey = context.jobDetail.key
        logger.info("Starting market data refresh job: {}", jobKey)
        
        try {
            eveMarketService.refreshAllMarketData()
                .doOnSuccess { 
                    logger.info("Market data refresh job completed successfully: {}", jobKey)
                }
                .doOnError { error ->
                    logger.error("Market data refresh job failed: {}", jobKey, error)
                }
                .block() // Block to ensure job completion
                
        } catch (e: Exception) {
            logger.error("Market data refresh job encountered an exception: {}", jobKey, e)
            throw e // Re-throw to mark job as failed in Quartz
        }
    }
}