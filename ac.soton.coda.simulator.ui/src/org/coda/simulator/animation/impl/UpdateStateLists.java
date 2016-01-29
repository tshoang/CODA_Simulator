package org.coda.simulator.animation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.coda.simulator.ui.windowBuilder.SimulatorView;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.emf.components.Component;
import ac.soton.eventb.emf.components.Connector;
import ac.soton.eventb.emf.components.WakeQueue;
import ac.soton.eventb.statemachines.AbstractNode;
import ac.soton.eventb.statemachines.State;
import ac.soton.eventb.statemachines.Statemachine;
import ac.soton.eventb.statemachines.StatemachinesPackage;
import ac.soton.eventb.statemachines.TranslationKind;
import de.prob.core.Animator;
import de.prob.core.domainobjects.History;
import de.prob.core.domainobjects.HistoryItem;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.Variable;

public class UpdateStateLists  {

	private static UpdateStateLists instance = null;;
	private UpdateStateLists(){}
	
	public static UpdateStateLists getInstance(){
		if (instance == null)
			instance = new UpdateStateLists();
		return instance;
	}
	
	
	SimulatorView simulatorView = null;
	private Group componentsGroup;
	private Group timeGroup;
	private Group connectorGroup;
	private FormToolkit toolkit;
	private Variable currTimeVar;

	// cache of what we consider to be the current state for this invocation of the execute method
	private int historyPtr = 0;
	private Operation lastOp;
	private Map<String, Variable> stateMap;
	
	/*
	 * This sets the 'stateMap' to reflect the current state
	 * and the 'lastOp' to reflect the last operation executed.
	 * Since the listener does not get activated as soon as the ProB state changes and ProB may have moved on,
	 * the 'current state' of ProB may not reflect the state we need to record here.
	 * Therefore this routine keeps track of the state of the history it last used and obtains the next item
	 * from ProB's history based on that.
	 */
	private void setCurrentStateAndLastOperation() {
		
		History hist = Animator.getAnimator().getHistory();
		int currHistPos = hist.getCurrentPosition();
		if (currHistPos<=historyPtr){
			historyPtr = 0;
		}
		
		HistoryItem last = hist.getHistoryItem(historyPtr-currHistPos);
		lastOp = last == null ? null : last.getOperation();

		HistoryItem current = hist.getHistoryItem(historyPtr-currHistPos+1);
		//State st = current == null ? null :current.getState();
		stateMap = current == null || current.getState()==null ? Collections.<String,Variable>emptyMap() : current.getState().getValues();
		historyPtr = historyPtr+1;
		return;
	}

	

