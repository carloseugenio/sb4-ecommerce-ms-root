# Phase 0 - Project Review (read-only)

Scope: static review only. No Gradle build/download was run. Not yet verified: CVE scan, build/test status, git history secret scan.
Severity: H = fix before Phase 1, M = fix during Phase 1/2, L = opportunistic.

## R1 Build system
| # | Sev | Finding | Proposed fix |
|---|-----|---------|--------------|
| 1 | H | `settings.gradle.kts` includes `services:inventory-service` but it has no `src` and no Spring plugins; `notification-service` is commented out; no `api-gateway`/`catalog`/`cart` modules. | Add modules as phases need them, each with a minimal bootable class + test so the build stays green. |
| 2 | L | `projectDir` mappings in settings are redundant (`include("a:b")` already maps to `a/b`); `common-spring` is included but has no mapping (works) - inconsistent. | Remove the mappings block. |
| 3 | H | Root `build.gradle.kts` applies `implementation(platform(bom))` and `spring-boot-starter-test` to ALL subprojects with JavaPlugin, and each module repeats the platform; `buildSrc` convention plugin `sb4-ecommerce-ms-root.spring-commons` is fully commented out and unused. | Pick one mechanism: finish the buildSrc convention plugin (java 21 toolchain, JUnit, BOM, test deps) and remove duplication from root and modules. |
| 4 | M | No Java toolchain set (code relies on JDK 21 present: 21.0.10 found). | Set `java.toolchain` to 21 in the convention plugin. |
| 5 | M | `gradle.properties` has parallel/caching/configuration-cache commented out; `run-converage.sh` passes `--no-configuration-cache` (jacoco `testCodeCoverageReport` is not configured anywhere found). | Enable `org.gradle.parallel` and `caching` after Phase 1 build is green; add the jacoco aggregation plugin or drop the script. |
| 6 | M | Version catalog: `testcontainers = 1.19.0` (old; Boot 4 manages 2.x `testcontainers-*` names, mixed `org.testcontainers:postgresql` vs `testcontainers-postgresql`), `h2 = 2.1.214` (old, has CVEs) used as the version of `spring-boot-h2console` (wrong mapping), `micrometer-registry-cloudwatch 1.12.0`/`otel 1.35.0` pinned against Boot BOM, `postgresql 42.7.7` pinned against BOM, `spring-cloud = 2026.0.0` declared but spring-cloud BOM is never imported, `aws-sdk 2.25.0` / `spring-cloud-aws 3.1.0` outdated. Many unused entries. | Remove version pins that the Boot BOM manages; import spring-cloud BOM when the gateway is added; confirm Boot 4.1/Spring Cloud 2026.0 compatibility; delete unused entries. Needs download approval to verify. |
| 7 | M | `container-validation.gradle.kts` makes every `JavaCompile` task of Boot modules depend on a Docker/Podman check (only when `spring-boot-docker-compose` is present). Because root adds that dependency to all Boot modules, compile fails without a container engine (CI and WSL included). | Attach the check to `bootRun` only; never to compile. |
| 8 | M | Root applies `from("container-validation.gradle.kts")` with `project.apply`; the file has mojibake emoji (encoding issue). | Save as UTF-8, drop emoji. |
| 9 | M | Boot `bootRun` sets `spring.docker.compose.file` to each module's `docker-compose.yaml`, but only order-service has one. | Provide a per-service compose or one root compose with profiles. |
| 10 | L | Wrapper Gradle 9.3.0 (ok). | - |

## R2 Repository hygiene
| # | Sev | Finding | Fix |
|---|-----|---------|-----|
| 1 | H | `.idea/*` is tracked in git (`workspace.xml`, `dataSources.local.xml`, `dataSources.xml`); `.idea/` is not ignored (only some files). Can leak local DB connection info. | `git rm --cached -r .idea`, ignore `.idea/` entirely. |
| 2 | M | `idea-old/` folder committed. | Delete. |
| 3 | H | `docker-compose.txt` at root: wrong extension; uses undefined `${DB_IMAGE_TAG}`/`${DB_PASSWORD}`; `customer-db` password hard-coded `1234`. | Rename to `docker-compose.yml`, move values to `.env` (ignored) + commit `.env.example`. |
| 4 | M | `services/order-service/docker-compose.yaml` duplicates the root order-db definition with different env var names (`DB_USER`, `DB_CONTAINER_NAME`, `DB_HOST_PORT`) vs application.yaml (`DB_USERNAME`, ...). | One compose source of truth; align variable names. |
| 5 | L | `propmpt.md` typo in name; `.gitignore` is missing `.idea/`, `*.log`, `.env.*`. | Rename `prompt.md`, extend ignore. |
| 6 | M | `docs/` contains a saved copy of a Medium article (paid/copyrighted content) and its image. | Remove from the repo; keep only the link in README. |

