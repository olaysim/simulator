package dk.orda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class App {
    //private static final Logger log = LoggerFactory.getLogger(App.class);

    static {
        System.setProperty("java.net.preferIPv4Stack" , "true");
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = SpringApplication.run(App.class, args);
    }
}
