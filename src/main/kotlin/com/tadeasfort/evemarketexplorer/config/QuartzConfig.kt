package com.tadeasfort.evemarketexplorer.config

import com.tadeasfort.evemarketexplorer.job.MarketDataRefreshJob
import com.tadeasfort.evemarketexplorer.job.UniverseDataRefreshJob
import org.quartz.*
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.JobDetailFactoryBean
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.SpringBeanJobFactory

@Configuration
class QuartzConfig {
    
    @Bean
    fun universeDataRefreshJobDetail(): JobDetail {
        return JobBuilder.newJob(UniverseDataRefreshJob::class.java)
            .withIdentity("universeDataRefreshJob", "dataRefresh")
            .withDescription("Daily job to refresh universe data (regions, systems, constellations, item types)")
            .usingJobData("jobType", "universeRefresh")
            .storeDurably()
            .build()
    }
    
    @Bean
    fun universeDataRefreshTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(universeDataRefreshJobDetail())
            .withIdentity("universeDataRefreshTrigger", "dataRefresh")
            .withDescription("Trigger for daily universe data refresh at 2:00 AM")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 2 * * ?") // Daily at 2:00 AM
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .build()
    }
    
    @Bean
    fun marketDataRefreshJobDetail(): JobDetail {
        return JobBuilder.newJob(MarketDataRefreshJob::class.java)
            .withIdentity("marketDataRefreshJob", "marketRefresh")
            .withDescription("Hourly job to refresh market data for all regions")
            .usingJobData("jobType", "marketRefresh")
            .storeDurably()
            .build()
    }
    
    @Bean
    fun marketDataRefreshTrigger(): Trigger {
        return TriggerBuilder.newTrigger()
            .forJob(marketDataRefreshJobDetail())
            .withIdentity("marketDataRefreshTrigger", "marketRefresh")
            .withDescription("Trigger for hourly market data refresh")
            .withSchedule(
                CronScheduleBuilder.cronSchedule("0 0 * * * ?") // Every hour at the top of the hour
                    .withMisfireHandlingInstructionFireAndProceed()
            )
            .build()
    }
    
    @Bean
    fun quartzJobFactory(): JobFactory {
        return AutowiringSpringBeanJobFactory()
    }
    
    @Bean
    fun schedulerFactoryBean(): SchedulerFactoryBean {
        val factory = SchedulerFactoryBean()
        factory.setJobFactory(quartzJobFactory())
        factory.setJobDetails(
            universeDataRefreshJobDetail(),
            marketDataRefreshJobDetail()
        )
        factory.setTriggers(
            universeDataRefreshTrigger(),
            marketDataRefreshTrigger()
        )
        factory.setWaitForJobsToCompleteOnShutdown(true)
        factory.setOverwriteExistingJobs(true)
        return factory
    }
}

class AutowiringSpringBeanJobFactory : SpringBeanJobFactory(), ApplicationContextAware {
    private lateinit var beanFactory: AutowireCapableBeanFactory
    
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        beanFactory = applicationContext.autowireCapableBeanFactory
    }
    
    override fun createJobInstance(bundle: TriggerFiredBundle): Any {
        val job = super.createJobInstance(bundle)
        beanFactory.autowireBean(job)
        return job
    }
}