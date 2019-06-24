package dk.orda.seedserver.util;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class XmlSerializer {
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    /**
     * Converts Object to String
     * @param graph
     * @return
     * @throws IOException
     */
    public String objectToXmlString(Object graph) throws IOException {
        StringResult result = new StringResult();
        marshaller.marshal(graph, result);
        return result.toString();
    }

    /**
     * Converts Object to XML file
     * @param file
     * @param graph
     * @throws IOException
     */
    public void objectToXml(String file, Object graph) throws IOException {
        File f = new File(file);
        if (!f.exists()) throw new IOException("File not found! ('" + file + "')");
        FileOutputStream fos = new FileOutputStream(f);
        marshaller.marshal(graph, new StreamResult(fos));
        fos.close();
    }

    /**
     * Converts String (XML) to Java Object
     * @param xml
     * @return
     * @throws IOException
     */
    public Object xmlStringToObject(String xml) throws IOException {
        return unmarshaller.unmarshal(new StringSource(xml));
    }

    /**
     * Converts XML to Java Object
     * @param file
     * @return
     * @throws IOException
     */
    public Object xmlToObject(String file) throws IOException {
        File f = new File(file);
        if (!f.exists()) throw new IOException("File not found! ('" + file + "')");
        FileInputStream fis = new FileInputStream(f);
        Object obj = unmarshaller.unmarshal(new StreamSource(fis));
        fis.close();
        return obj;
    }
}
