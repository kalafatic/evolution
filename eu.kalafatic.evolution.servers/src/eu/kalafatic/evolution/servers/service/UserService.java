package eu.kalafatic.evolution.servers.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import eu.kalafatic.evolution.servers.model.User;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.security.BCryptUtils;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String password, String role) throws SQLException {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(BCryptUtils.hashPassword(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }

    public Optional<User> getUserById(Long id) throws SQLException {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) throws SQLException {
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAll();
    }

    public void updateUser(User user) throws SQLException {
        userRepository.update(user);
    }

    public void deleteUser(Long id) throws SQLException {
        userRepository.delete(id);
    }
}
