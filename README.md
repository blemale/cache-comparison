# Cache smackdown : TTLCacheMap vs Guava vs Caffeine

## TTLCacheMap

TTLCacheMap is a map with TTL coded from scratch.

Cons:

* Does not implement Map interface
* Performance issue: all methods (read/write) are synchronized 
* Oversimplified API: only get/put
* Hard to test
* Not java 8 friendly

## Guava cache

Applicative cache from Google (https://github.com/google/guava/wiki/CachesExplained)

Pro:

* Battle tested
* Feature complete (self loading cache, refresh strategy, stats,...)
* Good performances
* Easy to test

Cons:

* Refresh is not async by default
* Not completely java 8 friendly

## Caffeine cache

Applicative cache from Ben Manes (Guava committer) (https://github.com/ben-manes/caffeine)

Pro:

* Feature complete (self loading cache, refresh strategy, stats,...)
* Excellent performances
* Async refresh by default
* Easy to test
* Java 8 friendly

Cons:

* Less battle tested than Guava cache

Tips:

* By default uses ForkJoinPool.commonPool() to maintenance tasks and refresh calls. 
  ForkJoinPool is not design to handle blocking IO.
  Thus prefer a dedicated ExecutorService for this purpose.
