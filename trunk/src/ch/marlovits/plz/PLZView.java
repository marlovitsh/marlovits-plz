package ch.marlovits.plz;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.data.Zahlung;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.Money;



public class PLZView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	
	public static final String ID = "ch.marlovits.plz.PLZView";
	
	// command from org.eclipse.ui
	private static final String COMMAND_COPY = "org.eclipse.ui.edit.copy";
	
	private FormToolkit tk;
	private Form form;
	private Label totalLabel;
	private Label paidLabel;
	private Label openLabel;
	private TableViewer billsViewer;
	
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
		"Nummer", // NUMBER
		"Datum", // DATE
		"Betrag", // AMOUNT
		"Offen", // AMOUNT_DUE
		"Status", // STATUS
		"Rechnungsempfänger", // GARANT
	};
	
	private static final int[] COLUMN_WIDTH = {
		80, // NUMBER
		80, // DATE
		80, // AMOUNT
		80, // AMOUNT_DUE
		80, // STATUS
		80, // GARANT
	};
	
	private List<Rechnung> getRechnungen(Patient patient){
		List<Rechnung> rechnungen = patient.getRechnungen();
		Collections.sort(rechnungen, new Comparator<Rechnung>() {
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
		
		return rechnungen;
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
		
		tk.createLabel(generalArea, "Total:");
		totalLabel = tk.createLabel(generalArea, "");
		totalLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		tk.createLabel(generalArea, "Bezahlt:");
		paidLabel = tk.createLabel(generalArea, "");
		paidLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		tk.createLabel(generalArea, "Offen:");
		openLabel = tk.createLabel(generalArea, "");
		openLabel.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// bills
		billsViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = billsViewer.getTable();
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
		
		billsViewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement){
				if (actPatient == null) {
					return new Object[] {
						"Kein Patient ausgewählt."
					};
				}
				
				return getRechnungen(actPatient).toArray();
			}
			
			public void dispose(){
			// nothing to do
			}
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
			// nothing to do
			}
		});
		billsViewer.setLabelProvider(new ITableLabelProvider() {
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
				if (!(element instanceof Rechnung)) {
					return "";
				}
				
				Rechnung rechnung = (Rechnung) element;
				String text = "";
				
				switch (columnIndex) {
				case NUMBER:
					text = "die rechnungs-nummer"; //rechnung.get("RnNummer");
					break;
				case DATE:
					text = "das rechnungsdatum"; //rechnung.get("RnDatum");
					break;
				case AMOUNT:
					text = "und der betrag"; // rechnung.getBetrag().toString();
					break;
				case AMOUNT_DUE:
					text = "fehlt noch"; // rechnung.getOffenerBetrag().toString();
					break;
				case STATUS:
					text = "Rn Status..."; //RnStatus.getStatusText(rechnung.getStatus());
					break;
				case GARANT:
					text = "der garant"; // rechnung.getFall().getGarant().getLabel();
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
		
		billsViewer.setInput(getViewSite());
		
		makeActions();
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(exportToClipboardAction);
		GlobalEvents.getInstance().addActivationListener(this, this);
		billsViewer.addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
		billsViewer.getControl().setFocus();
	}
	
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, this);
		billsViewer.removeSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
		super.dispose();
	}
	
	private void setPatient(Patient patient){
		actPatient = patient;
		
		String title = "";
		if (actPatient != null) {
			title = actPatient.getLabel();
		} else {
			title = "Kein Patient ausgewählt";
		}
		form.setText(title);
		
		setGeneralText();
		billsViewer.refresh();
		
		form.layout();
	}
	
	// maybe called from foreign thread
	private void setGeneralText(){
		// check wheter the labels are valid, since we may be called
		// from a different thread
		if (totalLabel.isDisposed() || paidLabel.isDisposed() || openLabel.isDisposed()) {
			return;
		}
		
		String totalText = "";
		String paidText = "";
		String openText = "";
		
		if (actPatient != null) {
			Money total = new Money(0);
			Money paid = new Money(0);
			
			List<Rechnung> rechnungen = actPatient.getRechnungen();
			for (Rechnung rechnung : rechnungen) {
				// don't consider canceled bills
				if (rechnung.getStatus() != RnStatus.STORNIERT) {
					total.addMoney(rechnung.getBetrag());
					for (Zahlung zahlung : rechnung.getZahlungen()) {
						paid.addMoney(zahlung.getBetrag());
					}
				}
			}
			
			Money open = new Money(total);
			open.subtractMoney(paid);
			
			totalText = total.toString();
			paidText = paid.toString();
			openText = open.toString();
		}
		
		totalLabel.setText(totalText);
		paidLabel.setText(paidText);
		openLabel.setText(openText);
	}
	
	/*
	 * SelectionListener methods
	 */

	public void selectionEvent(PersistentObject obj){
		if (obj instanceof Patient) {
			Patient selectedPatient = (Patient) obj;
			
			setPatient(selectedPatient);
		}
	}
	
	public void clearEvent(Class template){
		if (template.equals(Patient.class)) {
			setPatient(null);
		}
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
			
			Patient patient = GlobalEvents.getSelectedPatient();
			setPatient(patient);
		} else {
			GlobalEvents.getInstance().removeSelectionListener(this);
			
			setPatient(null);
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
	
}
