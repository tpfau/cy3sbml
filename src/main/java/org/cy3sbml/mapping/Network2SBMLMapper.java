package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sbml.jsbml.SBMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mapping between CyNetworks and SBMLDocuments.
 * The mapping is defined for the CyRootNetwork. All subnetworks use the same
 * mapping and can access it via the rootNetwork SUID.
 *
 * The rootNetwork SUID is accessible via
 *     NetworkUtil.getRootNetworkSUID(CyNetwork network);
 *
 * The SBMLReader creates multiple networks with the mapping between networks
 * and SBMLDocuments managed by the SBMLManager which updates this mapper.
 * The mappings should only be changed via the SBMLManager and not directly.
 */
public class Network2SBMLMapper implements Serializable{
	private static final Logger logger = LoggerFactory.getLogger(Network2SBMLMapper.class);
	private static final long serialVersionUID = 1L;

	private Map<Long, SBMLDocument> documentMap;
	private Map<Long, One2ManyMapping<String, Long>> sbase2nodeMappingMap;
	private Map<Long, One2ManyMapping<Long, String>> node2sbaseMappingMap;
	
	public Network2SBMLMapper(){
		logger.debug("Network2SBMLMapper created");
		initMaps();
    }

	private void initMaps(){
		documentMap = new HashMap<>();
		sbase2nodeMappingMap = new HashMap<>();
		node2sbaseMappingMap = new HashMap<>();
	}

	/**
     * Get SBMLDocument for given rootNetworkSUID.
     */
    public SBMLDocument getDocumentForNetwork(Long rootSUID){
        SBMLDocument doc = null;
        if (rootSUID == null) {
            logger.debug("No SUID set. No SBMLDocument can be retrieved !");
            return null;
        }
        if (documentMap.containsKey(rootSUID)){
            doc = documentMap.get(rootSUID);
        }
        return doc;
    }

    /** Exists a SBMLDocument for the given rootNetwork ? */
	public boolean containsNetwork(Long rootSUID){
	    return (documentMap.containsKey(rootSUID));
	}

	/** Get all rootNetwork SUIDs which have an association SBMLDocument. */
	public Set<Long> keySet(){
	    return documentMap.keySet();
	}

	/** Get the complete DocumentMap. */
    public Map<Long, SBMLDocument> getDocumentMap(){
        return documentMap;
    }

    /**
     * The SBMLDocument is stored with the SBML id to SUID (CyNode) mapping.
     * A reverse mapping from SUIDs to SBML ids is created in the process.
     */
	public void putDocument(Long rootSUID, SBMLDocument doc, One2ManyMapping<String, Long> mapping){
        logger.debug("Network put: " + rootSUID.toString());
	    documentMap.put(rootSUID,  doc);
		sbase2nodeMappingMap.put(rootSUID, mapping);
		node2sbaseMappingMap.put(rootSUID, mapping.createReverseMapping());
	}

	/**
	 * Removes the document for a given root network SUID.
     * This removes the mapping for all subnetworks of the given root network SUID.
	 */
	public void removeDocument(Long rootSUID){
		logger.debug("Network remove:" + rootSUID.toString());
		documentMap.remove(rootSUID);
		sbase2nodeMappingMap.remove(rootSUID);
		node2sbaseMappingMap.remove(rootSUID);
	}


    public One2ManyMapping<Long, String> getCyNode2SBaseMapping(Long rootSUID){
        if (rootSUID == null) {
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return node2sbaseMappingMap.get(rootSUID);
    }

    public One2ManyMapping<String, Long> getSBase2CyNodeMapping(Long rootSUID){
        if (rootSUID == null){
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return sbase2nodeMappingMap.get(rootSUID);
    }

	public String toString(){
		String info = "\n--- SBML2NetworkMapping ---\n";
		for (Long key: documentMap.keySet()){
			info += String.format("%s -> %s\n", key.toString(), documentMap.get(key).toString());
		}
		info += "-------------------------------";
		return info;
	}
}