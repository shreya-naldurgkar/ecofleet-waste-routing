package ecofleet.service;

import ecofleet.dao.UserDAO;
import ecofleet.model.User;
import ecofleet.exception.InvalidRequestException;
import java.sql.Connection;
import java.util.Objects;
import static ecofleet.db.DatabaseContract.*;

final class Access {
    private Access() {}
    static User current(Connection connection, UserSession session) {
        Validation.require(session != null, "Authentication is required.");
        User user = new UserDAO(connection).findById(session.getUserId())
                .orElseThrow(() -> new InvalidRequestException("Session user no longer exists."));
        Validation.require(user.isActive() && user.getRole() != null && ROLES.contains(user.getRole()), "User is inactive or has an invalid role.");
        Validation.require(user.getRole().equals(session.getRole())
                && Objects.equals(user.getGeneratorId(), session.getGeneratorId()), "User permissions changed; sign in again.");
        Validation.require(GENERATOR.equals(user.getRole()) ? user.getGeneratorId() != null : user.getGeneratorId() == null,
                "User role and generator association are inconsistent.");
        return user;
    }
    static void staff(Connection connection, UserSession session) {
        User user = current(connection, session);
        Validation.require(ADMIN.equals(user.getRole()) || OPERATOR.equals(user.getRole()) || DRIVER.equals(user.getRole()),
                "Staff access is required.");
    }
    static void operatorOrAdmin(Connection connection, UserSession session) {
        User user = current(connection, session);
        Validation.require(ADMIN.equals(user.getRole()) || OPERATOR.equals(user.getRole()),
                "Operator or admin access is required.");
    }
    static void admin(Connection connection, UserSession session) {
        Validation.require(ADMIN.equals(current(connection, session).getRole()), "Admin access is required.");
    }
    static void generator(Connection connection, UserSession session, long generatorId) {
        User user = current(connection, session);
        Validation.require(!GENERATOR.equals(user.getRole()) || Objects.equals(user.getGeneratorId(), generatorId),
                "You cannot access another generator's requests.");
    }
    static void generatorOrAdmin(Connection connection, UserSession session, long generatorId) {
        User user = current(connection, session);
        Validation.require(GENERATOR.equals(user.getRole()) && Objects.equals(user.getGeneratorId(), generatorId)
                        || ADMIN.equals(user.getRole()),
                "Only the owning generator or an admin may access this request.");
    }
    static void driver(Connection connection, UserSession session) {
        User user = current(connection, session);
        Validation.require(DRIVER.equals(user.getRole()) || ADMIN.equals(user.getRole()), "Driver access is required.");
    }
}
