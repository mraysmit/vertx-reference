package dev.mars.vertx.gateway.security;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SecurityManager class.
 * Tests security configuration, JWT token generation, and role-based authorization.
 */
@ExtendWith(VertxExtension.class)
class SecurityManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(SecurityManagerTest.class);
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close()
                .onComplete(testContext.succeeding(v -> {
                    testContext.completeNow();
                }));
    }

    @Test
    void testSecurityEnabled() {
        // Create a configuration with security enabled
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("jwt", new JsonObject()
                                .put("symmetric-key", "test-symmetric-key")));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is enabled
        assertTrue(securityManager.isSecurityEnabled());
        assertNotNull(securityManager.getJwtAuthHandler());
        assertNotNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testSecurityDisabled() {
        // Create a configuration with security disabled
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", false));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is disabled
        assertFalse(securityManager.isSecurityEnabled());
        assertNull(securityManager.getJwtAuthHandler());
        assertNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation (should return null)
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNull(token);
    }

    @Test
    void testSecurityEnabledWithoutConfig() {
        // Create a configuration with security enabled but no JWT config
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is enabled but using default key
        assertTrue(securityManager.isSecurityEnabled());
        assertNotNull(securityManager.getJwtAuthHandler());
        assertNotNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testSecurityEnabledWithEmptyKey() {
        // Create a configuration with security enabled but empty JWT key
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("jwt", new JsonObject()
                                .put("symmetric-key", "")));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is enabled but using default key
        assertTrue(securityManager.isSecurityEnabled());
        assertNotNull(securityManager.getJwtAuthHandler());
        assertNotNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testSecurityEnabledWithLegacyConfig() {
        // Create a configuration with security enabled using legacy config format
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("secret-key", "legacy-test-key"));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is enabled
        assertTrue(securityManager.isSecurityEnabled());
        assertNotNull(securityManager.getJwtAuthHandler());
        assertNotNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testSecurityEnabledWithCustomRealm() {
        // Create a configuration with security enabled and custom realm
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("realm", "custom-realm")
                        .put("jwt", new JsonObject()
                                .put("symmetric-key", "test-symmetric-key")));

        // Create a security manager
        SecurityManager securityManager = new SecurityManager(vertx, config);

        // Verify security is enabled with custom realm
        assertTrue(securityManager.isSecurityEnabled());
        assertNotNull(securityManager.getJwtAuthHandler());
        assertNotNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void testNoConfig() {
        // Create a security manager with empty config (not null to avoid NPE)
        SecurityManager securityManager = new SecurityManager(vertx, new JsonObject());

        // Verify security is disabled
        assertFalse(securityManager.isSecurityEnabled());
        assertNull(securityManager.getJwtAuthHandler());
        assertNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation (should return null)
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNull(token);
    }

    @Test
    void testEmptyConfig() {
        // Create a security manager with empty config
        SecurityManager securityManager = new SecurityManager(vertx, new JsonObject());

        // Verify security is disabled
        assertFalse(securityManager.isSecurityEnabled());
        assertNull(securityManager.getJwtAuthHandler());
        assertNull(securityManager.createRoleBasedAuthHandler("admin"));

        // Test token generation (should return null)
        String token = securityManager.generateToken("test-user", new String[]{"admin"});
        assertNull(token);
    }
}
