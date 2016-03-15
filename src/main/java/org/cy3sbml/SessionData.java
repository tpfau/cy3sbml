package org.cy3sbml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save cy3sbml data in session file and restore from session file.
 * 
 * Which data has to be saved and restored?
 * - SBMLManager (SBMLDocuments & network2sbml mapper)
 * TODO: write restore function, which create the mapping from the rest
 * - CofactorManager
 * 
 * TODO: look into serializing the key singleton classes which store
 * 			the cy3sbml information.
 */
public class SessionData implements SessionAboutToBeSavedListener, SessionLoadedListener {
	private static final Logger logger = LoggerFactory.getLogger(SessionData.class);
	// File names for serialization
	// private static final String DOCUMENT_MAP = "documentMap.ser";
	private static final String SBML2NETWORK_SERIALIZATION = "SBML2NetworkMapper.ser";
	private File directory;
	
	/**
	 * Session data is locally saved in given directory.
	 * Normally this is the CytoscapeConfiguration/cy3sbml directory.
	 */
	public SessionData(File dir){
		directory = dir;
	}
	
	// Save app state in a file
	public void handleEvent(SessionAboutToBeSavedEvent event){
		logger.info("SessionAboutToBeSaved Event: Save cy3sbml session state");
		
		// Files to save
		List<File> files = new LinkedList<File>();
		
		// get SBMLManager for serialization
		SBMLManager sbmlManager = SBMLManager.getInstance();
		SBML2NetworkMapper mapper = sbmlManager.getSBML2NetworkMapper();
		Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();
		
		// Save all the SBML files
		for (Long networkSuid : documentMap.keySet()){
			SBMLDocument doc = documentMap.get(networkSuid);
			SBMLWriter writer = new SBMLWriter();
				
			String modelId = "";
			Model model = doc.getModel();
			if ((model != null) && (model.isSetId())){
				modelId = model.getId();
			}
			
			File sbmlFile = new File(directory, modelId + ".xml");
			try {
				writer.write(doc, sbmlFile);
				files.add(sbmlFile);
			} catch (SBMLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (XMLStreamException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
				
		// Serialize
		logger.info("Serializing SBMl2NetworkMapper");
		try {
			File file = new File(directory, SBML2NETWORK_SERIALIZATION);
	        FileOutputStream fileOut;
			try {
				fileOut = new FileOutputStream(file.getAbsolutePath());
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
		        out.writeObject(mapper);
		        out.close();
		        fileOut.close();
		        files.add(file);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		// Write files in session file
		try {
			event.addAppFiles("cy3sbml", files);
			logger.info("Save SBML files for session.");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			
	}
	
	/**
	 * Restore app state from session files.
	 */
	public void handleEvent(SessionLoadedEvent event){
		// check if there is app file data
		if (event.getLoadedSession().getAppFileListMap() == null || event.getLoadedSession().getAppFileListMap().size() ==0){
	       return;
	    }       
		List<File> files = event.getLoadedSession().getAppFileListMap().get("cy3sbml");
		for (File f : files){
			logger.info("cy3sbml file in session:" + f.toString());
			logger.info(f.getName());

			// deserialize the documentMap
			if (SBML2NETWORK_SERIALIZATION.equals(f.getName())){
				logger.info("Deserialize documentMap");
			    File mapFile = new File(directory, SBML2NETWORK_SERIALIZATION);
				
			    InputStream inputStream;
			    ObjectInput input;
				try {
					inputStream = new FileInputStream(mapFile.getAbsolutePath());
					InputStream buffer = new BufferedInputStream(inputStream);
					input = new ObjectInputStream (buffer);
					
					// read the mapper
					SBML2NetworkMapper mapper = (SBML2NetworkMapper)input.readObject();
					// update the suids
					updateSUIDS(event.getLoadedSession(), mapper);
					
					
					SBMLManager sbmlManager = SBMLManager.getInstance();
					sbmlManager.setSBML2NetworkMapper(mapper);
					
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				} catch (ClassNotFoundException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}
						
			}
		}
    }
	
	/**
	 * Updates the changed suids in the data structure
	 * @param s
	 * @param m
	 */
	public void updateSUIDS(CySession s, SBML2NetworkMapper m){

		// Long newSUID = s.getObject(oldSUID, CyIdentifiable.class).getSUID();
		
		// documentMap
		Map<Long, SBMLDocument> documentMap = m.getDocumentMap();
		Map<Long, SBMLDocument> newDocumentMap = new HashMap<Long, SBMLDocument>();
		
		
		for (Long oldSuid: documentMap.keySet()){
			Long suid = s.getObject(oldSuid, CyNetwork.class).getSUID();
			newDocumentMap.put(suid, documentMap.get(oldSuid));
		}
		
		/* TODO
		// mappings
		Map<Long, One2ManyMapping<String, Long>> nsb2nodeMap = m.get
		Map<Long, One2ManyMapping<String, Long>> nsb2nodeMap;
		for (Long oldSuid: )
		
		
		//private Map<Long, One2ManyMapping<String, Long>> NSBToNodeMappingMap;
		//private Map<Long, One2ManyMapping<Long, String>> nodeToNSBMappingMap;

		
		// update currentSUID
		Long suid = s.getObject(m.getCurrentSUID(), CyNetwork.class).getSUID();
		m.setCurrentSUID(suid);
		*/
		
	}
	
}
