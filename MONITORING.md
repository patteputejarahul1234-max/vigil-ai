# Monitoring — Spring Boot Actuator + Prometheus

Unlike Kubernetes/Kafka, this genuinely runs on your laptop right now
with almost no overhead — it's just a dependency and a config change,
no extra infrastructure needed.

## What it gives you
- `/actuator/health` — is the app alive (used by the Kubernetes
  readiness probe in `k8s/backend-deployment.yaml`, and useful on its
  own even without Kubernetes)
- `/actuator/metrics` — request counts, JVM memory, response times
- `/actuator/prometheus` — the same metrics in Prometheus's scrape
  format, ready for a real monitoring dashboard later

## Setup — two small edits to files you already have

### 1. Add to `backend/pom.xml`, inside the `<dependencies>` section
(anywhere among the existing dependencies — order doesn't matter):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 2. Add to `backend/src/main/resources/application.properties`
(append at the end):

```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=when-authorized
```

### 3. One more small edit — `SecurityConfig.java`

Find the `PUBLIC_ROUTES` array (same one we edited earlier for
`/swagger-ui.html`) and add the actuator health check as public, since
Kubernetes needs to reach it without a JWT token:

```java
private static final String[] PUBLIC_ROUTES = {
        "/api/auth/**",
        "/oauth2/**",
        "/login/oauth2/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/h2-console/**",
        "/actuator/health"
};
```

## Try it
```
mvn spring-boot:run
```
Then visit `http://localhost:8080/actuator/health` in your browser —
you should see `{"status":"UP"}`.

Also try `http://localhost:8080/actuator/metrics` to see the full list
of available metrics, and click into any of them (e.g.
`/actuator/metrics/jvm.memory.used`) for live numbers.
