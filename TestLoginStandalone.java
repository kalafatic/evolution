import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.repository.UserRepository;
import eu.kalafatic.evolution.servers.repository.SessionRepository;
import eu.kalafatic.evolution.servers.service.AuthService;
import java.util.Optional;

public class TestLoginStandalone {
    public static void main(String[] args) throws Exception {
        DatabaseManager db = new DatabaseManager();
        UserRepository userRepo = new UserRepository(db);
        SessionRepository sessRepo = new SessionRepository(db);
        AuthService auth = new AuthService(userRepo, sessRepo);

        Optional<String> token = auth.login("admin", "admin", "127.0.0.1");
        if (token.isPresent()) {
            System.out.println("Login SUCCESS: " + token.get());
        } else {
            System.out.println("Login FAILED");
            // Check if user even exists
            userRepo.findByUsername("admin").ifPresentOrElse(
                u -> System.out.println("User exists but password match failed. Hash: " + u.getPasswordHash()),
                () -> System.out.println("User 'admin' NOT found in DB")
            );
        }
    }
}
