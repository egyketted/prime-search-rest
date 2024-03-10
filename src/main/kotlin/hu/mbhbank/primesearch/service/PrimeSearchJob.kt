package hu.mbhbank.primesearch.service

import java.util.function.Supplier
import kotlin.math.sqrt
import lombok.Builder
import mu.KotlinLogging

@Builder
class PrimeSearchJob(val threadIndex: Int, val threadCount: Int, val reportMap: MutableMap<String, Long>, val resultMap: MutableMap<String, MutableSet<Long>>, val batchSize: Long, val shutdownCallback: Supplier<Boolean>) :
        Runnable {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        logger.info("Starting new prime search job with threadIndex: $threadIndex")
        resultMap["PrimeSearchJob-$threadIndex"] = HashSet()
        var iterationNumber = 0
        var previousRangeEnd = 0L

        while (true) {
            if (shutdownCallback.get()) {
                logger.info("PrimeSearchJob-$threadIndex is shutting down.")
                break
            }
            val rangeStart = (iterationNumber * threadCount + threadIndex - 1) * batchSize
            var rangeEnd = rangeStart + batchSize
            //logger.info("Searching primes in range $rangeStart .. $rangeEnd")

            if (isOverflow(previousRangeEnd, rangeEnd)) {
                logger.warn("Overflow detected, falling back to Long.Max_Value")
                rangeEnd = Long.MAX_VALUE
            }

            val iterationResult = (rangeStart..rangeEnd).filter(this::isPrime)
            reportMap["PrimeSearchJob-$threadIndex"] = iterationResult.last()
            //resultMap["PrimeSearchJob-$threadIndex"]!!.addAll(iterationResult)
            iterationNumber++
            previousRangeEnd = rangeEnd

            if (rangeEnd == Long.MAX_VALUE) {
                logger.info("Long range end reached terminating prime search job with threadIndex: $threadIndex")
                break
            }
        }
    }

    private fun isPrime(n: Long): Boolean {
        return if (n < 2) {
            false
        } else if (n == 2L) {
            true
        } else {
            if (n % 2 == 0L) {
                false
            } else {
                val max: Long = sqrt(n.toDouble()).toLong() + 1
                for (divisor in 3 until max step 2) {
                    if (n % divisor == 0L) {
                        return false
                    }
                }
                true
            }
        }
    }

    private fun isOverflow(a: Long, b: Long) = a > b
}