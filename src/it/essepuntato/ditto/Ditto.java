package it.essepuntato.ditto;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Ditto {

    private String erMinimal;
    private String erIntermediate;
    private String erMaximal;
    private String graffoo;
    
    public Ditto(String erMinimal, String erIntermediate, String erMaximal, String graffoo) {
		super();
		this.erMinimal = erMinimal;
		this.erIntermediate = erIntermediate;
		this.erMaximal = erMaximal;
		this.graffoo = graffoo;
	}

	public ByteArrayOutputStream createZipFile(List<String> list, OutputStream out) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(out);
        for (int i = 0; i < list.size(); i++) {
            String ontology = list.get(i);
            InputStream is = new ByteArrayInputStream(ontology.getBytes());
            ZipEntry entry = new ZipEntry("ontology_" + (i+1) + ".owl");
            zip.putNextEntry(entry);
            int len;
            byte[] buf = new byte[1024];
            while ((len = is.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            zip.closeEntry();
            is.close();
        }
        zip.close();
        return b;
    }

    public String getXSLT(String parameter) {
        String result = erMaximal;

        if (parameter != null) {
            if (parameter.equals("er-minimal")) {
                result = erMinimal;
            } else if (parameter.equals("er-intermediate")) {
                result = erIntermediate;
            } else if (parameter.equals("er-maximal")) {
                result = erMaximal;
            } else if (parameter.equals("graffoo")) {
                result = graffoo;
            }
        }

        return result;
    }

    public String applyXSLTTransformation(
            String source, String ontologyURI, String ontologyPrefix,
            boolean lookAcrossForLabels, boolean mainOntologyOnly, boolean versionIRIImported,
            String xsltURL, String type)
            throws TransformerException {
        TransformerFactory tfactory = new net.sf.saxon.TransformerFactoryImpl();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Transformer transformer =
                tfactory.newTransformer(
                        new StreamSource(xsltURL));

        if (type.equals("graffoo")) {
            if (ontologyURI != null && !ontologyURI.equals("")) {
                transformer.setParameter("default-empty-prefix-param", ontologyURI + "/");
                transformer.setParameter("default-ontology-iri-param", ontologyURI);
            }
            transformer.setParameter("use-imported-ontology-version-iri-param", versionIRIImported);
            transformer.setParameter("generate-all-ontologies-param", !mainOntologyOnly);
        } else {
            transformer.setParameter("ontology-prefix", ontologyPrefix);
            transformer.setParameter("ontology-uri", ontologyURI);
            transformer.setParameter("look-across-for-labels", lookAcrossForLabels);
        }

        StreamSource inputSource = new StreamSource(new StringReader(source));

        transformer.transform(
                inputSource,
                new StreamResult(output));

        return output.toString();
    }
}
