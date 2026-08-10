package moe.koseirin.nyanruaineo.utils.WebMvc;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/*
 * @author KoseiRin_
 * awa
 */
@Component
public class StrictIpResolver {
    private static final Set<String> PRIVATE_NETWORKS = Set.of(
            "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "127.", "0.", "169.254.", "224.", "240."
    );
    private static final Set<String> RESERVED_IPS = Set.of(
            "0.0.0.0", "255.255.255.255", "127.0.0.1", "localhost"
    );
    private static final Set<String> SUSPICIOUS_PATTERNS = Set.of(
            "unknown", "undefined", "null", "0.0.0.0", "255.255.255.255"
    );

    public String getStrictClientIp(HttpServletRequest request) {
        String directIp = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        String candidateIp = parseXForwardedForStrictly(xff, directIp);
        if (!isValidAndTrustworthyIp(candidateIp)) {
            candidateIp = checkOtherHeadersStrictly(request);
        }
        if (!isValidAndTrustworthyIp(candidateIp)) {
            candidateIp = directIp;
        }
        if (isSuspiciousRequest(request, candidateIp, directIp)) {
            logSuspiciousRequest(request, candidateIp, directIp);
        }

        return sanitizeIp(candidateIp);
    }

    private String parseXForwardedForStrictly(String xff, String directIp) {
        if (xff == null || xff.trim().isEmpty()) {
            return null;
        }

        String[] ips = xff.split(",");
        List<String> validIps = new ArrayList<>();

        // 严格验证每个IP
        for (String ip : ips) {
            String cleanedIp = ip.trim();
            if (isValidIpFormat(cleanedIp) &&
                    !isReservedOrPrivate(cleanedIp) &&
                    !isSuspiciousPattern(cleanedIp)) {
                validIps.add(cleanedIp);
            }
        }

        if (validIps.isEmpty()) {
            return null;
        }

        for (String ip : validIps) {
            if (!isPrivateNetwork(ip) && !isReservedIp(ip)) {
                return ip;
            }
        }

        return validIps.getFirst();
    }

    private String checkOtherHeadersStrictly(HttpServletRequest request) {
        String[][] headers = {
                {"X-Real-IP", "1"},
                {"CF-Connecting-IP", "1"},
                {"True-Client-IP", "1"},
                {"Proxy-Client-IP", "0"},
                {"WL-Proxy-Client-IP", "0"},
                {"HTTP_CLIENT_IP", "0"},
                {"HTTP_X_FORWARDED_FOR", "0"}
        };

        for (String[] header : headers) {
            String ip = request.getHeader(header[0]);
            if (isValidAndTrustworthyIp(ip)) {
                if ("0".equals(header[1])) {
                    if (!hasHighTrustIndicators(request)) {
                        continue;
                    }
                }
                return ip;
            }
        }
        return null;
    }

    private boolean hasHighTrustIndicators(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String via = request.getHeader("Via");
        if (via != null && !via.isEmpty()) {
            return true;
        }

        if (userAgent != null) {
            String lowerUserAgent = userAgent.toLowerCase();
            return lowerUserAgent.contains("proxy") ||
                    lowerUserAgent.contains("cache") ||
                    lowerUserAgent.contains("balancer");
        }

        return false;
    }

    private boolean isValidAndTrustworthyIp(String ip) {
        return isValidIpFormat(ip) &&
                !isReservedOrPrivate(ip) &&
                !isSuspiciousPattern(ip) &&
                !isLocalhost(ip);
    }

    private boolean isValidIpFormat(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

    private boolean isPrivateNetwork(String ip) {
        return PRIVATE_NETWORKS.stream().anyMatch(ip::startsWith);
    }

    private boolean isReservedIp(String ip) {
        return RESERVED_IPS.contains(ip);
    }

    private boolean isReservedOrPrivate(String ip) {
        return isPrivateNetwork(ip) || isReservedIp(ip);
    }

    private boolean isSuspiciousPattern(String ip) {
        return SUSPICIOUS_PATTERNS.stream().anyMatch(pattern ->
                pattern.equalsIgnoreCase(ip));
    }

    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "localhost".equalsIgnoreCase(ip) ||
                "::1".equals(ip) || ip.startsWith("127.");
    }

    private boolean isSuspiciousRequest(HttpServletRequest request, String candidateIp, String directIp) {
        long ipHeaderCount = Collections.list(request.getHeaderNames()).stream()
                .filter(header -> header.toUpperCase().contains("IP"))
                .count();

        if (ipHeaderCount > 3) {
            return true;
        }

        if (!candidateIp.equals(directIp) &&
                (isPrivateNetwork(directIp) && !isPrivateNetwork(candidateIp))) {
            return true;
        }

        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && (xff.contains("unknown") || xff.contains("undefined"));
    }

    private String sanitizeIp(String ip) {
        if (ip == null) return "0.0.0.0";
        ip = ip.replaceAll("[^0-9.]", "");
        return isValidIpFormat(ip) ? ip : "0.0.0.0";
    }

    private void logSuspiciousRequest(HttpServletRequest request, String candidateIp, String directIp) {
        String logMessage = String.format(
                "Suspicious IP detection - Candidate: %s, Direct: %s, Headers: %s",
                candidateIp, directIp, getIpHeaders(request)
        );
        Logger.getLogger("NyanID").warning(logMessage);
    }

    private String getIpHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .filter(header -> header.toUpperCase().contains("IP"))
                .map(header -> header + "=" + request.getHeader(header))
                .collect(Collectors.joining(", "));
    }
}
