package com.tadeasfort.evemarketexplorer.job

import com.tadeasfort.evemarketexplorer.service.EveUniverseService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class UniverseDataRefreshJob : Job {
    
    @Autowired
    private lateinit var eveUniverseService: EveUniverseService
    
    private val logger = LoggerFactory.getLogger(UniverseDataRefreshJob::class.java)
    
    override fun execute(context: JobExecutionContext) {
        val jobKey = context.jobDetail.key
        logger.info("Starting universe data refresh job: {}", jobKey)
        
        try {
            eveUniverseService.refreshUniverseData()
                .doOnSuccess { 
                    logger.info("Universe data refresh job completed successfully: {}", jobKey)
                }
                .doOnError { error ->
                    logger.error("Universe data refresh job failed: {}", jobKey, error)
                }
                .block() // Block to ensure job completion
                
        } catch (e: Exception) {
            logger.error("Universe data refresh job encountered an exception: {}", jobKey, e)
            throw e // Re-throw to mark job as failed in Quartz
        }
    }
}