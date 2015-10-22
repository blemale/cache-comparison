# Cache smackdown : TTLCacheMap vs Guava vs Caffeine

## TTLCacheMap

TTLCacheMap is a map with TTL coded from scratch.

Issues:

* Does not implement Map interface
* Performance issue: all methods (read/write) are synchronized 
* Oversimplified API: only get/put
* Not java 8 friendly