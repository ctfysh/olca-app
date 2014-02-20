package org.openlca.app.editors.processes;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.ParameterSection;
import org.openlca.app.editors.ParameterSectionInput;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessParameterPage extends ModelPage<Process> {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ProcessEditor editor;

	ProcessParameterPage(ProcessEditor editor) {
		super(editor, "ProcessParameterPage", Messages.ParametersPageLabel);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Process + ": "
				+ getModel().getName());
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		try {
			createGlobalParamterSection(body);
			ParameterSectionInput input = new ParameterSectionInput();
			input.setComposite(body);
			input.setEditor(editor);
			input.setInterpreter(editor.getInterpreter());
			input.setParameters(getModel().getParameters());
			input.setScope(ParameterScope.PROCESS);
			ParameterSection.forInputParameters(input);
			ParameterSection.forDependentParameters(input);
			body.setFocus();
			form.reflow(true);
		} catch (Exception e) {
			log.error("failed to create parameter tables", e);
		}
	}

	private void createGlobalParamterSection(Composite body) {
		Section section = UI.section(body, toolkit, "Global parameters");
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridLayout(client, 1);
		String[] columns = { Messages.Name, Messages.Value,
				Messages.Uncertainty, Messages.Description };
		TableViewer table = Tables.createViewer(client, columns);
		table.setLabelProvider(new ParameterLabel());
		IDatabase database = Database.get();
		ParameterDao dao = new ParameterDao(database);
		List<Parameter> params = dao.getGlobalParameters();
		Collections.sort(params, new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {
				return Strings.compare(o1.getName(), o2.getName());
			}
		});
		table.setInput(params);
		Tables.bindColumnWidths(table.getTable(), 0.4, 0.3);
		section.setExpanded(false);
	}

	private class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			switch (columnIndex) {
			case 0:
				return parameter.getName();
			case 1:
				return Double.toString(parameter.getValue());
			case 2:
				return UncertaintyLabel.get(parameter.getUncertainty());
			case 3:
				return parameter.getDescription();
			default:
				return null;
			}
		}
	}

}