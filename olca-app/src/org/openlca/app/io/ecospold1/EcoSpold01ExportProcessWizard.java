/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.io.ecospold1;

import org.openlca.core.model.ModelType;

/**
 * Extension of {@link EcoSpold01ExportWizard} for processes
 */
public class EcoSpold01ExportProcessWizard extends EcoSpold01ExportWizard {

	public EcoSpold01ExportProcessWizard() {
		super(ModelType.PROCESS);
	}

}