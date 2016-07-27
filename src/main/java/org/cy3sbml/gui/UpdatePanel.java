package org.cy3sbml.gui;

import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;


/**
 * Updates the Panel information based on selection.
 */
public class UpdatePanel implements Runnable {
	private SBMLPanel panel;
	private CyNetwork network;

    public UpdatePanel(SBMLPanel panel, CyNetwork network) {
        this.panel = panel;
        this.network = network;
    }

    public void run() {
        updateInformation();
    }
    
	/**
	 * Here the node information update is performed.
	 * If multiple nodes are selected only the information for the first node is displayed.
	 * */
	public void updateInformation(){
		SBMLManager sbmlManager = SBMLManager.getInstance();

		// selected node SUIDs
		LinkedList<Long> suids = new LinkedList<Long>();
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		for (CyNode n : nodes){
			suids.add(n.getSUID());
		}
		// information for selected node(s)
		SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
		if (document != null){
			List<String> objectIds = sbmlManager.getObjectIds(suids);
		
			if (objectIds.size() > 0){
				// use first node in selection
				String key = objectIds.get(0);
				SBase sbase = sbmlManager.getSBaseById(key);
				if (sbase != null){
					panel.showSBaseInfo(sbase);
				} else {
					panel.setText("<h2>No information</h2><p>No SBML object registered for node.</p>");
				}
						
			} else {
				panel.showSBaseInfo(document.getModel());
			}
		} else {
			panel.setText("<h2>No information</h2><p>No SBML associated with current network.</p>");
		}
	}	
    
    
}
