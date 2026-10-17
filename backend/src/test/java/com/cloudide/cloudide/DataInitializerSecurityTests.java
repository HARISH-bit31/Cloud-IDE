package com.cloudide.cloudide;

import com.cloudide.cloudide.config.JwtProperties;
import com.cloudide.cloudide.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:initsecuritytestdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "execution.worker.secret=test_worker_secret_12345"
})
class DataInitializerSecurityTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("In default/non-dev configuration, DataInitializer does not seed demo user")
    void testDemoUserNotSeededByDefault() {
        assertFalse(userRepository.existsByEmail("alex.developer@cloud-ide.io"),
                "Predictable demo user alex.developer@cloud-ide.io must not be seeded by default");
    }

    @Test
    @DisplayName("JwtProperties throws IllegalStateException when secret is null or blank")
    void testJwtPropertiesFailsFastOnMissingSecret() {
        JwtProperties props = new JwtProperties();
        props.setSecret(null);
        assertThrows(IllegalStateException.class, props::validate, "Should fail when secret is null");

        props.setSecret("");
        assertThrows(IllegalStateException.class, props::validate, "Should fail when secret is empty");

        props.setSecret("   ");
        assertThrows(IllegalStateException.class, props::validate, "Should fail when secret is whitespace");
    }

    @Test
    @DisplayName("JwtProperties validates successfully when valid secret is provided")
    void testJwtPropertiesPassesWithValidSecret() {
        JwtProperties props = new JwtProperties();
        props.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        assertDoesNotThrow(props::validate, "Should pass when valid secret is configured");
    }
}
