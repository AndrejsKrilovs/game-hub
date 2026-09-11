package krilovs.andrejs;

import jakarta.servlet.http.HttpSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import static krilovs.andrejs.SpringSecurityConfig.CHESS_ACTIVE;
import static krilovs.andrejs.SpringSecurityConfig.CHESS_ALLOWED;

@Controller
@SpringBootApplication
public class Application {
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @PostMapping("/games/chess/start")
    public ResponseEntity<Void> startChess(HttpSession session) {
        session.setAttribute(CHESS_ALLOWED, true);
        return ResponseEntity.ok().build();
    }

    @GetMapping({"/chess", "/chess/"})
    public String chess(HttpSession session) {
        boolean allowed = Boolean.TRUE.equals(session.getAttribute(CHESS_ALLOWED));
        boolean active = Boolean.TRUE.equals(session.getAttribute(CHESS_ACTIVE));

        if (!allowed && !active) {
            return "redirect:/";
        }

        session.removeAttribute(CHESS_ALLOWED);
        session.setAttribute(CHESS_ACTIVE, true);

        return "forward:/chess/index.html";
    }

    @PostMapping("/games/chess/exit")
    public ResponseEntity<Void> exitChess(HttpSession session) {
        session.removeAttribute(CHESS_ALLOWED);
        session.removeAttribute(CHESS_ACTIVE);
        return ResponseEntity.ok().build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}