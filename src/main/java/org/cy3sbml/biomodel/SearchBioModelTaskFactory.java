package org.cy3sbml.biomodel;

import org.cytoscape.task.internal.creation.CloneNetworkTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;


public class SearchBioModelTaskFactory implements TaskFactory {

	
	
	@Override
	public TaskIterator createTaskIterator() {
		
		SearchBioModelTask task = new SearchBioModelTask(content, bmInterface);
		
    	return new TaskIterator();
		
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

}
