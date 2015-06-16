package org.cy3sbml.biomodel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.cy3sbml.ConnectionProxy;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;

import uk.ac.ebi.biomodels.ws.SimpleModel;

public class SearchBioModel {
	DialogTaskManager dialogTaskManager;
	SearchBioModelTaskFactory searchBioModelTaskFactory;
	
	private SearchContent searchContent;
	private List<String> modelIds;
	private LinkedHashMap<String, SimpleModel> simpleModels;
	
	private BioModelWSInterface bmInterface;
	
	public SearchBioModel(ServiceAdapter adapter){
		ConnectionProxy connectionProxy = adapter.connectionProxy;
		if ("direct".equals(connectionProxy.getProxyType())){
			bmInterface = new BioModelWSInterface();	
		} else {
			String host = connectionProxy.getProxyHost();
			String port = connectionProxy.getProxyPort();
			bmInterface = new BioModelWSInterface(host, port);
		}
		resetSearch();
	}
	
	private void resetSearch(){
		searchContent = null;
		modelIds = new LinkedList<String>();
		simpleModels = new LinkedHashMap<String, SimpleModel>();
	}
		
	public List<String> getModelIds() {
		return modelIds;
	}

	public String getModelId(int index){
		return modelIds.get(index);
	}
	
	public LinkedHashMap<String, SimpleModel> getSimpleModels(){
		return simpleModels;
	}
	public SimpleModel getSimpleModel(int index){
		return simpleModels.get(index);
	}
	
	public int getSize(){
		return modelIds.size();
	}
	
	public void searchBioModels(SearchContent sContent){
		resetSearch();
		searchContent = sContent;
		modelIds = searchModelIdsForSearchContent(searchContent);
		simpleModels = getSimpleModelsForSearchResult(modelIds);
	}
	
	public void getBioModelsByParsedIds(Set<String> parsedIds){
		resetSearch();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS);
		searchContent = new SearchContent(map);
		modelIds = new LinkedList<String>(parsedIds);
		simpleModels = getSimpleModelsForSearchResult(modelIds);
	}
	
	private LinkedHashMap<String, SimpleModel> getSimpleModelsForSearchResult(List<String> idsList){
		String[] ids = (String[]) idsList.toArray();
		return bmInterface.getSimpleModelsByIds(ids);
	}
	
	private List<String> searchModelIdsForSearchContent(SearchContent content){
		// Run the biomodel task with a taskManger
		
		
		// Necessary to init the tasks with different contents
		SearchBioModelTaskFactory searchBioModelTaskFactory = new SearchBioModelTaskFactory();
		TaskIterator iterator = searchBioModelTaskFactory.createTaskIterator(content, bmInterface);
		// The TaskIterator manages the creation of tasks of the form:
	
		// execute the iterator with dialog
		dialogTaskManager.execute(iterator);
		
		//TODO: necessary to get information back from the tasks, namely the ids
		return task.getIds();
	}
	
	public static void addIdsToResultIds(final List<String> ids, List<String> resultIds, final String mode){
		// OR -> combine all results
		if (mode.equals(SearchContent.CONNECT_OR)){
			resultIds.addAll(ids);
		}
		// AND -> only the combination results of all search terms	
		if (mode.equals(SearchContent.CONNECT_AND)){
			if (resultIds.size() > 0){
				resultIds.retainAll(ids);
			} else {
				resultIds.addAll(ids);
			}
		}
	}
	
	public String getHTMLInformation(final List<String> selectedModelIds){
		String info = getHTMLHeaderForModelSearch();
		info += BioModelWSInterfaceTools.getHTMLInformationForSimpleModels(simpleModels, selectedModelIds);
		return BioModelGUIText.getString(info);
	}
	
	private String getHTMLHeaderForModelSearch(){
		String info = String.format(
				"<h2>%d BioModels found for </h2>" +
				"<hr>", getSize());
		info += searchContent.toHTML();
		info += "<hr>";
		return info;
	}
		
	public String getHTMLInformationForModel(int modelIndex){
		SimpleModel simpleModel = getSimpleModel(modelIndex);
		return BioModelWSInterfaceTools.getHTMLInformationForSimpleModel(simpleModel);
	}
}
