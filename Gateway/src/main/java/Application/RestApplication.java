package Application;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import java.util.HashSet;

@ApplicationPath("/api")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(Service.GatewayController.class);
        classes.add(Service.TestController.class); // Aggiungi questo
        return classes;
    }
}