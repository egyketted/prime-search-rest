package hu.mbhbank.primesearch.controller

import hu.mbhbank.primesearch.service.PrimeResponse
import hu.mbhbank.primesearch.service.PrimeResponseWithMessage
import hu.mbhbank.primesearch.service.PrimeResponseWithResult
import hu.mbhbank.primesearch.service.PrimeService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/prime-search")
class PrimeController(private val primeService: PrimeService) {

    @Value("\${prime.threads.max:1}")
    private var maxThreadNumber: Int = 1

    @Value("\${prime.result.max-count:1000}")
    private var maxResultCount: Long = 1000

    @PostMapping
    fun startSearch(@RequestParam threadNumber: Int): ResponseEntity<PrimeResponse> {
        return if (threadNumber > maxThreadNumber) {
            ResponseEntity.badRequest()
                .body(PrimeResponseWithMessage("Invalid threadCount provided. Maximum number of threads is $maxThreadNumber"))
        } else if (threadNumber < 1) {
            ResponseEntity.badRequest().body(PrimeResponseWithMessage("Invalid threadCount provided. Minimum number of threads is 1"))
        } else if (primeService.isSearchRunning()) {
            ResponseEntity.badRequest()
                .body(PrimeResponseWithMessage("A search is already running, you can terminate it with POST /api/prime-search/stop or query results with GET /api/prime-search?from=&to="))
        } else {
            primeService.startSearch(threadNumber)
            ResponseEntity.ok()
                .body(PrimeResponseWithMessage("Prime search started with $threadNumber threads. You can query results on GET /api/prime-search?from=&to="))
        }
    }

    @GetMapping
    fun getResults(@RequestParam from: Long, @RequestParam to: Long): ResponseEntity<PrimeResponse> {
        return if (from >= to) {
            ResponseEntity.badRequest()
                .body(PrimeResponseWithMessage("Range start is greater then or equal to Range end. Please provide a valid Range."))
        } else if (to < 1) {
            ResponseEntity.badRequest()
                .body(PrimeResponseWithMessage("Range start is less than 1. Prime numbers can only be positive numbers. Please provide a valid Range."))
        } else if (primeService.getCurrentBiggestPrime() < to) {
            ResponseEntity.badRequest().body(PrimeResponseWithMessage("No search run for the provided range yet. Try again later."))
        } else if (primeService.areResultsMoreThenLimit(from, to, maxResultCount)) {
            ResponseEntity.badRequest()
                .body(PrimeResponseWithMessage("There are more then $maxResultCount results for the given range. Please try with a smaller range."))
        } else {
            ResponseEntity.ok().body(PrimeResponseWithResult(primeService.getResults(from, to)))
        }
    }

    @PostMapping("/stop")
    fun stopSearch(): ResponseEntity<PrimeResponse> {
        return if (!primeService.isSearchRunning()) {
            ResponseEntity.badRequest().body(PrimeResponseWithMessage("No active search to stop."))
        } else {
            primeService.stopSearch()
            ResponseEntity.ok()
                .body(PrimeResponseWithMessage("Prime search stopped. You can query results on GET /api/prime-search?from=&to="))
        }
    }

}