## R3 Modules and dependency direction
| # | Sev | Finding | Fix |
|---|-----|---------|-----|
| 1 | M | `common-domain`, `common-messaging`, `common-persistence` have no source (empty/placeholder); `inventory-service` empty. | Populate during Phase 2 (envelope, Money, outbox). |
| 2 | H | Libraries apply `spring-boot` with `apply false` and dependency-management; services depend on libs with `implementation(...)`. Lib dependencies are `implementation`, so transitive API (e.g. security types, validation) is not exposed - use `api` only where types appear in public signatures. | Review per lib when real code lands. |
| 3 | M | `common-domain` uses Lombok with `compileOnly`; `common-security` uses `implementation(libs.lombok)` (should be `compileOnly`). | Fix. |
| 4 | M | `common-spring` is not used by any service; `SharedSecurityConfig` is in `common-spring` but payment scans `br.com.cepp.ecommerce.security` (package does not exist; actual is `...platform.spring.security`). | Fix packages; prefer Spring Boot auto-configuration (`AutoConfiguration.imports`) in libs over component scan. |
| 5 | M | No service depends on another service (good). Direction rule must be tested (ArchUnit or a Gradle check). | Add check in Phase 1. |
| 6 | M | Inconsistent module style: order uses catalog aliases, payment uses string coordinates and `gradle.properties` copied from a template (spock, bouncyCastle, `your.package.name`), customer minimal. | Normalize with the convention plugin; delete payment's `gradle.properties` template junk. |

## R4 Code
| # | Sev | Finding | Fix |
|---|-----|---------|-----|
| 1 | H | `Order`/`OrderItem` are anonymous anemic `@Data` classes (not JPA entities, public setters, no state transitions, builder lets anyone set any status, `totalAmount` independent of items, `Integer`/`BigDecimal` primitives instead of Money). `OrderStatus` has PROCESSING/SHIPPED/DELIVERED outside the checkout saga. | Rebuild as aggregate with private state, factory `Order.place(...)`, methods `confirm()/cancel(reason)` enforcing transitions; money as `BigDecimal`+currency record. |
| 2 | M | `OrderTest` only tests the Lombok builder (no behavior); method naming `Given_When_Then` upper-case style. | Replace with state-machine tests. |
| 3 | H | `JwtTokenProvider`: reads secret as BASE64 but the default secret in config is a plain string (`YourSuper...`) - not valid Base64 -> startup/ decode failure or weak; secret committed in `application.yaml`; HS256 symmetric tokens issued by each service (no central auth). Order-service uses `jwt.expiration-minutes` but provider reads `jwt.expiration` (mismatch). `validateToken` swallows exceptions silently. `JwtTokenProviderTest` is entirely commented out. | Plan: use OAuth2 Resource Server (Cognito/Keycloak JWT) at gateway + services; remove self-signed JWT or keep only for local dev with secret from env. |
| 4 | M | `SharedSecurityConfig`: CSRF disabled (OK for stateless API) but no resource-server config, so `anyRequest().authenticated()` falls back to default form login/basic with generated password; `/actuator/**` fully public (exposes env/metrics if `include: *`). | Stateless session policy, resource server, restrict actuator to health/info. |
| 5 | M | `SharedGlobalExceptionHandler` is an empty class. | Implement `@RestControllerAdvice` returning ProblemDetail. |
| 6 | L | `CustomerServiceApplication`, `OrderServiceApplication` fine; `PaymentServiceApplication` uses `scanBasePackages` with wrong package. | See R3.4. |

