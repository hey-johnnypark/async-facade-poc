# Async Facade POC
POC for async rest facade dispatching requests to and reading responses from Kafka

# Flow
+------------+           +----------------+  -->  +-----+  -->  +-----------------+
|api-loadtest|  -http->  |async-api-facade|       |Kafka|       |async-worker-mock|
+------------+           +----------------+  <--  +-----+  <--  +-----------------+

