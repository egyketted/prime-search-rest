package hu.mbhbank.primesearch.service

import lombok.EqualsAndHashCode

@EqualsAndHashCode(exclude = ["cache"])
class ResultCache(val from: Long, val to: Long, val cache: List<Long>) {

}