package dk.orda;

import com.google.gson.Gson;
import dk.orda.seedserver.util.XmlSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {
//    private final Logger log = LoggerFactory.getLogger(this.getClass());

//    @Bean
//    @Lazy
//    public static ThreadPoolTaskExecutor taskExecutor() {
//        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
//        pool.setCorePoolSize(2);
//        pool.setMaxPoolSize(5);
//        pool.setWaitForTasksToCompleteOnShutdown(true);
//        return pool;
//    }

    @Bean
    public XmlSerializer xmlSerializer(Jaxb2Marshaller jaxb2Marshaller){
        XmlSerializer xmlSerializer = new XmlSerializer();
        xmlSerializer.setMarshaller(jaxb2Marshaller);
        xmlSerializer.setUnmarshaller(jaxb2Marshaller);
        return xmlSerializer;
    }
    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setPackagesToScan("dk.orda");
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("jaxb.formatted.output", true);
        jaxb2Marshaller.setMarshallerProperties(map);
        return jaxb2Marshaller;
    }

    @Bean
    @Lazy
    public Gson gson() {
        return new Gson();
    }
//
//    // currently just hardcoded filename... it's sufficient for now
//    @Bean
//    public Config config(XmlSerializer xmlSerializer) throws IOException {
//        return (Config) xmlSerializer.xmlToObject("config.conf");
//    }
}
