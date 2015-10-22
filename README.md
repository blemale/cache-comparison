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