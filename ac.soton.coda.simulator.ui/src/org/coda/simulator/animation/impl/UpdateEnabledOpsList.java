/*******************************************************************************
 * (c) Crown owned copyright (2017) (UK Ministry of Defence)
 *
 * All rights reserved. This program and the accompanying materials are 
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      University of Southampton - Initial API and implementation
 *******************************************************************************/
package org.coda.simulator.animation.impl;

import java.util.ArrayList;
import java.util.List;

import org.coda.simulator.ui.windowBuilder.SimulatorView;
import org.eclipse.emf.common.util.EList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;

import ac.soton.eventb.emf.components.AbstractComponentOperation;
import ac.soton.eventb.emf.components.Component;
import de.prob.core.Animator;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;

public class UpdateEnabledOpsList {
	
	private static UpdateEnabledOpsList instance = null;
	private List<AbstractExtension> componentList = new ArrayList<AbstractExtension>();
	private UpdateEnabledOpsList(){
	}
	
	public static UpdateEnabledOpsList getInstance(){
		if (instance == null)
			instance = new UpdateEnabledOpsList();
		return instance;
	}
	
	// Update the enabled ops list
	public void execute(){
		// return if there is no simulator view
		SimulatorView simulator = SimulatorView.getSimulator();
		if(simulator == null){
			return;
		}

		Machine mch = simulator.getMachine();
		if(mch == null) return;

		EList<AbstractExtension> exts = simulator.getMachine().getExtensions();
		// go through each extension and process components
		for (AbstractExtension ext : exts) {
			if (ext instanceof Component) {
				Component rootComponent = (Component) ext;
				componentList.addAll(rootComponent.getComponents());
			}
		}

		
		Animator animator = Animator.getAnimator();
		State currentState = animator.getCurrentState();
		List<Operation> enabledOps = currentState.getEnabledOperations();
		Table operationsTable = simulator.getOperationsTable();
		if (operationsTable == null) return;

		operationsTable.removeAll();
		
		String componentName = new String("");
		
		// for each enabled operation in the ProB model
		for(Operation proB_op: enabledOps){
			// find out which component is linked to this event 
			// for each component
			for(AbstractExtension ext: componentList){
				if(ext instanceof Component){
					Component component = (Component) ext;
					// get the component's operations
					EList<AbstractComponentOperation> opList = component.getOperations();
					for(AbstractComponentOperation absOp: opList){
						// see which events the component operations elaborate 
						EList<Event> elaboratesList = absOp.getElaborates();
						for(Event event: elaboratesList){
							// if the elaboration event is the same as the proB operation name
							if(event.getName().equals(proB_op.getName())){
								// then the component is linked to the enabled ProB op
								componentName = component.getName();
								break;
							}
						}
					}
				}
			}

			
			TableItem tableItem = new TableItem(operationsTable, SWT.NULL);
			List<String> arguments = proB_op.getArguments();

			String[] rowString = {componentName,  proB_op.getName() + " " + arguments};
			
			tableItem.setText(rowString);
			componentName = "";
		}
	}
}
