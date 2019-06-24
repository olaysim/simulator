package dk.orda.overlaynetwork.statistics;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.text.Format;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatLogger {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final long WRITE_WHEN = 100000L;

    private Map<String, List<String>> lines;
    private ExecutorService executor;
    private Format df;
    private Format logdf;
    private long count = 0L;
    private String folder = "";
    private String virtualid = "";
    private StatConfiguration config;

    public StatLogger(StatConfiguration configuration) {
        this.config = configuration;
        lines = new HashMap<>();
        df = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneId.systemDefault()).toFormat();
        logdf = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()).toFormat();
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void addValue(Value value) {
        addValues(value.getGroup(), Collections.singletonList(value));
    }

    public void addValues(String group, List<Value> valuelst) {
        StringBuilder strb = new StringBuilder(valuelst.size());
        for (Value value : valuelst) {
            if (!lines.containsKey(group)) {
                lines.put(group, Collections.synchronizedList(new ArrayList<>()));
            }
            strb
                .append(value.getTimestamp()).append(";")
                .append(logdf.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getTimestamp()), ZoneId.systemDefault()))).append(";")
                .append(value.getName()).append(";");
            for (Object str : value.getValuesAsStringList()) {
                strb.append(str).append(";");
            }
            strb
                .append(value.getTestid()).append(";")
                .append(value.getCorrelation()).append(";")
                .append(value.getDescription()).append(";");
        }
        lines.get(group).add(strb.toString());
        count++;
        if (count > WRITE_WHEN) {
            executor.submit(this::write);
        }
    }

    public void addLines(String key, List<String> value) {
        if (!lines.containsKey(key)) {
            lines.put(key, new ArrayList<>());
        }
        lines.get(key).addAll(value);
    }

    public void write() {
        try {
//            Path path1 = Paths.get("data");
            Path path2 = Paths.get("data", folder, virtualid);
            Files.createDirectories(path2);
//            if (!Files.exists(path1)) Files.createDirectory(path1);
//            if (!Files.exists(path2)) Files.createDirectory(path2);
        } catch (IOException e) {
            log.error("unable to create data folders", e);
            System.exit(-1);
        }

        for (Map.Entry<String, List<String>> entry : lines.entrySet()) {
            int size = 0;
            synchronized (entry.getValue()) {
                size = entry.getValue().size();
            }

            Path path = Paths.get("data", folder, virtualid, entry.getKey()+ "_" + df.format(Instant.now()) + ".csv");
            OpenOption[] openOptions = new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            List<String> remove = new ArrayList<>();
            try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"), openOptions)) {

                for (int i = 0; i < size; i++) {
                    String s = entry.getValue().get(i);
                    writer.write(s + '\n');
                    remove.add(s);
                }
            } catch (IOException e) {
                log.error("Unable to write experiment data to file", e);
            }

            for (String s : remove) {
                entry.getValue().remove(s);
            }

        }
    }

    public void initialize() {
        initialize(null);
    }

    public void initialize(String folder) {
        if (folder != null) {
            this.folder = folder;
        }
        lines = new HashMap<>();
    }

    public void setVirtualid(String virtualid) {
        this.virtualid = virtualid;
    }

    public StatConfiguration getConfig() {
        return config;
    }

    public void setConfig(StatConfiguration config) {
        this.config = config;
    }

    public Map<String, List<String>> getLines() {
        return lines;
    }
}
