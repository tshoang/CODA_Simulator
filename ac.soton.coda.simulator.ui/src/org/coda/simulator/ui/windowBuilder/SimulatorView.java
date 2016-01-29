package org.coda.simulator.ui.windowBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.coda.simulator.animation.impl.OracleHandler;
import org.coda.simulator.animation.impl.UpdateEnabledOpsList;
import org.coda.simulator.animation.impl.UpdateStateLists;
import org.coda.simulator.ui.SimulatorException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CBanner;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eventb.core.IMachineRoot;
import org.eventb.emf.core.AbstractExtension;
import org.eventb.emf.core.EventBObject;
import org.eventb.emf.core.machine.Event;
import org.eventb.emf.core.machine.Machine;
import org.eventb.emf.persistence.factory.RodinResource;
import org.rodinp.core.IRodinElement;

import swing2swt.layout.FlowLayout;
import ac.soton.eventb.emf.components.AbstractComponentOperation;
import ac.soton.eventb.emf.components.Component;
import ac.soton.eventb.emf.components.External;
import de.prob.core.Animator;
import de.prob.core.command.ExecuteOperationCommand;
import de.prob.core.command.GetCurrentStateIdCommand;
import de.prob.core.command.GetEnabledOperationsCommand;
import de.prob.core.domainobjects.Operation;
import de.prob.core.domainobjects.State;
import de.prob.core.domainobjects.Variable;
import de.prob.exceptions.ProBException;
import de.prob.ui.StateBasedViewPart;

public class SimulatorView extends StateBasedViewPart {

	private static SimulatorView simulator = null;

	public static SimulatorView getSimulator() {
		if (simulator==null) simulator = new SimulatorView();
		return simulator;
	}

	private static Machine machine;
	private String statusText;
	private String oldStatusText;
	public String getStatusText() {
		if (statusText == null) statusText = "NULL";
		return statusText;
	}

	public Machine getMachine(){
		return machine;
	}
	
	private static OracleHandler oracle = null;
	public OracleHandler getOracle(){
		if (oracle == null) {
			oracle = OracleHandler.getOracle();
		}
		return oracle;
	}
	
