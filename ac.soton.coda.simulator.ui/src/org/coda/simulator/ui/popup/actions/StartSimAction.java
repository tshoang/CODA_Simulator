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
package org.coda.simulator.ui.popup.actions;

import java.util.List;

import org.coda.simulator.ui.windowBuilder.SimulatorView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eventb.core.IMachineRoot;

import de.prob.core.Animator;
import de.prob.core.command.ExecuteOperationCommand;
import de.prob.core.command.GetCurrentStateIdCommand;
import de.prob.core.command.GetEnabledOperationsCommand;
import de.prob.core.command.LoadEventBModelCommand;
import de.prob.core.command.StartAnimationCommand;
import de.prob.core.domainobjects.Operation;
import de.prob.exceptions.ProBException;
import de.prob.ui.PerspectiveFactory;

public class StartSimAction implements IObjectActionDelegate {

	private IStructuredSelection selected;
	@SuppressWarnings("unused")
	private IWorkbenchPart targetPart;

	/**
	 * Constructor for Action1.
	 */
	public StartSimAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.targetPart = targetPart;
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		// There should be just one selection
		Object s = selected.iterator().next();
		Animator animator = Animator.getAnimator();


		
		if ( s instanceof IMachineRoot) {
			IMachineRoot machineRoot = (IMachineRoot) s;
			SimulatorView.getSimulator().initialise(machineRoot);
			try {
				PerspectiveFactory.openPerspective();	//open the ProB perspective
				LoadEventBModelCommand.load(animator, machineRoot);
				StartAnimationCommand.start(animator);
				animator.getLanguageDependendPart().reload(animator);
				
				
			} catch (ProBException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selected = (IStructuredSelection) selection;
	}

}
