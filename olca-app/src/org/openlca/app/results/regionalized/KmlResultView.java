package org.openlca.app.results.regionalized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.html.HtmlPage;
import org.openlca.app.rcp.html.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.geo.RegionalizedResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

class KmlResultView extends FormPage implements HtmlPage {

	Logger log = LoggerFactory.getLogger(getClass());

	RegionalizedResultProvider result;
	Browser browser;
	private FlowImpactSelection flowImpactSelection;

	public KmlResultView(FormEditor editor, RegionalizedResultProvider result) {
		super(editor, "KmlResultView", "Result map");
		this.result = result;
	}

	@Override
	public String getUrl() {
		return HtmlView.KML_RESULT_VIEW.getUrl();
	}

	@Override
	public void onLoaded() {
		Set<FlowDescriptor> flowDescriptors = result.getRegionalizedResult()
				.getFlowDescriptors();
		if (flowDescriptors.isEmpty())
			return;
		FlowDescriptor flow = flowDescriptors.iterator().next();
		flowImpactSelection.selectWithEvent(flow);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, "Result map");
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		flowImpactSelection = FlowImpactSelection
				.on(result.getRegionalizedResult(), Cache.getEntityCache())
				.withEventHandler(new KmlSelectionHandler(result))
				.create(composite, toolkit);
		browser = UI.createBrowser(body, this);
		UI.gridData(browser, true, true);
		form.reflow(true);
	}

	private class KmlSelectionHandler extends SelectionHandler {

		private KmlSelectionHandler(RegionalizedResultProvider result) {
			super(result);
		}

		@Override
		protected void processResultData(List<LocationResult> results) {
			double maximum = getMaximum(results);
			evaluate("initData(" + maximum + ")");
			for (LocationResult result : results)
				sendToView(result);
		}

		private double getMaximum(List<LocationResult> results) {
			Double maximum = null;
			for (LocationResult result : results)
				if (maximum == null)
					maximum = result.getTotalAmount();
				else
					maximum = Math.max(maximum, result.getTotalAmount());
			if (maximum == null)
				return 0;
			return maximum;
		}

		private void sendToView(LocationResult result) {
			if (result == null)
				return;
			if (result.getTotalAmount() == 0d)
				return;
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("kml", result.getKmlFeature().getKml());
			item.put("amount", result.getTotalAmount());
			App.runInUI("Setting item", () -> evaluate(item));
		}

		private void evaluate(Object value) {
			String command = null;
			if (value instanceof String)
				command = value.toString();
			else {
				Gson gson = new Gson();
				String json = gson.toJson(value);
				command = "addFeature(" + json + ")";
			}
			try {
				browser.evaluate(command);
			} catch (Exception e) {
				log.error("failed to evaluate " + value, e);
			}
		}
	}

}
