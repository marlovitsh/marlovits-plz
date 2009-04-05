package ch.marlovits.plz;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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

import au.com.bytecode.opencsv.CSVReader;
import ch.elexis.Desk;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.commands.Handler;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;



public class PLZView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	
	public static final String ID = "ch.marlovits.plz.PLZView";
	
	// command from org.eclipse.ui
	private static final String COMMAND_COPY   = "org.eclipse.ui.edit.copy";
	private static final String COMMAND_DELETE = "org.eclipse.ui.edit.delete";
	
	private FormToolkit tk;
	private Form form;
	private TableViewer plzViewer;
		
	private Action exportToClipboardAction;
	private Action copyAction;
	private Action deleteAction;
	private Action newAction;
	private Action importAction;
	
	// column indices
	private static final int COL_ID       = 0;
	private static final int COL_LAND     = 1;
	private static final int COL_LANDISO3 = 2;
	private static final int COL_PLZ      = 3;
	private static final int COL_ORT      = 4;
	private static final int COL_STRASSE  = 5;
	private static final int COL_KANTON   = 6;
	
	private static final String[] COLUMN_TEXT = {
		"ID",
		"Land",
		"LandISO3",
		"PLZ",
		"Ort",
		"Strasse",
		"Kanton",
	};
	
	private static final int[] COLUMN_WIDTH = {
		0, // ID
		80, // Land
		80, // LandISO3
		80, // PLZ
		80, // Ort
		80, // Strasse
		80, // Kanton
	};

	private List<ch.marlovits.plz.Plz> getPostleitzahlen(){
		// Erstellen des Return-Arrays
		List<ch.marlovits.plz.Plz> postleitzahlen = new ArrayList<ch.marlovits.plz.Plz>(); 		
		
		// Erstellen einer Query auf Plz und alle Datensätze einlesen, sortieren nach ID
		Query<ch.marlovits.plz.Plz> query = new Query<ch.marlovits.plz.Plz>(ch.marlovits.plz.Plz.class);
		query.insertTrue();
		query.orderBy(false, "ID");
		List<ch.marlovits.plz.Plz> plzList = query.execute();
		
		// Die aus der Datenbank eingelesenen Werte in den Return-Array schreiben
		if (plzList != null) {
			postleitzahlen.addAll(plzList);
		}
		
		// Sortieren der Daten
		Collections.sort(postleitzahlen, new Comparator<Plz>() {
			// Anfägliche Sortierung nach ID
			public int compare(Plz plz1, Plz plz2){
				// beide gleich gibt es nicht
				if (plz1 == null && plz2 == null) {
					return 0;
				}
				// plz1 ist null, plz2 nicht. setze plz2 vor plz1
				if (plz1 == null) {
					return 1;
				}
				
				// plz2 ist null, plz1 nicht. setze plz1 vor plz2
				if (plz2 == null) {
					return -1;
				}
				
				// beide nicht null
				//String sNumber1 = plz1.get("ID");
				//String sNumber2 = plz2.get("ID");
				
				try {
					Integer number1 = new Integer(plz1.get("ID"));
					Integer number2 = new Integer(plz2.get("ID"));
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
				case COL_ID:
					text = plz.getFieldData("ID");
					break;
				case COL_LAND:
					text = plz.getFieldData("Land");
					break;
				case COL_LANDISO3:
					text = plz.getFieldData("LandISO3");
					break;
				case COL_PLZ:
					text = plz.getFieldData("Plz");
					break;
				case COL_ORT:
					text = plz.getFieldData("Ort");
					break;
				case COL_STRASSE:
					text = plz.getFieldData("Strasse");
					break;
				case COL_KANTON:
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
		
		// Erstellen der Actions für die Menus, etc
		makeActions();
		
		// Erstellen des ViewMenus
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(exportToClipboardAction, null, copyAction);
		
		// Erstellen des KontextMenus
		menu.createViewerContextMenu(plzViewer, copyAction, deleteAction, newAction, null, importAction);

		
		GlobalEvents.getInstance().addActivationListener(this, this);
		plzViewer.addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
		
		// Doppelclick öffnen Eingabe-Dialog
		plzViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection sel = (IStructuredSelection) plzViewer.getSelection();
				if (!sel.isEmpty()) {
					Plz plz = (Plz) sel.getFirstElement();
					if (new PlzDialog(getSite().getShell(), plz).open() == Dialog.OK) {
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

	private void makeActions(){
		exportToClipboardAction = new Action("Export (Zwischenablage)") {
			{
				setToolTipText("Zusammenfassung in Zwischenablage kopieren");
			}
			
			public void run(){
				exportToClipboard();
			}
		};
		exportToClipboardAction.setActionDefinitionId("exportToClipboardAction");
		GlobalActions.registerActionHandler(this, exportToClipboardAction);
		
		copyAction = new Action("Kopieren") {
			{
				setToolTipText("Ausgewählten Datensatz in die Zwischenablage kopieren");
			}
			
			public void run(){
			//	exportToClipboard();
			}
		};
		copyAction.setActionDefinitionId(COMMAND_COPY);
		GlobalActions.registerActionHandler(this, copyAction);
		
		deleteAction = new Action("Löschen...") {
			{
				setToolTipText("Ausgewählten Datensatz löschen");
			}
			
			public void run(){
			//	exportToClipboard();
			}
		};
		deleteAction.setActionDefinitionId(COMMAND_DELETE);
		GlobalActions.registerActionHandler(this, deleteAction);
		
		newAction = new Action("Neue Postleitzahl...") {
			{
				setToolTipText("Neue Postleitzahl erfassen");
			}
			
			public void run(){
				exportToClipboard();
			}
		};
		newAction.setActionDefinitionId("NEUE_POSTLEITZAHL");
		GlobalActions.registerActionHandler(this, newAction);
		
		importAction = new Action("Importieren...")	{
			{
			setToolTipText("Importieren von Postleitzahlen aus Dateien");
			}
			public void run(){
				try {
					doImport();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		newAction.setActionDefinitionId("PLZ_IMPORT");
		GlobalActions.registerActionHandler(this, importAction);
	}
	
public void doImport() throws Exception{
	String[] results = {"", "", "", "", ""};
//	IProgressMonitor monitor = Handler.getMonitor(eev);;
	
	CSVReader reader = new CSVReader(new FileReader(results[0]), ';');
//	monitor.beginTask("Importiere Postleitzahlen", 100);
	String[] line = reader.readNext();
	while ((line = reader.readNext()) != null) {
		if (line.length < 3) {
			continue;
		}
//		monitor.subTask(line[1]);
		String id =
			new Query<Plz>(Plz.class).findSingle("Ziffer", "=", line[0]);
		if (id != null) {
			Plz pl = Plz.load(id);
			pl.set(new String[] {
				"Titel", "TP"
			}, line[1], line[2]);
		} else {
			/* Plz pl = */new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
		}
	}
//	monitor.done();
}

	/**
	 * Exportiert alle angezeigten Postleitzahlen in die Zwischenablage.
	 * @param -
	 * @return -
	 */
	private void exportToClipboard(){
		String clipboardText = "";
		String lineSeparator = System.getProperty("line.separator");
		
		List<Plz> plzs = Plz.getShownPostleitzahlen();
		StringBuffer sbTable = new StringBuffer();
		StringBuffer sbHeader = new StringBuffer();
		
		sbHeader.append(COLUMN_TEXT[COL_LAND]);
		sbHeader.append("\t");
		sbHeader.append(COLUMN_TEXT[COL_LANDISO3]);
		sbHeader.append("\t");
		sbHeader.append(COLUMN_TEXT[COL_PLZ]);
		sbHeader.append("\t");
		sbHeader.append(COLUMN_TEXT[COL_ORT]);
		sbHeader.append("\t");
		sbHeader.append(COLUMN_TEXT[COL_STRASSE]);
		sbHeader.append("\t");
		sbHeader.append(COLUMN_TEXT[COL_KANTON]);
		sbHeader.append(lineSeparator);
		sbTable.append(sbHeader);
		
		for (Plz plz : plzs) {
			StringBuffer sbLine = new StringBuffer();
			sbLine.append(plz.getFieldData("Land"));
			sbLine.append("\t");
			sbLine.append(plz.getFieldData("LandISO3"));
			sbLine.append("\t");
			sbLine.append(plz.getFieldData("Plz"));
			sbLine.append("\t");
			sbLine.append(plz.getFieldData("Ort"));
			sbLine.append("\t");
			sbLine.append(plz.getFieldData("Strasse"));
			sbLine.append("\t");
			sbLine.append(plz.getFieldData("Kanton"));
			sbLine.append(lineSeparator);
			sbTable.append(sbLine);
		}
		
		clipboardText = sbTable.toString();
		
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
