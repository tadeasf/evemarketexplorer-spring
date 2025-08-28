package com.tadeasfort.evemarketexplorer.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class EveEsiRateLimiter(
    private val maxErrorCount: Int = 90,
    private val errorWindowSeconds: Long = 60L
) {
    private val logger = LoggerFactory.getLogger(EveEsiRateLimiter::class.java)
    
    private val errorCount = AtomicInteger(0)
    private val windowStartTime = AtomicLong(Instant.now().epochSecond)
    private val isRateLimited = AtomicLong(0L)
    
    fun recordError() {
        val now = Instant.now().epochSecond
        val windowStart = windowStartTime.get()
        
        synchronized(this) {
            if (now - windowStart >= errorWindowSeconds) {
                windowStartTime.set(now)
                errorCount.set(0)
                logger.debug("Error window reset at {}", now)
            }
            
            val currentErrors = errorCount.incrementAndGet()
            logger.warn("Error recorded. Current error count: {}/{} in window starting at {}", 
                currentErrors, maxErrorCount, windowStart)
            
            if (currentErrors >= maxErrorCount) {
                val resetTime = windowStart + errorWindowSeconds
                isRateLimited.set(resetTime)
                logger.error("Rate limit exceeded! {} errors in {} seconds. Rate limited until {}", 
                    currentErrors, errorWindowSeconds, resetTime)
            }
        }
    }
    
    fun recordSuccess() {
        logger.trace("Successful request recorded")
    }
    
    fun isRateLimited(): Boolean {
        val rateLimitedUntil = isRateLimited.get()
        if (rateLimitedUntil == 0L) {
            return false
        }
        
        val now = Instant.now().epochSecond
        if (now >= rateLimitedUntil) {
            synchronized(this) {
                if (now >= isRateLimited.get()) {
                    isRateLimited.set(0L)
                    errorCount.set(0)
                    windowStartTime.set(now)
                    logger.info("Rate limit lifted at {}", now)
                    return false
                }
            }
        }
        
        logger.debug("Still rate limited until {}, current time: {}", rateLimitedUntil, now)
        return true
    }
    
    fun getRemainingErrorBudget(): Int {
        return maxOf(0, maxErrorCount - errorCount.get())
    }
    
    fun getSecondsUntilWindowReset(): Long {
        val windowStart = windowStartTime.get()
        val now = Instant.now().epochSecond
        return maxOf(0, (windowStart + errorWindowSeconds) - now)
    }
    
    fun getSecondsUntilRateLimitLifted(): Long {
        val rateLimitedUntil = isRateLimited.get()
        if (rateLimitedUntil == 0L) {
            return 0L
        }
        val now = Instant.now().epochSecond
        return maxOf(0, rateLimitedUntil - now)
    }
}