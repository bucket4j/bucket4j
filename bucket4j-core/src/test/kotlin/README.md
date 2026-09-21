# Kotest style guide

Tracking issue: https://github.com/bucket4j/bucket4j/issues/624

New tests, and tests migrated from `src/test/java/**/*Specification.groovy` (Spock), go here as
Kotlin/Kotest specs. The migration is incremental — Groovy/Spock and Kotlin/Kotest specs coexist
until every `*Specification.groovy` file has been ported, at which point the Groovy/Spock/gmavenplus
build wiring will be removed.

## Conventions

- **Package/class layout mirrors the Groovy source it replaces.** When migrating
  `src/test/java/io/github/bucket4j/foo/BarSpecification.groovy`, delete it and create
  `src/test/kotlin/io/github/bucket4j/foo/BarSpecification.kt` with the same class name, so the
  spec keeps matching the `**/*Specification.class` / `**/*Test.class` surefire include patterns
  without touching the shared parent POM.
- **Default style: `BehaviorSpec`** (`Given` / `When` / `Then`) for scenario-style tests, to keep
  the Given-When-Then readability Spock gave us.
- **Exception: sequential/stateful tests use `FunSpec`.** If a Spock feature method is really a
  single chain of state transitions checked at several points in time (e.g. advancing a mock clock
  step by step and asserting after each step), migrate it to a flat `FunSpec` `test("...") { }`
  block with plain, procedural Kotlin instead of nesting `Given`/`When`/`Then`. See the
  **pitfall** below for why.
- Prefer Kotest's typed matchers (`shouldBe`, `shouldBeExactly`, `shouldThrow`, ...) from
  `io.kotest.matchers.*` over manual `assert`/boolean checks — they produce readable failure
  messages, same as Spock's power assert.
- Numeric literals: bucket4j's public API returns `long`. Match with `Long` literals (`0L`, not
  `0`) or `shouldBeExactly` from `io.kotest.matchers.longs`, since `Long(0) != Int(0)` under
  Kotlin's `equals`.

## Pitfall: nested `Given`/`When`/`Then` does NOT execute like a Spock feature method

Spock's `when:`/`then:` blocks inside one feature method execute once, top to bottom, sharing
mutable local state — so a chain like "advance clock by 4, assert, advance clock by 6, assert" just
accumulates.

Kotest's `BehaviorSpec`/`DescribeSpec` containers don't work that way: **each leaf test replays
its own path from the root**, re-running every ancestor `Given`/`When` block on the way down. Two
sibling `When` blocks under the same `Given` do NOT see each other's mutations — each starts fresh
from the `Given` setup. To reproduce Spock's cumulative behavior with `BehaviorSpec`, you must nest
each subsequent step *inside* the previous `When` (not as a sibling), which gets unreadable fast for
more than 2-3 steps — hence the `FunSpec` exception above for that shape of test.

See `io.github.bucket4j.core_algorithms.FixedIntervalRefillSpecification` (`FunSpec`, cumulative
mock-clock steps) and `io.github.bucket4j.ToStringSpecification` (`BehaviorSpec`, single scenario)
for worked examples of both styles.

## Build wiring

Kotlin test sources are compiled by `kotlin-maven-plugin`, bound to the `process-test-classes`
phase (i.e. after Java's `test-compile` and Groovy's `gmavenplus:compileTests`), so Kotest specs
can reference Java/Groovy test-scope classes such as `io.github.bucket4j.mock.*`. See
`bucket4j-core/pom.xml`.
