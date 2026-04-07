# Security Configuration Notes

## Anti-crawler signature mode

- `anti-crawler.signature.enabled=true` now requires a custom `anti-crawler.signature.secret`.
- The secret must be at least 32 characters long.
- The placeholder repository secret is rejected at startup to avoid deploy-time misconfiguration.

## Trusted proxies

- Preferred property: `anti-crawler.trusted-proxies`
- Backward-compatible fallback: `security.trusted-proxies`
- Supply the exact CIDR ranges for your load balancer, reverse proxy, or CDN so client IP extraction does not collapse to the balancer address.

## Honeypot rollout

- `anti-crawler.honeypot.dry-run=true` keeps honeypot endpoints active without banning visitors.
- Use dry-run mode first in new environments to validate logs and false-positive rates before enabling bans.
