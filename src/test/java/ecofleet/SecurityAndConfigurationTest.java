package ecofleet;

import ecofleet.db.*;
import ecofleet.exception.*;
import ecofleet.service.PasswordHasher;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityAndConfigurationTest {
    @Test void saltedPasswordFormatRejectsMalformedAndLegacyHashes() {
        char[] password = "explainable-secure-password".toCharArray();
        String first = PasswordHasher.hash(password), second = PasswordHasher.hash(password);
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify(password, first));
        assertFalse(PasswordHasher.verify("wrong".toCharArray(), first));
        for (String invalid : List.of("plaintext", "sha256$abc", "pbkdf2-sha256$1$bad$bad",
                "pbkdf2-sha256$2147483647$bad$bad", "pbkdf2-sha256$x$bad$bad", "pbkdf2-sha256$600000$!$!")) {
            assertFalse(PasswordHasher.verify(password, invalid));
        }
        assertFalse(PasswordHasher.verify(null, first));
        String shortPasswordHash = PasswordHasher.hash("7chars!!".toCharArray());
        assertTrue(PasswordHasher.verify("7chars!!".toCharArray(), shortPasswordHash));
        assertThrows(InvalidRequestException.class, () -> PasswordHasher.hash(null));
        assertThrows(InvalidRequestException.class, () -> PasswordHasher.hash(new char[0]));
        Arrays.fill(password, '\0');
    }
    @Test void missingConfigDoesNotPrintSecrets() {
        String secret = "do-not-print-me";
        DatabaseOperationException error = assertThrows(DatabaseOperationException.class,
                () -> DatabaseConnection.fromEnvironment(Map.of("ECOROUTE_DB_PASSWORD", secret)));
        assertFalse(error.getMessage().contains(secret));
        assertTrue(error.getMessage().contains("ECOROUTE_DB_URL"));
    }
    @Test void integrationGuardRejectsAppDatabaseAndMissingAcknowledgement() {
        assertThrows(IllegalStateException.class, () -> MySqlBackendIT.testProvider(Map.of()));
        Map<String, String> env = new HashMap<>(Map.of("ECOROUTE_TEST_DB_URL", "jdbc:mysql://localhost/ecofleet",
                "ECOROUTE_TEST_DB_USER", "test", "ECOROUTE_TEST_DB_PASSWORD", "secret", "ECOROUTE_TEST_DB_DISPOSABLE", "YES"));
        assertThrows(IllegalStateException.class, () -> MySqlBackendIT.testProvider(env));
        env.put("ECOROUTE_TEST_DB_URL", "jdbc:mysql://localhost/ecofleet_member2_test_example");
        assertNotNull(MySqlBackendIT.testProvider(env));
        env.put("ECOROUTE_DB_URL", "jdbc:mysql://localhost/ecofleet_member2_test_example");
        assertThrows(IllegalStateException.class, () -> MySqlBackendIT.testProvider(env));
    }
}
