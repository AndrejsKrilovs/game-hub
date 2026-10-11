package krilovs.andrejs;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@SpringBootApplication
@RequiredArgsConstructor
@EnableConfigurationProperties(AppSessionProperties.class)
public class Application {
    private final AppSessionProperties sessionProperties;

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @PostMapping("/games/{game}/start")
    public ResponseEntity<Void> startGame(@PathVariable String game, HttpSession session) {
        var gameAccess = GameAccess.fromPath(game);
        if (gameAccess.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        gameAccess.get().allow(session);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/{game:chess|checkers}", "/{game:chess|checkers}/"})
    public String game(@PathVariable String game,
                       HttpSession session,
                       @RequestParam(required = false) String gameSessionId) {
        var gameAccess = GameAccess.fromPath(game).orElseThrow();
        var authenticated = isAuthenticated();
        var allowedOrActive = gameAccess.isAllowedOrActive(session);
        var inviteLink = gameSessionId != null && !gameSessionId.isBlank();

        if (!authenticated || (!allowedOrActive && !inviteLink)) {
            return "redirect:/";
        }

        gameAccess.activate(session);
        return gameAccess.indexForward();
    }

    @PostMapping("/games/{game}/exit")
    public ResponseEntity<Void> exitGame(@PathVariable String game, HttpSession session) {
        var gameAccess = GameAccess.fromPath(game);
        if (gameAccess.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        gameAccess.get().exit(session);
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(Map.of(
                    "username", auth.getName(),
                    "inactivityTimeoutMs", sessionProperties.getTimeoutMillis()
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private static boolean isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}