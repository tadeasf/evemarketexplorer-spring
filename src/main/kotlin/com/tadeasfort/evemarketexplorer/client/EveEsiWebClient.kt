package com.tadeasfort.evemarketexplorer.client

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.Semaphore

@Component
class EveEsiWebClient(
    private val rateLimiter: EveEsiRateLimiter,
    @Value("\${eve.esi.base-url}") private val baseUrl: String,
    @Value("\${eve.esi.user-agent}") private val userAgent: String,
    @Value("\${eve.client.max-connections}") private val maxConnections: Int = 100
) {
    private val logger = LoggerFactory.getLogger(EveEsiWebClient::class.java)
    private val connectionSemaphore = Semaphore(maxConnections)
    
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .codecs { configurer -> 
            configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
        }
        .build()
    
    fun <T> get(path: String, responseType: Class<T>): Mono<T> {
        return checkRateLimit()
            .then(Mono.fromCallable { connectionSemaphore.acquire() })
            .then(
                webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(responseType)
                    .doOnNext { 
                        rateLimiter.recordSuccess() 
                        logger.debug("Successful request to {}", path)
                    }
                    .doOnError { error ->
                        handleError(path, error)
                    }
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                            .filter { it !is WebClientResponseException || it.statusCode.is5xxServerError }
                    )
                    .doFinally { connectionSemaphore.release() }
            )
    }
    
    fun <T> getList(path: String, responseType: Class<T>): Mono<List<T>> {
        return checkRateLimit()
            .then(Mono.fromCallable { connectionSemaphore.acquire() })
            .then(
                webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToFlux(responseType)
                    .collectList()
                    .doOnNext { 
                        rateLimiter.recordSuccess() 
                        logger.debug("Successful list request to {} returned {} items", path, it.size)
                    }
                    .doOnError { error ->
                        handleError(path, error)
                    }
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                            .filter { it !is WebClientResponseException || it.statusCode.is5xxServerError }
                    )
                    .doFinally { connectionSemaphore.release() }
            )
    }
    
    fun <T> getListPaginated(path: String, responseType: Class<T>): Mono<List<T>> {
        return getPagedData(path, responseType, 1)
            .expand { result ->
                if (result.second < result.third) {
                    getPagedData(path, responseType, result.second + 1)
                } else {
                    Mono.empty()
                }
            }
            .map { it.first }
            .reduce { acc, list -> acc + list }
            .switchIfEmpty(Mono.just(emptyList()))
    }
    
    private fun <T> getPagedData(path: String, responseType: Class<T>, page: Int): Mono<Triple<List<T>, Int, Int>> {
        return checkRateLimit()
            .then(Mono.fromCallable { connectionSemaphore.acquire() })
            .then(
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.path(path)
                            .queryParam("page", page)
                            .build()
                    }
                    .exchangeToMono { response ->
                        val totalPages = response.headers().header("X-Pages").firstOrNull()?.toIntOrNull() ?: 1
                        
                        if (response.statusCode().is2xxSuccessful) {
                            response.bodyToFlux(responseType)
                                .collectList()
                                .map { data ->
                                    rateLimiter.recordSuccess()
                                    logger.debug("Successful paginated request to {} page {} returned {} items (total pages: {})", 
                                        path, page, data.size, totalPages)
                                    Triple(data, page, totalPages)
                                }
                        } else {
                            response.createException()
                                .flatMap { error -> Mono.error<Triple<List<T>, Int, Int>>(error) }
                        }
                    }
                    .doOnError { error ->
                        handleError(path, error)
                    }
                    .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                            .filter { it !is WebClientResponseException || it.statusCode.is5xxServerError }
                    )
                    .doFinally { connectionSemaphore.release() }
            )
    }
    
    fun <T> getConcurrentlyFromIds(
        pathTemplate: String, 
        ids: List<Int>, 
        responseType: Class<T>,
        maxConcurrency: Int = 20
    ): Flux<Pair<Int, T>> {
        return Flux.fromIterable(ids)
            .flatMap({ id ->
                get(pathTemplate.replace("{id}", id.toString()), responseType)
                    .map { response -> Pair(id, response) }
                    .onErrorResume { error ->
                        logger.error("Failed to fetch data for ID {} from {}: {}", id, pathTemplate, error.message)
                        Mono.empty()
                    }
            }, maxConcurrency)
    }
    
    private fun checkRateLimit(): Mono<Void> {
        return if (rateLimiter.isRateLimited()) {
            val delay = rateLimiter.getSecondsUntilRateLimitLifted()
            logger.warn("Rate limited. Waiting {} seconds before making request", delay)
            Mono.delay(Duration.ofSeconds(delay)).then()
        } else {
            Mono.empty()
        }
    }
    
    private fun handleError(path: String, error: Throwable) {
        when (error) {
            is WebClientResponseException -> {
                val statusCode = error.statusCode
                logger.error("HTTP {} error for path {}: {}", statusCode.value(), path, error.message)
                
                if (statusCode.is4xxClientError || statusCode.is5xxServerError) {
                    rateLimiter.recordError()
                }
                
                if (statusCode == HttpStatus.valueOf(420)) {
                    logger.error("Received 420 rate limit response from ESI")
                }
                
                processEsiHeaders(error.headers)
            }
            else -> {
                logger.error("Request error for path {}: {}", path, error.message)
                rateLimiter.recordError()
            }
        }
    }
    
    private fun processEsiHeaders(headers: HttpHeaders) {
        headers.getFirst("X-ESI-Error-Limit-Remain")?.let { remain ->
            logger.debug("ESI Error Limit Remain: {}", remain)
        }
        
        headers.getFirst("X-ESI-Error-Limit-Reset")?.let { reset ->
            logger.debug("ESI Error Limit Reset: {} seconds", reset)
        }
        
        headers.getFirst("expires")?.let { expires ->
            logger.debug("Cache expires: {}", expires)
        }
    }
    
    fun getRemainingErrorBudget(): Int = rateLimiter.getRemainingErrorBudget()
    fun isRateLimited(): Boolean = rateLimiter.isRateLimited()
    fun getAvailableConnections(): Int = connectionSemaphore.availablePermits()
}