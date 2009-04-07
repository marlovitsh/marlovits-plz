package ch.marlovits.plz;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.actions.GlobalActions;


public class PLZDetailView extends ViewPart implements ISaveablePart2 {
	public static final String ID="ch.marlovits.plz.PLZDetailView";
	PLZBlatt kb;
	
	public PLZDetailView() {
	
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
//+++++		kb=new PLZBlatt(parent, SWT.NONE, getViewSite());
	}

	@Override
	public void setFocus() {
		kb.setFocus();
	}
	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2
	 * Wir benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */ 
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}
	public void doSave(IProgressMonitor monitor) { /* leer */ }
	public void doSaveAs() { /* leer */}
	public boolean isDirty() {
		return true;
	}
	public boolean isSaveAsAllowed() {
		return false;
	}
	public boolean isSaveOnCloseNeeded() {
		return true;
	}
}
