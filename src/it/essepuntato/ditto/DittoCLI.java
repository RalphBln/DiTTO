package it.essepuntato.ditto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.TransformerException;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command
public class DittoCLI implements Runnable {

	@Option(names = {"-t", "--type"}, required = true)
	private String type;
	
	@Option(names = {"-f", "--diagram-file-path"}, required = true)
	private String diagramFilePathString;
	
	@Option(names = {"-i", "--ontology-iri"})
	private String ontologyIRI;
	
	@Option(names = {"-p", "--ontology-prefix"})
	private String ontologyPrefix;
	
	@Option(names = {"-l", "--look-across-for-labels"})
	private boolean lookAcrossForLabels;
	
	@Option(names = {"-m", "--main-ontology-only"})
	private boolean mainOntologyOnly;
	
	@Option(names = {"-v", "--version-iri-imported"})
	private boolean versionIRIimported;
	

	public static void main(String[] args) {
		CommandLine.run(new DittoCLI(), args);

	}

	@Override
	public void run() {
		String erMinimal = DittoCLI.class.getResource("/er-minimal.xsl").toString();
		String erIntermediate = DittoCLI.class.getResource("/er-intermediate.xsl").toString();
		String erMaximal = DittoCLI.class.getResource("/er-maximal.xsl").toString();
		String graffoo =  DittoCLI.class.getResource("/graffoo.xsl").toString();
		
		Ditto ditto = new Ditto(erMinimal, erIntermediate, erMaximal, graffoo);
		
		Path diagramFilePath = Paths.get(diagramFilePathString);
		if (!diagramFilePath.toFile().isFile()) {
			System.out.format("Diagram file %s does not exist.", diagramFilePathString);
			System.exit(1);
		}
		
		String result = null;
		
		try {
			String content = new String(Files.readAllBytes(diagramFilePath), StandardCharsets.UTF_8);
			
			result = ditto.applyXSLTTransformation(
					content, 
					ontologyIRI,
					ontologyPrefix,
					lookAcrossForLabels,
					mainOntologyOnly,
					versionIRIimported,
					ditto.getXSLT(type), type).trim();
		} catch (IOException e) {
			System.out.format("Error reading file %s: %s", diagramFilePathString, e.getMessage());
			System.exit(1);
		} catch (TransformerException e) {
			System.out.format("Error during transformation: %s", e.getMessage());
			System.exit(1);
		}

		FileOutputStream out = null;
		
		try {
			// TODO the following is copied and adapted from from DittoServlet. Check if this could be unified.
			if (type.equals("graffoo")) {
				String[] tempOntologies = result.split("\r\r\r");
				List<String> finalOntologies = new ArrayList<String>(); 
				for (String tempOntology : tempOntologies) {
					if (!tempOntology.replaceAll("\\s+", "").trim().equals("")) {
						finalOntologies.add(tempOntology);
					}
				}
				
				if (finalOntologies.size() > 1) {
					out = new FileOutputStream(new File("ontologies.zip"));
					ditto.createZipFile(finalOntologies, out);
					out.flush();
					out.close();
					
				} else {
					out = new FileOutputStream(new File("ontology.owl"));
					out.write(result.getBytes());
					out.flush();
					out.close();
				}
				
			} else {
				out = new FileOutputStream(new File("ontology.rdf"));
				out.write(result.getBytes());
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			System.out.format("Error writing result file %s", e.getMessage());
			System.exit(1);
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					System.out.format("Error closing result file %s", e.getMessage());
					System.exit(1);
				}
			}
		}
			
		
	}

}
