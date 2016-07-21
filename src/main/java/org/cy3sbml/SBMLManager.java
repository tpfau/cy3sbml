package org.cy3sbml;

import java.util.*;

import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.mapping.IdObjectMap;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.NetworkUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager class manages mappings between SBMLDocuments & CyNetworks.
 * 
 * The SBMLManager provides the entry point to interact with SBMLDocuments.
 * All access to SBMLDocuments should go via the SBMLManager.
 * 
 * The SBMLManager is a singleton class.
 */
public class SBMLManager {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private CyApplicationManager cyApplicationManager;

    private Long currentSUID;
    private Network2SBMLMapper network2sbml;
	private HashMap<Long, IdObjectMap> network2objectMap;

    /**
     * Construct the instance.
     * Use the variant without arguments for access.
     */
	public static synchronized SBMLManager getInstance(CyApplicationManager cyApplicationManager){
		if (uniqueInstance == null){
			uniqueInstance = new SBMLManager(cyApplicationManager);
		}
		return uniqueInstance;
	}

    /**
     * Get SBMLManager instance.
     * Use this function to access the SBMLManager.
     */
	public static synchronized SBMLManager getInstance(){
		if (uniqueInstance == null){
			logger.error("Access to SBMLManager before creation");
		}
		return uniqueInstance;
	}

    /** Constructor. */
	private SBMLManager(CyApplicationManager cyApplicationManager){
		logger.debug("SBMLManager created");
		this.cyApplicationManager = cyApplicationManager;
		reset();
	}
	
	/** Reset SBMLManager to empty state. */
	private void reset(){
        currentSUID = null;
		network2sbml = new Network2SBMLMapper();
		network2objectMap = new HashMap<>();
	}

    /**
     * Access to the SBML <-> network mapper.
     * The mapper should not be modified.
     */
	public Network2SBMLMapper getNetwork2SBMLMapper(){
	    return network2sbml;
	}

	/**
	 * Adds an SBMLDocument - network entry to the SBMLManager.
	 * 
	 * For all networks the root network is associated with the SBMLDocument
	 * so that all subnetworks can be looked up via the root network and
	 * the mapping. 
	 */
	public void addSBMLForNetwork(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){
		addSBMLForNetwork(doc, NetworkUtil.getRootNetworkSUID(network), mapping);
	}

    /** Adds an SBMLDocument - network entry to the SBMLManager. */
     public void addSBMLForNetwork(SBMLDocument doc, Long rootNetworkSUID, One2ManyMapping<String, Long> mapping){
		// document & mapping
		network2sbml.putDocument(rootNetworkSUID, doc, mapping);
		// object map
		network2objectMap.put(rootNetworkSUID, new IdObjectMap(doc));
	}

	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		return getMapping(suid);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID){
		return network2sbml.getSBase2CyNodeMapping(rootNetworkSUID);
	}

    /** Update current SBML for network. */
	public void updateCurrent(CyNetwork network) {
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		updateCurrent(suid);
	}
	
	/** Update current SBML via rootNetworkSUID. */
	public void updateCurrent(Long rootNetworkSUID) {
		logger.debug("Set current network to root SUID: " + rootNetworkSUID);
		setCurrentSUID(rootNetworkSUID);
	}

	/** Set the current network SUID. */
    private void setCurrentSUID(Long SUID){
        currentSUID = null;
        if (SUID != null && network2sbml.containsNetwork(SUID)){
            currentSUID = SUID;
        }
        logger.debug("Current network set to: " + currentSUID);
    }

    /** Get current network SUID. */
    public Long getCurrentSUID(){
        return currentSUID;
    }

    /**
     * Get current SBMLDocument.
     * Returns null if no current SBMLDocument exists.
     */
    public SBMLDocument getCurrentSBMLDocument(){
        return getSBMLDocument(currentSUID);
    }

    /**
     * Get SBMLDocument for given network.
     * Returns null if no SBMLDocument exist for the network.
     */
    public SBMLDocument getSBMLDocument(CyNetwork network){
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return getSBMLDocument(suid);
    }

    /**
     * Get SBMLDocument for given rootNetworkSUID.
     * Returns null if no SBMLDocument exist for the network.
     */
    public SBMLDocument getSBMLDocument(Long rootNetworkSUID){
        return network2sbml.getDocumentForSUID(rootNetworkSUID);
    }


    public One2ManyMapping<Long, String> getCurrentCyNode2SBaseMapping(){
        return network2sbml.getCyNode2SBaseMapping(currentSUID);
    }

    public One2ManyMapping<String, Long> getCurrentSBase2CyNodeMapping(){
        return network2sbml.getSBase2CyNodeMapping(currentSUID);
    }

	/**
     * Lookup a SBase object via id.
     *
     * The SBases are stored so that their information can be used for display
     * in the results panel. The lookup gets the dictionary for the current network
     * and searches for the key.
     *
     * The object maps are created when the SBMLDocument is stored.
     */
	public SBase getSBaseById(String key){
        return getSBaseById(key, currentSUID);
	}

    public SBase getSBaseById(String key, Long SUID){
        return network2objectMap.get(SUID).getObject(key);
    }


    /**
     * Lookup the list of ObjectIds.
     * FIXME: not really needed, probably overkill for large selections.
     */
	public List<String> getObjectIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2SBaseMapping();
		return new LinkedList<>(mapping.getValues(suids));
	}

	/** String information. */
	public String toString(){
	    return network2sbml.toString();
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set all information in SBMLManager from given Network2SBMLMapper.
     * This function is used to set the Network2SBMLMapper from a stored state.
     * For instance during session reloading.
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper){
        logger.debug("SBMLManager from given mapper");

        network2sbml = mapper;
        network2objectMap = new HashMap<>();

        // Create all the trees
        Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();
        for (Long suid: documentMap.keySet()){
            SBMLDocument doc = documentMap.get(suid);

            // create and store navigation tree
            IdObjectMap map = new IdObjectMap(doc);
            network2objectMap.put(suid, map);
        }

        // Set current network and tree
        CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
        updateCurrent(currentNetwork);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /** The network is a network with a mapping to an SBMLDocument. */
    @Deprecated
    public boolean networkIsSBML(CyNetwork network){
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return network2sbml.containsNetwork(suid);
    }

    /**
     * Remove all mapping entries for networks which are not in the SBML<->network mapping.
     *
     * The current network set can be accessed via
     *      CyNetworkManager.getNetworkSet()
     */
    @Deprecated
    public void synchronizeDocuments(Collection<CyNetwork> networks){
        HashSet<Long> suids = new HashSet<>();
        for (CyNetwork network: networks){
            suids.add(NetworkUtil.getRootNetworkSUID(network));
        }
        for (Long key : network2sbml.keySet()){
            if (!suids.contains(key)){
                network2sbml.removeDocument(key);
            }
        }
    }

}
