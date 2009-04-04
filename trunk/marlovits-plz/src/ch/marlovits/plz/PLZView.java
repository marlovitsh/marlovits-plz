package ch.marlovits.plz;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;



public class PLZView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	
	public static final String ID = "ch.marlovits.plz.PLZView";
	
	// command from org.eclipse.ui
	private static final String COMMAND_COPY = "org.eclipse.ui.edit.copy";
	
	private FormToolkit tk;
	private Form form;
	private TableViewer plzViewer;
	
	private Patient actPatient;
	
	private Action exportToClipboardAction;
	
	// column indices
	private static final int NUMBER = 0;
	private static final int DATE = 1;
	private static final int AMOUNT = 2;
	private static final int AMOUNT_DUE = 3;
	private static final int STATUS = 4;
	private static final int GARANT = 5;
	
	private static final String[] COLUMN_TEXT = {
		"Land", // NUMBER
		"Landiso3", // DATE
		"PLZ", // AMOUNT
		"Ort", // AMOUNT_DUE
		"Strasse", // STATUS
		"Kanton", // GARANT
	};
	
	private static final int[] COLUMN_WIDTH = {
		80, // NUMBER
		80, // DATE
		80, // AMOUNT
		80, // AMOUNT_DUE
		80, // STATUS
		80, // GARANT
	};

	private List<ch.marlovits.plz.Plz> getPostleitzahlen(){
		List<ch.marlovits.plz.Plz> postleitzahlen = new ArrayList<ch.marlovits.plz.Plz>(); 		
		Query<ch.marlovits.plz.Plz> query = new Query<ch.marlovits.plz.Plz>(ch.marlovits.plz.Plz.class);
			//query.add("ID" , "not like", "74zfhd333333kfjjdks_fjkd");
			query.insertTrue();
			query.orderBy(false, "ID");
			
			List<ch.marlovits.plz.Plz> plzList = query.execute();
			if (plzList != null) {
				postleitzahlen.addAll(plzList);
			}
		
		/*Collections.sort(postleitzahlen, new Comparator<Rechnung>() {
			// compare on bill number
			public int compare(Rechnung r1, Rechnung r2){
				// both null, consider as equal
				if (r1 == null && r2 == null) {
					return 0;
				}
				
				// r1 is null, r2 not. sort r2 before r1
				if (r1 == null) {
					return 1;
				}
				
				// r2 is null, r1 not. sort r1 before r2
				if (r2 == null) {
					return -1;
				}
				
				// r1 and r2 not null
				String sNumber1 = r1.getNr();
				String sNumber2 = r2.getNr();
				
				try {
					Integer number1 = new Integer(r1.getNr());
					Integer number2 = new Integer(r2.getNr());
					return number1.compareTo(number2);
				} catch (NumberFormatException ex) {
					// error, consider equal
					return 0;
				}
			}
			
			// compare on id
			public boolean equals(Object obj){
				return (this == obj);
			}
		});
		*/
		return postleitzahlen;
	}
	
	public void createPartControl(Composite parent){
		parent.setLayout(new FillLayout());
		tk = Desk.getToolkit();
		form = tk.createForm(parent);
		form.getBody().setLayout(new GridLayout(1, false));
		
		// general infos
		Composite generalArea = tk.createComposite(form.getBody());
		generalArea.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		generalArea.setLayout(new GridLayout(2, false));
		
		// set title for this form, non-changeable
		form.setText("Harrys Postleitzahlen");
		
		// bills
		plzViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = plzViewer.getTable();
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tk.adapt(table);
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// columns
		TableColumn[] tc = new TableColumn[COLUMN_TEXT.length];
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			tc[i] = new TableColumn(table, SWT.NONE);
			tc[i].setText(COLUMN_TEXT[i]);
			tc[i].setWidth(COLUMN_WIDTH[i]);
		}
		
		plzViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement){
				/*if (actPatient == null) {
					return new Object[] {
						"Kein Patient ausgewählt."
					};
				}*/
				// ************************
				return getPostleitzahlen().toArray();
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
			// nothing to do
			}
		});
		plzViewer.setLabelProvider(new ITableLabelProvider() {
			public void addListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void removeListener(ILabelProviderListener listener){
			// nothing to do
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public String getColumnText(Object element, int columnIndex){
				if (!(element instanceof Plz)) {
					return "";
				}
				
				Plz plz = (Plz) element;
				String text = "";
								
				switch (columnIndex) {
				case NUMBER:
					text = plz.getFieldData("Land");
					break;
				case DATE:
					text = plz.getFieldData("LandISO3");
					break;
				case AMOUNT:
					text = plz.getFieldData("Plz");
					break;
				case AMOUNT_DUE:
					text = plz.getFieldData("Ort");
					break;
				case STATUS:
					text = plz.getFieldData("Strasse");
					break;
				case GARANT:
					text = plz.getFieldData("Kanton");
					break;
				}
				
				return text;
			}
			
			public Image getColumnImage(Object element, int columnIndex){
				return null;
			}
			
			public boolean isLabelProperty(Object element, String property){
				return false;
			}
		});
		
		plzViewer.setInput(getViewSite());
		
// +++++		makeActions();
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(exportToClipboardAction);
		GlobalEvents.getInstance().addActivationListener(this, this);
		plzViewer.addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
		
		// Doppelclick öffnen Eingabe-Dialog
		plzViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection sel = (IStructuredSelection) plzViewer.getSelection();
				if (!sel.isEmpty()) {
					Plz plz = (Plz) sel.getFirstElement();
					if (new BuchungsDialog(getSite().getShell(), kbe).open() == Dialog.OK) {
						plzViewer.refresh();
					}
				}
			}
		});
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
		plzViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, this);
		plzViewer.removeSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
		super.dispose();
	}

	/*
	 * SelectionListener methods
	 */

	public void selectionEvent(PersistentObject obj){
		Plz selectedPlz = (Plz) obj;
			plzViewer.refresh();
			//setPatient(selectedPatient);
	}
	
	public void clearEvent(Class template){
		plzViewer.refresh();
		//if (template.equals(Patient.class)) {
		//	setPatient(null);
		//}
	}
	
	/*
	 * ActivationListener
	 */

	public void activation(boolean mode){
	// nothing to do
	}
	
	public void visible(boolean mode){
		if (mode == true) {
			GlobalEvents.getInstance().addSelectionListener(this);
			
			//Patient patient = GlobalEvents.getSelectedPatient();
			//setPatient(patient);
			plzViewer.refresh();
		} else {
			GlobalEvents.getInstance().removeSelectionListener(this);
			//setPatient(null);
			plzViewer.refresh();
		}
	};
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(IProgressMonitor monitor){ /* leer */}
	
	public void doSaveAs(){ /* leer */}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
	/*
	 * class AccountEntry { TimeTool date; Money amount; String remarks;
	 * 
	 * AccountEntry(TimeTool date, Money amount, String remarks) { this.date = date; this.amount =
	 * amount; this.remarks = remarks;
	 * 
	 * if (remarks == null) { remarks = ""; } } }
	 */
