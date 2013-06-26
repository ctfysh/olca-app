package org.openlca.app.newwizards;

import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.ImpactMethod;

public class ImpactMethodWizard extends AbstractWizard<ImpactMethod> {

	@Override
	protected String getTitle() {
		return Messages.Methods_WizardTitle;
	}

	@Override
	protected BaseDao<ImpactMethod> createDao() {
		return Database.createDao(ImpactMethod.class);
	}

	@Override
	protected AbstractWizardPage<ImpactMethod> createPage() {
		return new ImpactMethodWizardPage();
	}

}
