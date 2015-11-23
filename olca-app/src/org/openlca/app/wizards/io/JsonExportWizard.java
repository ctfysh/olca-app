package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Callback.Message;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.jsonld.ZipStore;
import org.openlca.jsonld.output.JsonExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonExportWizard extends Wizard implements IExportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ModelSelectionPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("@Export data sets");
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		List<ModelType> types = new ArrayList<>();
		for (ModelType type : ModelType.values())
			if (!type.isCategorized())
				continue;
			else if (type == ModelType.PROJECT)
				continue;
			else
				types.add(type);
		page = new ModelSelectionPage(
				types.toArray(new ModelType[types.size()]));
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		IDatabase db = Database.get();
		if (db == null)
			return true;
		List<BaseDescriptor> models = page.getSelectedModels();
		if (models == null || models.isEmpty())
			return true;
		File exportDir = page.getExportDestination();
		if (exportDir == null)
			return false;
		File file = new File(exportDir, db.getName() + ".zip");
		try {
			Export export = new Export(file, models, db);
			getContainer().run(true, true, export);
			return true;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to export data sets", e);
			return false;
		}
	}

	private class Export implements IRunnableWithProgress {

		private File zipFile;
		private List<BaseDescriptor> models;
		private IDatabase database;

		public Export(File zipFile, List<BaseDescriptor> models,
				IDatabase database) {
			this.zipFile = zipFile;
			this.models = models;
			this.database = database;
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.Export, models.size());
			try (ZipStore store = ZipStore.open(zipFile)) {
				doExport(monitor, store);
				monitor.done();
			} catch (Exception e) {
				throw new InvocationTargetException(e);
			}

		}

		private void doExport(IProgressMonitor monitor, ZipStore store) {
			JsonExport export = new JsonExport(database, store);
			for (BaseDescriptor model : models) {
				if (monitor.isCanceled())
					break;
				monitor.subTask(model.getName());
				ModelType type = model.getModelType();
				Class<?> clazz = type.getModelClass();
				Object o = database.createDao(clazz).getForId(model.getId());
				if (o instanceof RootEntity)
					doExport(export, (RootEntity) o);
				monitor.worked(1);
			}
		}

		private void doExport(JsonExport export, RootEntity entity) {
			export.write(entity, (message, data) -> {
				if (message == null)
					return;
				switch (message.type) {
				case Message.INFO:
					log.trace("{}: {}", data, message.text);
					break;
				case Message.WARN:
					log.warn("{}: {}", data, message.text);
					break;
				case Message.ERROR:
					log.error("{}: {}", data, message.text);
					break;
				default:
					break;
				}
			});
		}
	}
}
