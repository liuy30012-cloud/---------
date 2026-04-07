package com.library.util;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class ClientIpResolver {

    private static volatile List<CidrRange> trustedProxyRanges = defaultTrustedProxyRanges();

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String directIp = normalizeIp(request.getRemoteAddr());
        if (!isTrustedProxy(directIp)) {
            return directIp;
        }

        List<String> forwardedFor = splitHeader(request.getHeader("X-Forwarded-For"));
        if (!forwardedFor.isEmpty()) {
            for (int i = forwardedFor.size() - 1; i >= 0; i--) {
                String candidate = normalizeIp(forwardedFor.get(i));
                if (!"unknown".equals(candidate) && !isTrustedProxy(candidate)) {
                    return candidate;
                }
            }
            return normalizeIp(forwardedFor.get(0));
        }

        String proxiedIp = firstUsableHeader(
                request.getHeader("X-Real-IP"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP")
        );
        return proxiedIp != null ? normalizeIp(proxiedIp) : directIp;
    }

    public static void setTrustedProxyCidrs(Collection<String> cidrs) {
        trustedProxyRanges = parseCidrs(cidrs);
    }

    public static boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }

        for (CidrRange range : trustedProxyRanges) {
            if (range.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitHeader(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return List.of();
        }

        List<String> parts = new ArrayList<>();
        for (String item : headerValue.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty() && !"unknown".equalsIgnoreCase(trimmed)) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private static String firstUsableHeader(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && !"unknown".equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    private static List<CidrRange> defaultTrustedProxyRanges() {
        return parseCidrs(List.of(
                "127.0.0.1/32",
                "::1/128"
        ));
    }

    private static List<CidrRange> parseCidrs(Collection<String> cidrs) {
        List<CidrRange> ranges = new ArrayList<>();
        if (cidrs == null) {
            return defaultTrustedProxyRanges();
        }

        for (String cidr : cidrs) {
            if (cidr == null || cidr.isBlank()) {
                continue;
            }
            ranges.add(CidrRange.parse(cidr.trim()));
        }

        if (ranges.isEmpty()) {
            return defaultTrustedProxyRanges();
        }
        return List.copyOf(ranges);
    }

    private record CidrRange(InetAddress networkAddress, BigInteger networkBits, int prefixLength) {

        static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr);
            }

            try {
                InetAddress address = InetAddress.getByName(parts[0]);
                int prefixLength = Integer.parseInt(parts[1]);
                int totalBits = address.getAddress().length * 8;
                if (prefixLength < 0 || prefixLength > totalBits) {
                    throw new IllegalArgumentException("Invalid prefix length in CIDR: " + cidr);
                }

                BigInteger networkBits = new BigInteger(1, address.getAddress());
                return new CidrRange(address, networkBits, prefixLength);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid CIDR: " + cidr, ex);
            }
        }

        boolean matches(String ip) {
            try {
                InetAddress candidate = InetAddress.getByName(ip);
                if (candidate.getAddress().length != networkAddress.getAddress().length) {
                    return false;
                }

                if (prefixLength == 0) {
                    return true;
                }

                int totalBits = candidate.getAddress().length * 8;
                BigInteger candidateBits = new BigInteger(1, candidate.getAddress());
                int shift = totalBits - prefixLength;
                return networkBits.shiftRight(shift).equals(candidateBits.shiftRight(shift));
            } catch (Exception ex) {
                return false;
            }
        }
    }
}