/*
	private void makeActions(){
		exportToClipboardAction = new Action("Export (Zwischenablage)") {
			{
				setToolTipText("Zusammenfassung in Zwischenablage kopieren");
			}
			
			public void run(){
				exportToClipboard();
			}
		};
		exportToClipboardAction.setActionDefinitionId(COMMAND_COPY);
		GlobalActions.registerActionHandler(this, exportToClipboardAction);
	}
	

	private void exportToClipboard(){
		String clipboardText = "";
		String lineSeparator = System.getProperty("line.separator");
		
		if (actPatient != null) {
			List<Rechnung> rechnungen = getRechnungen(actPatient);
			StringBuffer sbTable = new StringBuffer();
			StringBuffer sbHeader = new StringBuffer();
			
			sbHeader.append(COLUMN_TEXT[NUMBER]);
			sbHeader.append("\t");
			sbHeader.append(COLUMN_TEXT[DATE]);
			sbHeader.append("\t");
			sbHeader.append(COLUMN_TEXT[AMOUNT]);
			sbHeader.append("\t");
			sbHeader.append(COLUMN_TEXT[AMOUNT_DUE]);
			sbHeader.append("\t");
			sbHeader.append(COLUMN_TEXT[STATUS]);
			sbHeader.append("\t");
			sbHeader.append(COLUMN_TEXT[GARANT]);
			sbHeader.append(lineSeparator);
			sbTable.append(sbHeader);
			
			for (Rechnung rechnung : rechnungen) {
				StringBuffer sbLine = new StringBuffer();
				sbLine.append(rechnung.get("RnNummer"));
				sbLine.append("\t");
				sbLine.append(rechnung.get("RnDatum"));
				sbLine.append("\t");
				sbLine.append(rechnung.getBetrag().toString());
				sbLine.append("\t");
				sbLine.append(rechnung.getOffenerBetrag().toString());
				sbLine.append("\t");
				sbLine.append(RnStatus.getStatusText(rechnung.getStatus()));
				sbLine.append("\t");
				sbLine.append(rechnung.getFall().getGarant().getLabel());
				sbLine.append(lineSeparator);
				sbTable.append(sbLine);
			}
			
			clipboardText = sbTable.toString();
		} else {
			clipboardText = "Keine Rechnungen verfügbar.";
		}
		
		Clipboard clipboard = new Clipboard(Desk.getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] transfers = new Transfer[] {
			textTransfer
		};
		Object[] data = new Object[] {
			clipboardText
		};
		clipboard.setContents(data, transfers);
		clipboard.dispose();
	}
	*/

}