	public void initialise(IMachineRoot machineRoot){
		//TODO: This would be better
		//machine = (Machine) EMFRodinDB.INSTANCE.loadEventBComponent(machineRoot); 
		
		// load the machine as an EMF model
		IFile machineFile = machineRoot.getResource();
		// create the path
		IPath path = new Path("platform:/resource");
		IPath filePath = machineFile.getFullPath();
		path = path.append(filePath);
		// Create a resource with the path
		ResourceSet machineResSet = new ResourceSetImpl();
		Resource resource = machineResSet.createResource(URI.createURI(path.toString()));
		// load the resource
		try {
			resource.load(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<IRodinElement, EventBObject> resourceMap = ((RodinResource) resource).getMap();
		machine = (Machine) resourceMap.get(machineRoot);
		getOracle().initialise(machine);	//ensure we start in record mode
	}

	public static final String ID = "org.coda.simulator.ui.windowBuilder.SimulatorView"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Button btnTickN;
	private Button btnSave;
	private Button btnContinue;
	private Text count;
	private Button btnStop;
	private Button btnReplay;
	private Button btnRestart;
	private Button btnStep;
	private Table operationsTable;
	private Composite container;
	private Composite parent;

	private String countField = "5";

	public Composite getParent() {
		return parent;
	}

	public Composite getContainer() {
		return container;
	}

	private Group buttonGroup;
	private Group timeGroup;
	private Table statusTable;
	private Group componentGroup;
	private Group connectorGroup;
	private FormData fd_componentGroup;
	private FormData fd_connectorGroup;
	private FormData fd_timeGroup;

	public Group getConnectorGroup() {
		return connectorGroup;
	}

	public Group getTimeGroup() {
		return timeGroup;
	}
	
	public void updateStatusTable() {
		if (statusTable != null) statusTable.dispose();
		statusTable = new Table(timeGroup, SWT.BORDER | SWT.FULL_SELECTION);
		toolkit.adapt(statusTable);
		toolkit.paintBordersFor(statusTable);
		statusTable.setHeaderVisible(true);
		statusTable.setLinesVisible(false);
		TableColumn col = new TableColumn(statusTable, SWT.NULL);
		col.setText(getStatusText());
		col.pack();
		timeGroup.layout();
	}

	public Group getComponentGroup() {
		return componentGroup;
	}

	public Table getOperationsTable() {
		return operationsTable;
	}

	/** 
	 * 
	 * 
	 */
	public SimulatorView() {
		simulator = this;
	}

	
	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 * @return 
	 */
	@Override
	public Control createStatePartControl(Composite parent) {
		this.parent = parent;
		
		getOracle().restart(getSite().getShell(), "test", machine);
		
		if (oracle.isPlayback()) statusText = "Playback";
		else statusText = "Recording";
		
		container = toolkit.createComposite(parent, SWT.V_SCROLL);
		toolkit.paintBordersFor(container);
		container.setLayout(new FormLayout());
		{
			operationsTable = new Table(container, SWT.BORDER
					| SWT.FULL_SELECTION);
			operationsTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDoubleClick(MouseEvent e) {

					// If we have selected an event (name) from the enabled
					// operations then we want to execute the event in ProB
					
					// Manual selection of events is disabled during playback
					if (oracle!=null && oracle.isPlayback()){
						MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_ERROR | SWT.OK);
						mbox.setText("Error - Cannot Execute Event");
						mbox.setMessage("Cannot select events manually while playback is in progress.");
						mbox.open();
						return;
					}
					
					TableItem selected = operationsTable
							.getItem(operationsTable.getSelectionIndex());
					String selectedOperationData = selected.getText(1);
					int endIdx = selectedOperationData.indexOf(" ");
					if (endIdx == -1) {
						endIdx = selectedOperationData.indexOf("[");
					}
					if (endIdx == -1) {
						try {
							throw new SimulatorException(
									"Simulator Error - Operation format not recognized");
						} catch (SimulatorException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
					String operationName = selectedOperationData.substring(0,
							endIdx);
					Animator animator = Animator.getAnimator();
					List<Operation> enabledOpsList = null;
					try {
						enabledOpsList = GetEnabledOperationsCommand.getOperations(
								animator,
								GetCurrentStateIdCommand.getID(animator));
					} catch (ProBException e1) {
						e1.printStackTrace();
					}
					Operation selectedOp = null;
					if (enabledOpsList != null) {
						for (Operation op : enabledOpsList) {
							if (op.getName().equals(operationName)) {
								selectedOp = op;
								break;
							}
						}
						executeOperation(animator, selectedOp, false);
					}

				}
			});
			fd_operationsTable = new FormData();
			operationsTable.setLayoutData(fd_operationsTable);

			String[] title = {"Component", "Enabled Operation"};

			int lastColumnIndex = title.length;
			for (int loopIndex = 0; loopIndex < lastColumnIndex; loopIndex++) {
				TableColumn col = new TableColumn(operationsTable, SWT.NULL);
				col.setText(title[loopIndex]);
			}
			
			toolkit.adapt(operationsTable);
			toolkit.paintBordersFor(operationsTable);
			operationsTable.setHeaderVisible(true);
			operationsTable.setLinesVisible(true);

			for(int i = 0; i < lastColumnIndex; i++){
				operationsTable.getColumn(i).pack();
			}

			createNewGroups();
		}
		initializeToolBar();
		initializeMenu();
		return container;
	}

	public void createNewGroups() {
		{
			buttonGroup = new Group(container, SWT.BORDER);
			fd_operationsTable.left = new FormAttachment(buttonGroup, 29);
			fd_operationsTable.right = new FormAttachment(buttonGroup, 263, SWT.RIGHT);
			fd_operationsTable.top = new FormAttachment(0, 10);
			fd_buttonGroup = new FormData();
			fd_buttonGroup.top = new FormAttachment(0, 100);
			fd_buttonGroup.right = new FormAttachment(100, -292);
			buttonGroup.setLayoutData(fd_buttonGroup);
			toolkit.adapt(buttonGroup);
			toolkit.paintBordersFor(buttonGroup);
			buttonGroup.setLayout(null);
			{
				btnTickN = new Button(buttonGroup, SWT.NONE);
				btnTickN.setBounds(10, 10, 60, 25);
				btnTickN.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						try {
							incTime();
						} catch (ProBException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				});
				toolkit.adapt(btnTickN, true, true);
				btnTickN.setText("Tick N");
			}
			{
				btnStep = new Button(buttonGroup, SWT.NONE);
				btnStep.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						Animator animator = Animator.getAnimator();
						executeOperation(animator, false);
					}
				});
				btnStep.setBounds(10, 41, 60, 25);
				toolkit.adapt(btnStep, true, true);
				btnStep.setText("Step");
			}
			{
				btnContinue = new Button(buttonGroup, SWT.NONE);
				btnContinue.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						try {
							if (oracle.isPlayback()){
								continuePlayback();
							}else{
								// Continue indefinitely until a non-deterministic choice is reached
								// 		(limited to 20 ticks)
								incTillChoiceOrTime();
							}
						} catch (ProBException e1) {
							e1.printStackTrace();
						}
					}
				});
				btnContinue.setBounds(10, 72, 60, 25);
				toolkit.adapt(btnContinue, true, true);
				btnContinue.setText("Continue");
			}
			{
				count = new Text(buttonGroup, SWT.BORDER);
				count.addFocusListener(new FocusAdapter() {
					@Override
					public void focusLost(FocusEvent e) {
						if (e.widget instanceof Text) {
							String newText = ((Text) e.widget).getText();
							count.setText(newText);
							countField = newText;
							count.pack();
						}
					}
				});
				count.setBounds(76, 12, 72, 21);
				count.setText(countField);
				toolkit.adapt(count, true, true);
			}
			{
				btnStop = new Button(buttonGroup, SWT.NONE);
				btnStop.setBounds(220, 72, 60, 25);
				btnStop.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {				
						if (oracle.isPlayback()){
							oracle.stopPlayback(false);
							statusText = "Recording";
						}
						updateStatusTable();
					}
				});

				toolkit.adapt(btnStop, true, true);
				btnStop.setText("Stop");
			}
			{
				btnRestart = new Button(buttonGroup, SWT.NONE);
				btnRestart.setBounds(154, 10, 60, 25);
				btnRestart.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {							
							if (oracle.isPlayback()){
								oracle.stopPlayback(false);
								oracle.startPlayback(true);
								statusText = "PlayBack";
							}else{
								oracle.stopRecording(false);
								oracle.startRecording();	
								statusText = "Recording";
							}
							updateStatusTable();
							
					}
				});
				toolkit.adapt(btnRestart, true, true);
				btnRestart.setText("Restart");
			}
			{
				btnSave = new Button(buttonGroup, SWT.NONE);
				btnSave.setBounds(220, 10, 60, 25);
				btnSave.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						oracle.saveRecording();
						if (!"Saved".equals(statusText)) oldStatusText = statusText;
						statusText = "Saved";
						updateStatusTable();
					}
				});
				toolkit.adapt(btnSave, true, true);
				btnSave.setText("Save");
			}
			{
				btnReplay = new Button(buttonGroup, SWT.NONE);
				btnReplay.setBounds(220, 41, 60, 25);
				btnReplay.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						if (!oracle.isPlayback()){
							oracle.stopRecording(false);
							oracle.startPlayback(false);
						}
						statusText = "Playback";
						updateStatusTable();
					}
				});
				toolkit.adapt(btnReplay, true, true);
				btnReplay.setText("Replay");
			}
		}

		CBanner banner = new CBanner(container, SWT.NONE);
		fd_operationsTable.bottom = new FormAttachment(100, -10);
		FormData fd_banner = new FormData();
		fd_banner.top = new FormAttachment(0, 290);
		fd_banner.left = new FormAttachment(0, 430);
		banner.setLayoutData(fd_banner);
		toolkit.adapt(banner);
		toolkit.paintBordersFor(banner);
		{
			timeGroup = new Group(container, SWT.BORDER);
			timeGroup.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			fd_timeGroup = new FormData();
			fd_timeGroup.right = new FormAttachment(buttonGroup, 0, SWT.RIGHT);
			fd_timeGroup.top = new FormAttachment(operationsTable, 0, SWT.TOP);
			fd_timeGroup.left = new FormAttachment(buttonGroup, 0, SWT.LEFT);
			timeGroup.setLayoutData(fd_timeGroup);
			toolkit.adapt(timeGroup);
			toolkit.paintBordersFor(timeGroup);
		}
		{
			componentGroup = new Group(container, SWT.BORDER);
			componentGroup.setText("Components");
			componentGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
			fd_componentGroup = new FormData();
			fd_componentGroup.top = new FormAttachment(0, 10);
			fd_componentGroup.left = new FormAttachment(0, 69);
			fd_componentGroup.right = new FormAttachment(buttonGroup, -12);
			componentGroup.setLayoutData(fd_componentGroup);
			toolkit.adapt(componentGroup);
			toolkit.paintBordersFor(componentGroup);
		}
		{
			connectorGroup = new Group(container, SWT.BORDER);
			connectorGroup.setText("Connectors");
			connectorGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
			fd_connectorGroup = new FormData();
			fd_connectorGroup.top = new FormAttachment(componentGroup, 12);
			fd_connectorGroup.left = new FormAttachment(0, 67);
			fd_connectorGroup.right = new FormAttachment(buttonGroup, -12);
			connectorGroup.setLayoutData(fd_connectorGroup);
			toolkit.adapt(connectorGroup);
			toolkit.paintBordersFor(connectorGroup);
		}
	}

	////////////////////////////////////////////
	// implements the step behaviour where we incTime by some ticks
	private void incTime() throws ProBException {	
		if (inSetup()) return;	
		Animator animator = Animator.getAnimator();
		Variable currTimeVar = animator.getCurrentState().getValues().get("current_time");
		if (currTimeVar != null) {
			// how many time ticks to step
			int endTime = Integer.valueOf(count.getText()) + Integer.valueOf(currTimeVar.getValue());
			boolean progress = true;
			while (Integer.valueOf(currTimeVar.getValue()) < endTime && progress) {
				currTimeVar = animator.getCurrentState().getValues().get("current_time");
				progress = executeOperation(animator, false);
			}
		}
	}
	
	private void continuePlayback(){
		if (inSetup()) return;
		boolean progress = true;
		while (progress){
			progress = executeOperation(Animator.getAnimator(),false);
		}
	}

	// implements the continue behaviour where we incTime until a tick or
	// component choice is available
	private void incTillChoiceOrTime() throws ProBException {
		if (inSetup()) return;	
		Animator animator = Animator.getAnimator();
		Variable currTimeVar = animator.getCurrentState().getValues().get("current_time");
		if (currTimeVar != null) {
			int ticks = 20; //limit
			// end time = current time + ticks
			final int endTime = ticks + Integer.valueOf(currTimeVar.getValue());
			boolean progress = true;
			while (Integer.valueOf(currTimeVar.getValue()) < endTime && progress) {
				if (nonDeterministicChoiceInComponent(animator)){
					adviseUser(ticks, currTimeVar, true, progress);
					return;
				}else{
					// if we reach here then no components have multiple ops enabled so we must invoke a step
					currTimeVar = animator.getCurrentState().getValues().get("current_time");
					progress = executeOperation(animator,false);
				}
			}
			//end of time or progress loop
			adviseUser(ticks, currTimeVar, false, progress);
		}
	}

	private boolean inSetup(){
		List<Operation> enabledOperations = Animator.getAnimator().getCurrentState().getEnabledOperations();
		for (Operation op : enabledOperations){
			if ("SETUP_CONTEXT".equals(op.getName()) || "INITIALISATION".equals(op.getName())){
				MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
				mbox.setText("Continue Terminated Message");
				mbox.setMessage("Use Step button to execute SETUP_CONTEXT and INITIALISATION");
				mbox.open();
				return true;
			}
		}
		return false;
	}
	
	private boolean nonDeterministicChoiceInComponent(Animator animator) {
		int foundComponentOpEnabled;
		// if there is a choice of operations then stop the animation
		List<Operation> enabledOps = animator.getCurrentState().getEnabledOperations();
		List<String> enabledOpNames = new ArrayList<String>();
		for (Operation op : enabledOps) {
			enabledOpNames.add(op.getName());
		}
		EList<AbstractExtension> exts = machine.getExtensions();
		// go through each extension and find components and map to their eventNames.
		for (AbstractExtension ext : exts) {
			if (ext instanceof Component) {
				Component topComponent = (Component) ext;
				EList<Component> components = topComponent.getComponents();
				// iterate through components
				for (Component component : components) {
					List<String> evtNames = new ArrayList<String>();					
					foundComponentOpEnabled = 0;
					EList<AbstractComponentOperation> operationsList = component
							.getOperations();
					for (AbstractComponentOperation op : operationsList) {
						if (!(op instanceof External)) {
							EList<Event> elaborates = op.getElaborates();
							for (Event evt : elaborates) {
								evtNames.add(evt.getName());
							}
						}
					}
					// we now have a list of enabled event names for this component
					for (String evtName : evtNames) {
						if (enabledOpNames.contains(evtName)) {
							foundComponentOpEnabled++;
							// if we have more than one enabled component then advise user and return
							if(foundComponentOpEnabled>1){
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void adviseUser(int ticks, Variable currTimeVar, boolean nonDetChoice, boolean progress) {
		MessageBox mbox = new MessageBox(getSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
		mbox.setText("Continue Terminated Message");
		if (progress==false){
			mbox.setMessage("Continue terminated due to lack of progress.");
		}else if (nonDetChoice){
			mbox.setMessage("Continue terminated after reaching non-deterministic choice");	
		}else if (Integer.valueOf(currTimeVar.getValue()) >= ticks){
			mbox.setMessage("Continue terminated after reaching tick limit ("+ticks+").");		
		}else{
			mbox.setMessage("Continue terminated for unknown reason");					
		}
		mbox.open();
	}

	@Override
	protected void stateChanged(final State activeState,
			final Operation operation) {
		UpdateStateLists.getInstance().execute();
		UpdateEnabledOpsList.getInstance().execute();
	}


	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		@SuppressWarnings("unused")
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		@SuppressWarnings("unused")
		IMenuManager manager = getViewSite().getActionBars().getMenuManager();
	}

	
	private static final Random random = new Random();
	private FormData fd_operationsTable;
	private FormData fd_buttonGroup;
	
	private boolean executeOperation(Animator animator, boolean silent){
		State currentState = animator.getCurrentState();	
		List<Operation> ops = currentState.getEnabledOperations();
		if (ops.isEmpty()) {
			return false; // / This is a deadlock
		}
		int nextOp;
		if (oracle.isPlayback()){
			nextOp = oracle.selectNextOperation(animator);
		}else{
			nextOp = random.nextInt(ops.size());
		}
		if (nextOp>=0){
			Operation operation = ops.get(nextOp);
			executeOperation(animator, operation, silent);
			return true;
		}else{
			return false;
		}
	}
	
	private boolean executeOperation(Animator animator, Operation operation, boolean silent){
		try {
			ExecuteOperationCommand.executeOperation(animator, operation, silent);
		} catch (ProBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		if ("Saved".equals(statusText)) statusText = oldStatusText;
		return true;
	}

}
