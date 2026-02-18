package com.skyblockflipper.backend.instrumentation.admin;

import com.skyblockflipper.backend.instrumentation.InstrumentationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;

@Component
public class AdminAccessGuard {

    private final InstrumentationProperties properties;

    /**
     * Create a new AdminAccessGuard using the provided instrumentation properties.
     *
     * @param properties instrumentation configuration used to determine admin access rules (local-only and token)
     */
    public AdminAccessGuard(InstrumentationProperties properties) {
        this.properties = properties;
    }

    /**
     * Enforces admin access restrictions for the incoming HTTP request.
     *
     * Checks whether admin endpoints are restricted to local-only access and, if an admin token is configured,
     * validates the request's "X-Admin-Token" header.
     *
     * @param request the incoming HTTP request; used to determine the remote address and to read the "X-Admin-Token" header
     * @throws ResponseStatusException with HTTP 403 if admin is configured as local-only and the request is not from a loopback address,
     *         or with HTTP 401 if an admin token is configured and the "X-Admin-Token" header is missing or does not match the configured token
     */
    public void validate(HttpServletRequest request) {
        if (properties.getAdmin().isLocalOnly() && !isLoopback(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin endpoint is local-only");
        }
        String configuredToken = properties.getAdmin().getToken();
        if (configuredToken != null && !configuredToken.isBlank()) {
            String provided = request.getHeader("X-Admin-Token");
            if (!configuredToken.equals(provided)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid admin token");
            }
        }
    }

    /**
     * Determines whether the given remote address refers to a loopback interface.
     *
     * Resolves the provided host or IP string and returns `true` if it maps to a loopback address; if resolution fails or an exception occurs, returns `false`.
     *
     * @param remoteAddr the remote host name or IP address to check
     * @return `true` if the address resolves to a loopback address, `false` otherwise
     */
    private boolean isLoopback(String remoteAddr) {
        try {
            return InetAddress.getByName(remoteAddr).isLoopbackAddress();
        } catch (Exception ignored) {
            return false;
        }
    }
}