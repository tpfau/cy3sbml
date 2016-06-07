package org.cy3sbml.miriam;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import javax.xml.stream.XMLStreamException;
import uk.ac.ebi.miriam.lib.MiriamLink;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.FunctionDefinition;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.ontology.Term;
import org.cy3sbml.util.AnnotationUtil;

/** 
 * Create the information for the selected NamedSBase.
 * Core information is parsed from the NamedSBase object,
 * with additional information like resources retrieved via
 * web services (MIRIAM).
 * Here the HTML information string is created which is displayed
 * on selection of SBML objects in the graph.
 * 
 * TODO: cached MIRIAM information (faster access & less workload on MIRIAM)
 * 
 * TODO: refactor SBML HTML information completely 
 * 		(https://github.com/matthiaskoenig/cy3sbml/milestones/0.1.8)
 */
public class SBaseInfoFactory {
	// private static final Logger logger = LoggerFactory.getLogger(NamedSBaseInfoFactory.class);
	
	private MiriamLink link;
	private SBase sbase;
	private String info = ""; 
	
	public SBaseInfoFactory(Object obj){		
		sbase = (SBase) obj;
		link = MiriamWebservice.getMiriamLink();
	}
	
	public String getInfo() {
		return info;
	}
	
	public void cacheMiriamInformation(){
		for (CVTerm term : sbase.getCVTerms()){
			for (String rURI : term.getResources()){
				MiriamResourceInfo.getLocationsFromURI(link, rURI);
			}
		}
	}
	
	/** Parse and create information for the current sbmlObject. */
	public void createInfo() throws XMLStreamException {
		if (sbase == null){
			// unsupported classes
			return;
		}
		// SBML information
		info = createHeader(sbase);
		info += createSBOInfo(sbase);
		info += createSBaseInfo(sbase);
  		
  		// CVterm annotations (MIRIAM action)
		List<CVTerm> terms = sbase.getCVTerms();
  		info += getCVTermsString(terms);
  		
  		// Non-RDF annotation
  		if (sbase.isSetAnnotation()){
  			Annotation annotation = sbase.getAnnotation();
  			String text = annotation.getNonRDFannotationAsString();
  			text = StringEscapeUtils.escapeHtml(text);
  			info += info += String.format("<p><span color=\"gray\">%s</span></p>", text);
  		}
  		
  		// notes
  		// !!! have to be set at the end due to the html content which
  		// breaks the rest of the html.
  		if (sbase.isSetNotes()){
  			String notes = sbase.getNotesString();
  	  		if (!notes.equals("") && notes != null ){
  	  			info += String.format("<p>%s</p>", notes);
  	  		}	
  		}
	}
	
	/**
	 * Creates the header information.
	 * Displays the class and id, name if existing.
	 */
	private String createHeader(SBase item){
		String className = getUnqualifiedClassName(item);
		String header = String.format("<h2><span color=\"gray\">%s</span></h2>",
									  className);
		// if NamedSBase get additional information
		if (NamedSBase.class.isAssignableFrom(item.getClass())){
			NamedSBase nsb = (NamedSBase) item;
			String template = "<h2><span color=\"gray\">%s</span> : %s</h2>";
			String name = nsb.getId();
			if (nsb.isSetName()){
				name =  String.format("%s (%s)", nsb.getId(), nsb.getName());
			}
			header = String.format(template, className, name);
		}
		return header; 
	}
	
	/** 
	 * Returns unqualified class name of a given object. 
	 */
	private static String getUnqualifiedClassName(Object obj){
		String name = obj.getClass().getName();
		if (name.lastIndexOf('.') > 0) {
		    name = name.substring(name.lastIndexOf('.')+1);
		}
		// The $ can be converted to a .
		name = name.replace('$', '.');  
		return name;
	}
	
