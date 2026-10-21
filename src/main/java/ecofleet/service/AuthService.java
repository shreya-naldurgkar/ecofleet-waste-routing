package ecofleet.service;

import ecofleet.dao.UserDAO;
import ecofleet.db.Database;
import ecofleet.model.User;
import ecofleet.exception.InvalidRequestException;
import java.util.List;
import static ecofleet.db.DatabaseContract.*;

public final class AuthService {
    private final Database database;
    // Used for unknown usernames too, so password derivation still happens.
    private final String dummyHash = PasswordHasher.hash("unused-dummy-password".toCharArray());
    public AuthService(Database database) { this.database = database; }

    /** Caller should clear its char[] after this call. */
    public UserSession login(String username, char[] password) {
        Validation.text(username, "Username");
        User user = database.read(c -> new UserDAO(c).findByUsername(username).orElse(null));
        boolean valid = PasswordHasher.verify(password, user == null ? dummyHash : user.getPasswordHash());
        if (!valid || user == null || !user.isActive() || user.getRole() == null || !ROLES.contains(user.getRole())
                || (GENERATOR.equals(user.getRole()) ? user.getGeneratorId() == null : user.getGeneratorId() != null)) {
            throw new InvalidRequestException("Invalid credentials or inactive account.");
        }
        return new UserSession(user.getUserId(), user.getUsername(), user.getRole(), user.getGeneratorId());
    }

    public List<User> findAll(UserSession session) {
        return database.read(c -> {
            Access.admin(c, session);
            return new UserDAO(c).findAll();
        });
    }

    public long createUser(UserSession session, User user, char[] password) {
        Validation.require(user != null, "User is required.");
        Validation.text(user.getUsername(), "Username");
        Validation.require(user.getRole() != null && ROLES.contains(user.getRole()), "Invalid user role.");
        if (GENERATOR.equals(user.getRole())) {
            Validation.require(user.getGeneratorId() != null, "Generator user requires a generator ID.");
        } else {
            Validation.require(user.getGeneratorId() == null, "Staff user must not have a generator ID.");
        }
        Validation.require(password != null && password.length > 0, "Password is required.");
        String hash = PasswordHasher.hash(password);
        User record = new User(null, user.getUsername(), hash, user.getRole(), user.getGeneratorId(), true, java.time.LocalDateTime.now());
        return database.transaction(c -> {
            Access.admin(c, session);
            return new UserDAO(c).insert(record);
        });
    }

    public void deactivate(UserSession session, long userId) {
        Validation.id(userId, "User");
        database.transaction(c -> {
            Access.admin(c, session);
            new UserDAO(c).updateActive(userId, false);
            return null;
        });
    }
}
