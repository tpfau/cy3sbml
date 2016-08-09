package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.validator.ValidatorDialog;
import org.cytoscape.application.swing.AbstractCyAction;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** 
 * Open the BioModel GUI for importing BioModels via search terms. 
 */
public class ValidationAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ValidationAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;
	
	/** Constructor. */
	public ValidationAction(ServiceAdapter adapter){
		super(ValidationAction.class.getSimpleName());
		this.adapter = adapter;
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_VALIDATION));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_VALIDATION);
		setToolbarGravity(GUIConstants.GRAVITY_VALIDATION);
	}

	public boolean isInToolBar() {
		return true;
	}
	
	public boolean isInMenuBar() {
		return false;
	}
	
    public static void openValidationPanel(ServiceAdapter adapter){
    	SBMLDocument document = SBMLManager.getInstance().getCurrentSBMLDocument();
    	JFrame parentFrame = adapter.cySwingApplication.getJFrame();
    	if (document == null){
    		JOptionPane.showMessageDialog(parentFrame,
					"<html>SBML network has to be loaded before validation.<br>" +
					"Import network from BioModel or load network from file or URL first.");
    	}
    	else{
    		ValidatorDialog validationDialog = new ValidatorDialog(adapter);
    		validationDialog.runValidation(document);
    		validationDialog.setVisible(true);
    	}
    }
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed()");
		openValidationPanel(adapter);  
	}
}