	private String createSBOInfo(SBase item){
		String text = "";
		if (item.isSetSBOTerm()){
			String sboTermId = item.getSBOTermID();
			// Aliases are a construct introduced and used by CellDesigner.
			// String sboAlias = SBO.convertSBO2Alias(item.getSBOTerm());
			Term sboTerm = SBO.getTerm(sboTermId);
			String definition = parseSBOTermDefinition(sboTerm.getDefinition());
			
			// sboTerm.getSynonyms();
  			CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "urn:miriam:biomodels.sbo:" + sboTermId);
  			text += String.format("<b> %s <span color=\"green\">%s</span></b><br>", sboTerm.getName(), sboTermId);
  			text += definition + "<br>";
  			for (String rURI : term.getResources()){
  				text += MiriamResourceInfo.getInfoFromURI(link, rURI);
  			}
  			text += "<hr>";
  		}
		return text;
	}
	
	private String parseSBOTermDefinition(String definition){
		String[] tokens = definition.split("\"");
		String[] defTokens = (String []) ArrayUtils.subarray(tokens, 1, tokens.length-1);
		return StringUtils.join(defTokens, "\"");
	}
	
	
	/** Get string HTML representation of the CVTerm information. */
	private String getCVTermsString(List<CVTerm> cvterms){
		String text = "<hr>";
		if (cvterms.size() > 0){
			for (CVTerm term : cvterms){
				if (term.isModelQualifier()){
					text += String.format("<p><b>%s : %s</b><br>", term.getQualifierType(), term.getModelQualifierType());	
				} else if (term.isBiologicalQualifier()){
					text += String.format("<p><b>%s : %s</b><br>", term.getQualifierType(), term.getBiologicalQualifierType());
				}
				
				Map<String, String> map = null;
				for (String rURI : term.getResources()){
					map = AnnotationUtil.getIdCollectionMapForURI(rURI);
					text += String.format("<span color=\"red\">%s</span> (%s)<br>", map.get("id"), map.get("collection"));
					text += MiriamResourceInfo.getInfoFromURI(link, rURI);
				}
				text += "</p>";
			}
			text += "<hr>";
		}
  		return text;
	}
		
	/** 
	 * The general NamedSBase information is created in the 
	 * header. Here the Class specific attribute information is generated.
	 */
	private String createSBaseInfo(SBase item){
		String text = "";
		// Model
		if (item instanceof Model){
			Model model = (Model) item;
			String template = "<b>L%sV%s</b> <a href=\"http://sbml-file\">(SBML file)</a>"; 
  			text = String.format(template, model.getLevel(), model.getVersion());
		}
		
		// Compartment
		else if (item instanceof Compartment){
			Compartment compartment = (Compartment) item;
			String template = "<b>spatialDimensions</b>: %s<br />" +
							  "<b>size</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s";
			String dimensions = "?";
			String size = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			if (compartment.isSetSize()){
				size = ((Double)compartment.getSize()).toString();
			}
			if (compartment.isSetUnits()){
				units = compartment.getUnits();
			}
			if (compartment.isSetSpatialDimensions()){
				dimensions = ((Double) compartment.getSpatialDimensions()).toString();
			}
			if (compartment.isSetConstant()){
				constant = booleanHTML(compartment.getConstant());
			}
			text = String.format(template, dimensions, size, units, constant); 
		}
		// Parameter
		else if (item instanceof Parameter){
			Parameter parameter = (Parameter) item;
			String template = "<b>value</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s";
			String value = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			if (parameter.isSetValue()){
				value = ((Double) parameter.getValue()).toString();
			}
			if (parameter.isSetUnits()){
				units = parameter.getUnits();
			}
			if (parameter.isSetConstant()){
				constant = booleanHTML(parameter.getConstant());
			}
			text = String.format(template, value, units, constant); 
		}
		
		// LocalParameter
		else if (item instanceof LocalParameter){
			LocalParameter parameter = (LocalParameter) item;
			String template = "<b>value</b>: %s [%s]";
			String value = noneHTML();
			String units = noneHTML();
			if (parameter.isSetValue()){
				value = ((Double) parameter.getValue()).toString();
			}
			if (parameter.isSetUnits()){
				units = parameter.getUnits();
			}
			text = String.format(template, value, units); 
		}
		
		// Species
		else if (item instanceof Species){
			Species species = (Species) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>value</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s<br />" +
							  "<b>boundaryCondition</b>: %s";
			String compartment = noneHTML();
			String value = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			String boundaryCondition = noneHTML();
			
			if (species.isSetCompartment()){
				compartment = species.getCompartment().toString();
			}
			if (species.isSetValue()){
				value = ((Double) species.getValue()).toString();
			}
			UnitDefinition udef = species.getDerivedUnitDefinition();
			if (udef != null){
				units = udef.toString();
			}
			if (species.isSetConstant()){
				constant = booleanHTML(species.isConstant());
			}
			if (species.isSetBoundaryCondition()){
				boundaryCondition = booleanHTML(species.getBoundaryCondition());
			}
			text = String.format(template, compartment, value, units, constant, boundaryCondition); 
		}
		
		// Reaction
		else if (item instanceof Reaction){
			Reaction reaction = (Reaction) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>reversible</b>: %s<br />" +
							  "<b>fast</b>: %s<br />" +
							  "<b>kineticLaw</b>: %s<br />" +
							  "<b>units</b>: [%s]";
			String compartment = noneHTML();
			String reversible = noneHTML();
			String fast = noneHTML();
			String kineticLaw = noneHTML();
			String units = noneHTML();
			
			if (reaction.isSetCompartment()){
				compartment = reaction.getCompartment().toString();
			}
			if (reaction.isSetReversible()){
				reversible = booleanHTML(reaction.getReversible());
			}
			if (reaction.isSetFast()){
				fast = booleanHTML(reaction.getFast());
			}
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				if (law.isSetMath()){
					kineticLaw = law.getMath().toFormula();	
				}
			}
			UnitDefinition udef = reaction.getDerivedUnitDefinition();
			if (udef != null){
				units = udef.toString();
			}
			text = String.format(template, compartment, reversible, fast, kineticLaw, units); 
		}
		// KineticLaw
		else if (item instanceof KineticLaw){
			KineticLaw law = (KineticLaw) item;
			String template = "<b>kineticLaw</b>: %s";
			String kineticLaw = noneHTML();
			if (law.isSetMath()){
				kineticLaw = law.getMath().toFormula();	
			}
			text = String.format(template, kineticLaw);
		}
		
		// QualitativeSpecies
		else if (item instanceof QualitativeSpecies){
			QualitativeSpecies species = (QualitativeSpecies) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>initial/max level</b>: %s/%s<br />" +
							  "<b>constant</b>: %s";
			String compartment = noneHTML();
			String initialLevel = noneHTML();
			String maxLevel = noneHTML();
			String constant = noneHTML();
			
			if (species.isSetCompartment()){
				compartment = species.getCompartment().toString();
			}
			if (species.isSetInitialLevel()){
				initialLevel = ((Integer) species.getInitialLevel()).toString();
			}
			if (species.isSetMaxLevel()){
				maxLevel = ((Integer) species.getMaxLevel()).toString();
			}
			if (species.isSetConstant()){
				constant = booleanHTML(species.getConstant());
			}
			text = String.format(template, compartment, initialLevel, maxLevel, constant); 
		}
		// Transition
		else if (item instanceof Transition){
			
		}
		// GeneProduct
		else if (item instanceof GeneProduct){
			
		}
		// FunctionDefinition
		else if (item instanceof FunctionDefinition){
			FunctionDefinition fd = (FunctionDefinition) item;
			String template = "<b>kineticLaw</b>: %s";
			String math = noneHTML();
			if (fd.isSetMath()){
				math = fd.getMath().toFormula();
			}
			text = String.format(template, math);
		}
		
		// comp:Port
		else if (item instanceof Port){
			Port port = (Port) item;
			String template = "<b>portRef</b>: %s <br />" +
							  "<b>idRef</b>: %s <br />" +
							  "<b>unitRef</b>: %s <br />" +
							  "<b>metaIdRef</b>: %s";
			String portRef = noneHTML();
			String idRef = noneHTML();
			String unitRef = noneHTML();
			String metaIdRef = noneHTML();
			if (port.isSetPortRef()){
				portRef = port.getPortRef();
			}
			if (port.isSetIdRef()){
				idRef = port.getIdRef();
			}
			if (port.isSetUnitRef()){
				unitRef = port.getUnitRef();
			}
			if (port.isSetMetaIdRef()){
				metaIdRef = port.getMetaIdRef();
			}
			text = String.format(template, portRef, idRef, unitRef, metaIdRef); 
		}
		return text;
	}
	
	private String trueHTML(){
		return "<img src=\"images/true.gif\" alt=\"true\" height=\"15\" width=\"15\"></img>";
	}
	private String falseHTML(){
		return "<img src=\"images/false.gif\" alt=\"false\" height=\"15\" width=\"15\"></img>";
	}
	private String noneHTML(){
		return "<img src=\"images/none.gif\" alt=\"none\" height=\"15\" width=\"15\"></img>";
	}
	
	private String booleanHTML(boolean b){
		if (b == true){
			return trueHTML();
		} else {
			return falseHTML();
		}	
	}
	
	
	
	
	/** Get additional image information for the database and identifier.
	 * TODO: This has to be done offline and in the background (images have to be cashed) !
	 * TODO: Create background database of information.  
	 */
	/*
	@Deprecated
	private String getAdditionalInformation(String r){
		Map<String, String> map = getMapForURI(r);
		String text = "";
		String id = map.get("id");
		String key = map.get("key");
		String[] keyitems = key.split(":");
		String item = keyitems[keyitems.length-1];
		
		// Add chebi info
		if (item.equals("obo.chebi")){
			try{
				String[] tmps = id.split("%3A");
				String cid = tmps[tmps.length-1];
				text += "<img src=\"http://www.ebi.ac.uk/chebi/displayImage.do?defaultImage=true&imageIndex=0&chebiId="+cid+"&dimensions=160\"></img>";
			} catch (Exception e){
				//e.printStackTrace();
				logger.warn("obo.chebi image not available");
			}
		
		// Add kegg info
		}else if (item.equals("kegg.compound")){
			try{
				String imgsrc = 
					NamedSBaseInfoFactory.class.getClassLoader().getResource("http://www.genome.jp/Fig/compound/"+id+".gif").toString();
				text += "<img src=\""+imgsrc+"\"></img>";
			} catch (Exception e){
				//e.printStackTrace();
				logger.warn("kegg.compound image not available");
			}
		
		// TODO resize image and use KEGG
		// Uncomment for reactions, but problems with large images 
//		}else if (item.equals("kegg.reaction")){
//			try{
//				String imgsrc = 
//					BioModelText.class.getClassLoader().getResource("http://www.genome.jp/Fig/reaction/"+id+".gif").toString();
//				text += "<img src=\""+imgsrc+"\"></img>";
//			} catch (Exception e){
//				//e.printStackTrace();
//				System.out.println("CySBML -> kegg.reaction image not available");
//			}
//		}
			
			
		}
		return text;
	}
	*/
	
}
