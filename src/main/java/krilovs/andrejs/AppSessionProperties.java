package krilovs.andrejs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.session")
public record AppSessionProperties(int inactivityTimeoutSeconds) {
    public long getTimeoutMillis() {
        return inactivityTimeoutSeconds * 1000L;
    }
}