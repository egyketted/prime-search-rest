package hu.mbhbank.primesearch.service

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PrimeService {

    private val logger = KotlinLogging.logger {}

    @Value("\${prime.threads.batch-size:1000}")
    private var batchSize: Long = 1000
    private var resultCache: ResultCache? = null
    private var executor: ExecutorService? = null

    private val currentMaxPrimes: MutableMap<String, Long> = HashMap() //to check the current progress of the search
    private val results: MutableMap<String, MutableSet<Long>> = HashMap()

    fun startSearch(threadNumber: Int) {
        logger.info("Starting prime search")
        currentMaxPrimes.clear()
        results.clear()
        executor = Executors.newFixedThreadPool(threadNumber)

        for (threadIndex in 1..threadNumber) {
            executor?.submit(PrimeSearchJob(threadIndex, threadNumber, currentMaxPrimes, results, batchSize, executor!!::isShutdown))
        }
    }

    fun isSearchRunning(): Boolean {
        return executor != null && !executor!!.isShutdown
    }

    fun getResults(from: Long, to: Long): List<Long> {
        return getResultsBetween(from, to)
    }

    fun areResultsMoreThenLimit(from: Long, to: Long, limit: Long): Boolean = getResultsBetween(from, to).size > limit

    fun getCurrentBiggestPrime(): Long = currentMaxPrimes.values.max()

    fun stopSearch() {
        logger.info("Stopping prime search")
        executor!!.shutdownNow()
    }

    private fun getResultsBetween(from: Long, to: Long): List<Long> {
        if (resultCache == null || !resultCache?.equals(ResultCache(from, to, ArrayList()))!!) {
            resultCache = ResultCache(from, to, results.map { it.value }.flatten().filter { it in from..to })
        }

        return resultCache!!.cache
    }
}