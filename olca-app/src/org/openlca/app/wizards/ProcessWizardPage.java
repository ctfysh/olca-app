package org.openlca.app.wizards;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.NavigationRoot;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.navigation.filters.FlowTypeFilter;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessWizardPage extends AbstractWizardPage<Process> {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Composite contentStack;
	private Button createRefFlowCheck;
	private Composite labelStack;
	private TreeViewer productViewer;
	private FlowPropertyViewer flowPropertyViewer;
	private Label selectFlowLabel;
	private Label selectFlowPropertyLabel;
	private Composite flowPropertyViewerContainer;
	private Composite productViewerContainer;

	protected ProcessWizardPage() {
		super("ProcessWizardPage");
		setTitle(Messages.Processes_WizardTitle);
		setMessage(Messages.Processes_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PROCESS.getDescriptor());
		setPageComplete(false);
	}

	private void initListeners() {

		createRefFlowCheck.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean createFlow = createRefFlowCheck.getSelection();
				StackLayout labelLayout = (StackLayout) labelStack.getLayout();
				StackLayout contentLayout = (StackLayout) contentStack
						.getLayout();
				if (createFlow) {
					labelLayout.topControl = selectFlowPropertyLabel;
					contentLayout.topControl = flowPropertyViewerContainer;
				} else {
					labelLayout.topControl = selectFlowLabel;
					contentLayout.topControl = productViewerContainer;
				}
				labelStack.layout();
				contentStack.layout();
				checkInput();
			}
		});

		productViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						checkInput();
					}
				});

	}

	private void setData() {
		NavigationRoot root = Navigator.getNavigationRoot();
		if (root != null)
			productViewer.setInput(root);
		flowPropertyViewer.setInput(Database.get());
		flowPropertyViewer.selectFirst();
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		boolean createFlow = createRefFlowCheck.getSelection();
		String err = Messages.Processes_EmptyQuantitativeReferenceError;
		if (createFlow) {
			if (flowPropertyViewer.getSelected() == null)
				setErrorMessage(err);
		} else {
			Flow flow = getSelectedFlow();
			if (flow == null)
				setErrorMessage(err);
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(Composite container) {
		new Label(container, SWT.NONE);
		createRefFlowCheck = new Button(container, SWT.CHECK);
		createRefFlowCheck.setText(Messages.Processes_CreateProductFlow);

		labelStack = new Composite(container, SWT.NONE);
		labelStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelStack.setLayout(new StackLayout());

		contentStack = new Composite(container, SWT.NONE);
		UI.gridData(contentStack, true, true).heightHint = 200;
		contentStack.setLayout(new StackLayout());

		selectFlowLabel = new Label(labelStack, SWT.NONE);
		selectFlowLabel.setText(Messages.Common_QuantitativeReference);

		createProductViewer();

		selectFlowPropertyLabel = new Label(labelStack, SWT.NONE);
		selectFlowPropertyLabel.setText(Messages.Flows_ReferenceFlowProperty);

		// create combo viewer for selecting a reference flow property
		flowPropertyViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(flowPropertyViewerContainer, true, false);
		flowPropertyViewerContainer.setLayout(gridLayout());
		flowPropertyViewer = new FlowPropertyViewer(flowPropertyViewerContainer);

		setData();
		initListeners();

		((StackLayout) labelStack.getLayout()).topControl = selectFlowLabel;
		((StackLayout) contentStack.getLayout()).topControl = productViewerContainer;
		labelStack.layout();
		contentStack.layout();
	}

	private GridLayout gridLayout() {
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		return layout;
	}

	private void createProductViewer() {
		log.trace("start initialise product viewer");
		productViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(productViewerContainer, true, false);
		productViewerContainer.setLayout(gridLayout());
		productViewer = NavigationTree.createViewer(productViewerContainer);
		productViewer.setInput(Navigator.getNavigationRoot()); // TODO: only
																// processes
		UI.gridData(productViewer.getTree(), true, true).heightHint = 200;
		productViewer.addFilter(new FlowTypeFilter(FlowType.ELEMENTARY_FLOW,
				FlowType.WASTE_FLOW));
		productViewer.addFilter(new EmptyCategoryFilter());
		log.trace("product viewer initialised");
	}

	@Override
	public Process createModel() {
		ProcessCreationController controller = new ProcessCreationController(
				Database.get());
		controller.setName(getModelName());
		controller.setCreateWithProduct(createRefFlowCheck.getSelection());
		controller.setDescription(getModelDescription());
		Flow flow = getSelectedFlow();
		if (flow != null)
			controller.setFlow(Descriptors.toDescriptor(flow));
		controller.setFlowProperty(flowPropertyViewer.getSelected());
		return controller.create();
	}

	private Flow getSelectedFlow() {
		INavigationElement<?> e = Viewers.getFirstSelected(productViewer);
		if (e == null || !(e.getContent() instanceof Flow))
			return null;
		return (Flow) e.getContent();
	}
}