	// Update the state list, taking into account filtering
	public void execute() {
		simulatorView = SimulatorView.getSimulator();
		// Do nothing if the simulator view is not active
		if (simulatorView == null) return;
		Machine machine = simulatorView.getMachine();
		if(machine == null) return;
		
		setCurrentStateAndLastOperation();
		
		componentsGroup = simulatorView.getComponentGroup();
		connectorGroup = simulatorView.getConnectorGroup();
		timeGroup = simulatorView.getTimeGroup();
		Composite container = simulatorView.getContainer();
		// we do not really want to dispose of all of the component group
		// this is a temporary measure.
		if (componentsGroup == null) return;
		componentsGroup.dispose();
		if (connectorGroup != null) timeGroup.dispose();
		if (timeGroup != null) connectorGroup.dispose();
		// create new groups and re-assign to the fields
		simulatorView.createNewGroups();
		componentsGroup = simulatorView.getComponentGroup();
		timeGroup = simulatorView.getTimeGroup();
		connectorGroup = simulatorView.getConnectorGroup();

		// Keep track of this machine's state machines in this list
		List<Component> componentList = new ArrayList<Component>();
		List<Connector> connectorList = new ArrayList<Connector>();

		// add the current time
		toolkit = new FormToolkit(Display.getCurrent());

		currTimeVar = stateMap.get("current_time");
		if (currTimeVar != null) {
			String[] timeRow = { "current_time", currTimeVar.getValue() };
			Table timeTable = new Table(timeGroup, SWT.BORDER
					| SWT.FULL_SELECTION);
			toolkit.adapt(timeTable);
			toolkit.paintBordersFor(timeTable);
			timeTable.setHeaderVisible(true);
			timeTable.setLinesVisible(true);
			int lastColumnIndex = timeRow.length;
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				TableColumn col = new TableColumn(timeTable, SWT.NULL);
				col.setText(timeRow[loopIndex]);
			}
			// ... and pack
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				timeTable.getColumn(loopIndex).pack();
			}
			
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				timeTable.getColumn(loopIndex).pack();
			}

			timeGroup.pack();
			timeGroup.layout();
		}

		
		//===================================
		simulatorView.updateStatusTable();
		//===================================
		
		// go through each extension and process components and connectors
		for (AbstractExtension ext : machine.getExtensions()) {
			if (ext instanceof Component) {
				Component rootComponent = (Component) ext;
				componentList.addAll(rootComponent.getComponents());
				connectorList.addAll(rootComponent.getConnectors());
			}
		}
		
		OracleHandler oracle = simulatorView.getOracle();
		String clock = currTimeVar==null? "0" : currTimeVar.getValue();
		
		// first record the last executed event
		if (oracle != null  && lastOp != null){
			oracle.addStepToTrace(simulatorView.getMachine().getName(), lastOp, clock);
		}
		// now record a snapshot of the components state
		if (oracle != null) oracle.startSnapshot(clock);			
		
		for (Component component : componentList) {
			processComponent(component, oracle);
		}

		for (Connector connector : connectorList) {
			processConnector(connector, oracle);
		}
		
		// re-layout the container
		container.layout();
	
		if (oracle != null ) oracle.stopSnapshot(clock);
	}

	private void processConnector(Connector connector, OracleHandler oracle) {
		List<String[]> tableContent = new ArrayList<String[]>();
		Variable connStatesRaw = stateMap.get(connector.getName());
		if (connStatesRaw != null) {
			Map<Integer, String> connStatesParsed = new StateResultStringParser(
					connStatesRaw.getValue()).parse();

			Table connectorTable = new Table(connectorGroup, SWT.BORDER
					| SWT.FULL_SELECTION);
			toolkit.adapt(connectorTable);
			toolkit.paintBordersFor(connectorTable);
			connectorTable.setHeaderVisible(true);
			connectorTable.setLinesVisible(true);
			
			String timeString = currTimeVar.getValue();
			Integer currTimeVarAsInteger = Integer.valueOf(timeString);

			Map<Integer, String> connStatesParsedandFiltered = new HashMap<Integer, String>();
			
			
			String lastValue = "";
			
			// this works out the contents of the connectors table by finding all the
			// parsed states with a time >= current time and adding those as String arrays
			Iterator<Integer> iter = connStatesParsed.keySet().iterator();
			while (iter.hasNext()) {
				Integer time = iter.next();
				if (time.compareTo(currTimeVarAsInteger) >= 0) {
					connStatesParsedandFiltered.put(time, connStatesParsed.get(time));
					String[] rowString = { 
							time.toString(), connStatesParsed.get(time) };
					tableContent.add(rowString);
				}
				else{
					lastValue = connStatesParsed.get(time);
				}
			}
			
			// set up the table, with a header
			String[] title = { connector.getName(), lastValue };
			
			//record the new connector state in the oracle
			if (oracle != null ) oracle.addValueToSnapshot(connector.getName(), connStatesParsedandFiltered.toString(), currTimeVar.getValue());
			
			int lastColumnIndex = title.length;
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				TableColumn col = new TableColumn(connectorTable, SWT.NULL);
				col.setText(title[loopIndex]);
			}
			// iterate through the tableContent strings adding them as
			// tableItems
			for (String[] rowContent : tableContent) {
				TableItem ti = new TableItem(connectorTable, SWT.NULL);
				ti.setText(rowContent);
			}
			// ... and pack
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				connectorTable.getColumn(loopIndex).pack();
			}
			connectorGroup.pack();
		}
	}

	// The top 'root' component contains all the components in the model
	private void processComponent(Component component, OracleHandler oracle) {
		// The following code creates a group of tables.
		// we need to obtain the statemachines for this component, and report their
		// current states
		EList<EObject> statemachineList = component.getAllContained(StatemachinesPackage.Literals.STATEMACHINE, true);

		String componentName = component.getName();

		List<String[]> tableContent = new ArrayList<String[]>();

		// get the state-machine state
		for (EObject eObject : statemachineList) {
			if (eObject instanceof Statemachine){
				Statemachine statemachine = (Statemachine)eObject;
				if (TranslationKind.SINGLEVAR.equals(statemachine.getTranslation())){
					Variable variable = stateMap.get(statemachine.getName());
					if (variable != null) {
						String value = variable.getValue();
						// add to the list tableContent
						String[] tempString = { statemachine.getName(), value };
						tableContent.add(tempString);
						
						//record the statemachine state in the oracle
						if (oracle != null ) oracle.addValueToSnapshot(statemachine.getName(), value, currTimeVar.getValue());

					}
				}else if(TranslationKind.MULTIVAR.equals(statemachine.getTranslation())){
					EList<AbstractNode> smNodes = statemachine.getNodes();
					for (AbstractNode node : smNodes) {
						if (node instanceof State){
							String currentStateName = ((State)node).getName();
							// we have found a state-machine state
							// now get the value from the stateMap
							Variable variable = stateMap.get(currentStateName);
							if (variable != null) {
								String value = variable.getValue();
								if (value.equals("TRUE")) {
									// add to the list tableContent
									String[] tempString = { statemachine.getName(), currentStateName };
									tableContent.add(tempString);
									//record the statemachine state in the oracle
									if (oracle != null ) oracle.addValueToSnapshot(statemachine.getName(), currentStateName, currTimeVar.getValue());
								}
							}
						}
					}
				}
			}
		}
		
		int numbRows = tableContent.size();
		
		// the wakequeues for the component are added in extra columns
		EList<WakeQueue> wakeQueueList = component.getWakeQueues();
		List<String[]> wqCols = new ArrayList<String[]>();
		for (WakeQueue wq : wakeQueueList){	
			List<String> newCol = new ArrayList<String>();
			
			String wqName = wq.getName()+"_wakequeue";	//componentName +"_"+
			Variable wakeUpVar = stateMap.get(wqName);
			Variable wakeQMax = stateMap.get(wqName+"_max");
			if (wakeUpVar != null && wakeQMax != null) {
				Map<Integer, String> parsedWakeups = new StateResultStringParser(wakeUpVar.getValue()).parse();
				Map<Integer, String> parsedWakeQMax = new StateResultStringParser(wakeQMax.getValue()).parseReverse();
				if (parsedWakeups != null && parsedWakeQMax != null) {
					Map<Integer, String> parsedandFilteredWakeups = new HashMap<Integer, String>();
					Map<Integer, String> parsedandFilteredWakeQMax = new HashMap<Integer, String>();

					Integer currTimeVarAsInteger = Integer.valueOf(currTimeVar.getValue());
					for (Integer time :  parsedWakeups.keySet()){
						if (time.compareTo(currTimeVarAsInteger) >= 0) {
							parsedandFilteredWakeups.put(time, parsedWakeups.get(time));
							String max = "?";
							if (parsedWakeQMax.containsKey(time)){
								max = parsedWakeQMax.get(time);
								parsedandFilteredWakeQMax.put(time, max);
							}
							newCol.add(time.toString()+".."+max);
						}
					}
					if (oracle != null ){ 
						oracle.addValueToSnapshot(wqName, parsedandFilteredWakeups.toString(), currTimeVar.getValue());
						oracle.addValueToSnapshot(wqName+"_max", parsedandFilteredWakeQMax.toString(), currTimeVar.getValue());
					}
				}
			}
			
			if (newCol.size()>numbRows) numbRows = newCol.size();
			if (newCol.size() >0) {
				wqCols.add(newCol.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
			}else{
				wqCols.add(ArrayUtils.EMPTY_STRING_ARRAY);
			}
		}

		//Now construct the table from the statemachine rows and the wakequeue columns
		Table componentsTable = new Table(componentsGroup, SWT.BORDER
				| SWT.FULL_SELECTION);
		toolkit.adapt(componentsTable);
		toolkit.paintBordersFor(componentsTable);
		componentsTable.setHeaderVisible(true);
		componentsTable.setLinesVisible(true);
		//Titles and columns
		List<String> titleList = new ArrayList<String>();
		titleList.add(componentName);
		titleList.add("State");
		for (WakeQueue wq : wakeQueueList){
			titleList.add(wq.getName());
		}
		for (String title : titleList){
			TableColumn col = new TableColumn(componentsTable, SWT.NULL);
			col.setText(title);
		}
		
		String[] rowContent = new String[titleList.size()];

		for (int i=0;i<numbRows;i++){
			TableItem ti = new TableItem(componentsTable, SWT.NULL);
			for (int j=0; j<rowContent.length; j++){
				assert (tableContent.size() == 2);
				rowContent[j] = j<2? 
									i<tableContent.size() ? tableContent.get(i)[j] : "" :
									i<wqCols.get(j-2).length? wqCols.get(j-2)[i] : ""	;
			}		
			ti.setText(rowContent);
		}
		
		// ... and pack
		for (TableColumn column : componentsTable.getColumns()){
			column.pack();
		}
		componentsGroup.pack();
	}

}
