package org.cy3sbml.gui;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.taverna.robundle.Bundle;
import org.cy3sbml.archive.ArchiveReaderTask;
import org.cy3sbml.archive.BundleAnnotation;
import org.cy3sbml.archive.BundleManager;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.SBMLManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the Panel information based on selection.
 */
public class PanelUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PanelUpdater.class);

    private static final String TEMPLATE_NO_SBML_NODE = SBaseHTMLFactory.createHTMLText(
            "<h2>No information</h2>" +
            "<p>No SBML object registered for node in ObjectMapper.</p>" +
            "<p>Some nodes do not have SBase objects associated, e.g." +
            "the <code>AND</code> and <code>OR</code> nodes in the FBC package.</p>" +
            "<p>Other examples are the base units like <code>dimensionless</code>" +
            "or <code>mole</code> which are not part of the model.</p>");

    private static final String TEMPLATE_NO_BUNDLE_NODE = SBaseHTMLFactory.createHTMLText(
            "<h2>No information</h2>" +
            "<p>No Bundle object registered for node in ObjectMapper.</p>" +
            "<p>Some nodes do not have annotation information associated.");

    private static final String TEMPLATE_LOAD_WEBSERVICE = SBaseHTMLFactory.createHTMLText(
            "<h2>Web Services</h2>" +
            "<p><i class=\"fa fa-spinner fa-spin fa-3x fa-fw\"></i>\n" +
            "Loading information from WebServices ...</p>");


    private static final String TEMPLATE_NO_SBML_OR_BUNDLE = SBaseHTMLFactory.createHTMLText(
            "<h2>No information</h2>" +
            "<p>No SBMLDocument or Archive Bundle associated with the current network.</p>");


    private InfoPanel panel;
	private CyNetwork network;

    public PanelUpdater(InfoPanel panel, CyNetwork network) {
        this.panel = panel;
        this.network = network;
    }

    /**
     * Here the node information update is performed.
     * Depending of the kind of network different updates are performed
     * If multiple nodes are selected only the information for the first node is displayed.
     */
    public void run() {

        // associated SBMLDocument
        SBMLManager sbmlManager = SBMLManager.getInstance();
        SBMLDocument document = sbmlManager.getCurrentSBMLDocument();

        // associated bundle
        BundleManager bundleManager = BundleManager.getInstance();
        Bundle bundle = bundleManager.getCurrentBundle();

        if (document != null) {
            updateSBMLPanel(document);
        } else if (bundle != null) {
            updateBundlePanel(bundle);
        } else {
            logger.debug("No SBMLDocument or Bundle for current network: " + network);
            panel.setText(TEMPLATE_NO_SBML_OR_BUNDLE);
        }
    }


    /**
     * Updates the panel information for an SBMLDocument.
     * @param document
     */
    private void updateSBMLPanel(SBMLDocument document){
        SBMLManager sbmlManager = SBMLManager.getInstance();

        // selected node SUIDs
        LinkedList<Long> suids = new LinkedList<>();
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        for (CyNode n : nodes){
            suids.add(n.getSUID());
        }
        // information for selected node(s)

            List<String> cyIds = sbmlManager.getCyIdsFromSUIDs(suids);
            if (cyIds.size() > 0){
                // use first SBase
                String cyId = cyIds.get(0);
                SBase sbase = sbmlManager.getSBaseByCyId(cyId);
                if (sbase != null){
                    panel.setText(TEMPLATE_LOAD_WEBSERVICE);
                    panel.showSBaseInfo(sbase);
                } else {
                    panel.setText(TEMPLATE_NO_SBML_NODE);
                }
            } else {
                // show document/model information
                panel.showSBaseInfo(document);
            }
    }

    /**
     * Updates the panel information for a Bundle.
     */
    private void updateBundlePanel(Bundle bundle){
        BundleManager bundleManager = BundleManager.getInstance();

        // selected nodes
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);

        // information for selected node(s)

        BundleAnnotation bundleAnnotation = bundleManager.getCurrentBundleAnnotation();
        Map<String, List<String>> pathAnnotations = bundleAnnotation.getPathAnnotations();

        // Get annotation for node (default to root)
        String path = "/";
        // TODO: get root node

        if (nodes != null && nodes.size() > 0) {
            CyNode n = nodes.get(0);
            path = AttributeUtil.get(network, n, ArchiveReaderTask.NODE_ATTR_PATH, String.class);
        }

        // create html

        String text = String.format("<h1><small>%s</small></h1>\n", path);
        // TODO: link to file, image, path, mediatype from node attributes
        if (pathAnnotations != null && pathAnnotations.containsKey(path)){
            for (String s : pathAnnotations.get(path)) {
                text += String.format("<p><code>%s</code></p>",
                        StringEscapeUtils.escapeHtml(s));
            }
        }

        // add links
        text = text.replaceAll("&quot;(http://.*?)&quot;", "<a href=\"$1\">$1</a>");
        // pack in html
        text = SBaseHTMLFactory.createHTMLText(text);
        panel.setText(text);
    }

}
