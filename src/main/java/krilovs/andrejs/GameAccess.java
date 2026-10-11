package krilovs.andrejs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.Optional;

public enum GameAccess {
    CHESS("chess"),
    CHECKERS("checkers");

    private final String path;
    private final String allowedAttribute;
    private final String activeAttribute;

    GameAccess(String path) {
        this.path = path;
        this.allowedAttribute = name() + "_ALLOWED";
        this.activeAttribute = name() + "_ACTIVE";
    }

    public String indexForward() {
        return "forward:/" + path + "/index.html";
    }

    public void allow(HttpSession session) {
        session.setAttribute(allowedAttribute, true);
    }

    public void activate(HttpSession session) {
        session.removeAttribute(allowedAttribute);
        session.setAttribute(activeAttribute, true);
    }

    public void exit(HttpSession session) {
        session.removeAttribute(allowedAttribute);
        session.removeAttribute(activeAttribute);
    }

    public boolean isAllowedOrActive(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(allowedAttribute))
                || Boolean.TRUE.equals(session.getAttribute(activeAttribute));
    }

    public boolean matches(HttpServletRequest request) {
        var uri = request.getRequestURI();
        var basePath = "/" + path;

        return uri.equals(basePath) || uri.startsWith(basePath + "/");
    }

    public static Optional<GameAccess> fromPath(String path) {
        return Arrays.stream(values())
                .filter(game -> game.path.equalsIgnoreCase(path))
                .findFirst();
    }

    public static Optional<GameAccess> fromRequest(HttpServletRequest request) {
        return Arrays.stream(values())
                .filter(game -> game.matches(request))
                .findFirst();
    }

    public static String[] requestPatterns() {
        return Arrays.stream(values())
                .flatMap(game -> Arrays.stream(new String[]{
                        "/" + game.path,
                        "/" + game.path + "/",
                        "/" + game.path + "/**"
                }))
                .toArray(String[]::new);
    }
}