## R5 Configuration
| # | Sev | Finding | Fix |
|---|-----|---------|-----|
| 1 | H | order-service `application.yaml`: `server.port`/`shutdown` are nested under `spring:` (ignored); `spring.docker.compose.skip.in-tests`-style key is malformed (`.inbound-ports`); both `flyway.enabled=false` and `liquibase.enabled=false`; so no migrations run while `ddl-auto: validate` -> startup fails with no schema. Default DB user `order-db`, password `1234`, JWT secret in file. | Fix structure, enable Liquibase, externalize secrets via env/`.env`. |
| 2 | H | payment-service `application.yaml`: `datasource` nested under `spring.config` (ignored), `management` nested under `spring` (ignored), `generate-ddl: true` alongside Liquibase, `naming_strategy` obsolete property, `allow-bean-definition-overriding`, devtools/h2-console in main config; no PostgreSQL profile. | Rewrite config; dev profile H2/Testcontainers; PostgreSQL by default. |
| 3 | M | Ports: order 8033, payment 8082, customer none (default 8080) -> collisions; no port plan. | Port plan: gateway 8080, customer 8081, order 8082, payment 8083, inventory 8084, notification 8085, catalog 8086. |
| 4 | M | Liquibase `includeAll path db/changelog/datasets/` - folder does not exist in order/payment/customer resources (only master file). includeAll ordering by file name; no rollback conventions. | Create changelog folder and naming convention `NNN-description.yaml`; use explicit includes. |
| 5 | M | order: `management.metrics.export.cloudwatch.enabled: true` with no CloudWatch dependency (property path also outdated); logging DEBUG + `org.hibernate.SQL: DEBUG` by default; `health.show-details: always`; `.env` import `optional:file:.env[.properties]` is OK. | Dev-only logging in profile; actuator hardening. |
| 6 | M | customer-service has no `application.yaml` in src (earlier listing showed it, but content missing) - verify build output vs sources; dev profile mentioned only in ordering. | Re-check on Phase 3. |

## R6 Tests
- Only 2 test files; JwtTokenProviderTest is empty (passes vacuously), OrderTest is trivial. Testcontainers dependencies exist but no tests use them. Build status unknown (needs approved Gradle run).
- Add: Testcontainers Postgres/Kafka base test class, ArchUnit module-direction test, jacoco aggregate report.

## R7 Docs
- README: directory tree has mojibake (encoding) and describes `infrastructure/terraform`, `notification-service` that do not exist; no mention of Kafka/outbox/saga/idempotency/gateway. Needs update after plan decisions.
- No ADR folder/index; plan requires `docs/adr/0001..0005`.
- Copyrighted article copy in docs (see R2.6).

## R8 Security baseline
- Secrets in repo: `jwt.secret` default in `common-security/application.yaml` and order `application.yaml`; DB password `1234` in `docker-compose.txt` and order defaults. `.env` is ignored (good) but no `.env.example`.
- `.idea/dataSources*.xml` tracked (may contain connection URLs/usernames).
- NOT done: git-history secret scan and dependency CVE scan (h2 2.1.214, testcontainers 1.19.0 suspects) - requires your approval for a download/scan tool.

## R9 Compatibility
- JDK 21.0.10 present in the environment (matches toolchain); repo lives under WSL: run Gradle from WSL/git bash (not via `\\wsl.localhost` UNC path from Windows; git also reports "dubious ownership" over UNC).
- Boot 4.1.0 / Spring Cloud 2026.0.0 / Gradle 9.3.0: starter names used (`spring-boot-starter-restclient`, `-webmvc-test`, `spring-boot-h2console`) match Boot 4 modularization but must be verified by an actual resolve (requires download approval).
- Docker/Podman needed locally for compose and Testcontainers.

## R10 Synthesis - changes to the plan
1. Add **Phase 1a "stabilize"** before ADRs: fix R1.3/1.7, R2.1/2.3, R5.1/5.2 so `./gradlew build` is green in WSL with no container engine required.
2. Replace self-issued JWT with OAuth2 resource server (Cognito dev / Keycloak local); keep `common-security` small.
3. Rebuild `Order` as a proper aggregate in the saga phase (S3); trim `OrderStatus` to PENDING/CONFIRMED/CANCELLED for now.
4. Add port plan, `.env.example`, single compose, ArchUnit module-direction test, jacoco aggregate.
5. Remove copyrighted docs, `.idea`, `idea-old`.

### Accepted risks until fixed
- No verified baseline build or CVE scan yet.

### Sign-off
[x] User approved (2026-11-02); this review and the R10 plan changes -> Phase 1a may start.


## Phase 1a result
Done: container check only on bootRun, order/payment config fixed, inventory-service bootable, compose/.env.example, .idea untracked, idea-old and article copy removed, ADR-0001/0002. ./gradlew build --offline (WSL, JDK 21) is green. Remaining from review: convention plugin, version-catalog cleanup, JWT/security rework, Order aggregate (next phases).
