package it.essepuntato.ditto;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DittoServlet
 */
public class DittoServlet extends HttpServlet {
	private static final long serialVersionUID = -6136725632087248125L;
	private int maxTentative = 3;
	
	private Ditto ditto;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DittoServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		ServletOutputStream out = null;
		
		SourceExtractor extractor = new SourceExtractor();
		extractor.addMimeTypes(MimeType.mimeTypes);
		
		for (int i = 0; i < maxTentative; i++) {
			try {
				String stringURL = request.getParameter("diagram-url");
				
				URL diagramURL = new URL(stringURL);
				String content = extractor.exec(diagramURL);
				String type = request.getParameter("type");
				
				content = ditto.applyXSLTTransformation(
						content, 
						request.getParameter("ontology-url"),
						request.getParameter("ontology-prefix"),
						new Boolean(request.getParameter("look-across-for-labels")),
						new Boolean(request.getParameter("main-ontology-only")),
						new Boolean(request.getParameter("version-iri-imported")),
						ditto.getXSLT(type), type).trim();
				
				if (type.equals("graffoo")) {
					String[] tempOntologies = content.split("\r\r\r");
					List<String> finalOntologies = new ArrayList<String>(); 
					for (String tempOntology : tempOntologies) {
						if (!tempOntology.replaceAll("\\s+", "").trim().equals("")) {
							finalOntologies.add(tempOntology);
						}
					}
					
					if (finalOntologies.size() > 1) {
						response.setContentType("application/zip"); /* ZIP file in case of multiple ontologies */
						response.setHeader("content-disposition", "attachment; filename=\"ontologies.zip\"");
						out = response.getOutputStream();
						ditto.createZipFile(finalOntologies, out);
						
					} else {
						response.setContentType("text/plain"); /* Manchester Syntax */
						response.setHeader("content-disposition", "attachment; filename=\"ontology.owl\"");
						out = response.getOutputStream();
						out.write(content.getBytes());
					}
					
				} else {
					response.setContentType("application/rdf+xml");  /* RDF/XML */
					response.setHeader("content-disposition", "attachment; filename=\"ontology.owl\"");
					out = response.getOutputStream();
					out.write(content.getBytes());
				}
				
				out.flush();
				i = maxTentative;
			} catch (Exception e) {
				if (i + 1 == maxTentative) {
					response.setContentType("text/html");
					//out = response.getWriter();
					out.println(getErrorPage(e));
				}
			}
		}
	}

	private String getErrorPage(Exception e) {
		return "DiTTO error\nReason:\n\t" + e.getMessage();
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String erMinimal = getServletContext().getRealPath("er-minimal.xsl");
		String erIntermediate = getServletContext().getRealPath("er-intermediate.xsl");
		String erMaximal = getServletContext().getRealPath("er-maximal.xsl");
		String graffoo =  getServletContext().getRealPath("graffoo.xsl");
		ditto = new Ditto(erMinimal, erIntermediate, erMaximal, graffoo);
	}
	
	@Override
	public void destroy() {
		ditto = null;
		super.destroy();
	}
}
