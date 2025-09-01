package Application;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api") // il prefisso URL per le tue API REST
public class RestApplication extends Application {
    // non serve altro, così WildFly sa che deve abilitare i REST endpoint in questo package
}
