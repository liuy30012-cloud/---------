# Maintainability Refactor Checklist

## Priority 1

- Split request-defense responsibilities in the filter chain.
  `RateLimitFilter` currently mixes IP bans, captcha gating, suspicion scoring, and response shaping.
- Extract a dedicated challenge coordinator.
  Captcha pass-token validation and challenge triggering should move behind a small service so filters only decide allow or deny.
- Centralize trusted-proxy and anti-crawler configuration.
  Keep `anti-crawler.*` as the primary namespace and retain legacy aliases only as temporary compatibility shims.

## Priority 2

- Isolate time-based behavior behind `Clock`.
  Security state storage, rate limiting, and crawler pattern analysis should continue using injected clocks so tests stay deterministic.
- Replace ad-hoc request maps with DTOs.
  `SearchHistoryController` was a good example of why raw `Map<String, Object>` request bodies are hard to validate and harder to maintain.
- Reduce static global state.
  `ClientIpResolver` still uses static trusted-proxy state; the next refactor should wrap this in a bean-backed resolver to remove cross-test coupling.

## Priority 3

- Add focused unit tests before integration tests for security features.
  Preferred order: state store, service, controller/filter integration.
- Keep public error messages stable and short.
  Validation failures should stay user-safe while logs carry the technical detail.
- Document every new anti-abuse check with:
  expected headers and config keys,
  failure response shape,
  one deterministic unit test,
  one integration test if the check sits in the filter chain.

## Extension Rules

- New rate limits must declare scope explicitly.
  Choose `ip`, `user`, or both and test rollover between buckets.
- New crawler heuristics must be deterministic under test.
  If the heuristic depends on time, randomness, or cleanup windows, inject collaborators instead of calling system APIs directly.
- New honeypot endpoints must support dry-run mode first.
  Validate logging and false-positive behavior before enabling automatic bans in production.
