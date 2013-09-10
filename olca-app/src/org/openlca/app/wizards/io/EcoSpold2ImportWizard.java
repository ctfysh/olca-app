package org.openlca.app.wizards.io;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.resources.ImageType;
import org.openlca.core.database.IDatabase;
import org.openlca.io.ecospold2.EcoSpold2Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import wizard for files in the EcoSpold format version 2.
 */
public class EcoSpold2ImportWizard extends Wizard implements IImportWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private FileImportPage importPage;

	public EcoSpold2ImportWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import EcoSpold 02 data sets");
		setDefaultPageImageDescriptor(ImageType.IMPORT_ZIP_WIZARD
				.getDescriptor());
	}

	@Override
	public boolean performFinish() {
		File[] files = importPage.getFiles();
		IDatabase database = Database.get();
		if (files == null || files.length == 0 || database == null)
			return false;
		final EcoSpold2Import pi = new EcoSpold2Import(database);
		pi.setFiles(files);
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Import", IProgressMonitor.UNKNOWN);
					ImportHandler handler = new ImportHandler(monitor);
					handler.run(pi);
				}
			});
			Navigator.refresh();
		} catch (Exception e) {
			log.error("EcoSpold 02 import failed", e);
		}
		return true;
	}

	@Override
	public void addPages() {
		importPage = new FileImportPage(new String[] { "zip", "spold", "xml" },
				false);
		addPage(importPage);
	}

}