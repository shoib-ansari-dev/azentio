---
name: kafka-streaming-phase
description: Kafka + Spring Cloud Stream real-time transaction pipeline is planned as the next phase after core implementation is complete
metadata:
  type: project
---

After the core implementation is complete (data model, detection engine, alerts, cases, REST API), implement a real-time streaming pipeline:

- Kafka + Spring Cloud Stream for continuous transaction ingestion
- Sub-second alerting on the streaming path

**Why:** User explicitly requested this as the next feature after the core backend is done.

**How to apply:** Do not include Kafka in the initial implementation. Once REST API + detection engine + case management are working, add Kafka as a separate phase. Update pom.xml with `spring-cloud-stream` + `spring-cloud-starter-stream-kafka` at that point.
