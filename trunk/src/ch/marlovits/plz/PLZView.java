package ch.marlovits.plz;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.FileDialog;
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
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Query;
import ch.elexis.importers.ExcelWrapper;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.rgw.tools.ExHandler;

public class PLZView extends ViewPart implements SelectionListener, ActivationListener,
		ISaveablePart2 {
	
	private static final String SRC_ENCODING				= "UTF-8";
	public static final String ID							= "ch.marlovits.plz.PLZView";

	/**
	 * 
	 * Stand per 10.04.2009 für "http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste"
	 * 
	 * Die HTML-Marker, welche den jeweiligen Anfang/Ende von Datenblöcken markieren
	 * 
	 * Start der Tabelle mit den Land-Iso-Codes;
	 * <table class="wikitable sortable" style="vertical-align:top; width:100%;">
	 * Suche nach: <table class="wikitable sortable"
	 * 
	 * Ende der Tabelle mit den Land-Iso-Codes:
	 * </table>
	 * 
	 * dann folgen die Zeilen, jeweils mit Start-Marker <tr> und End-Marker </tr>
	 * 
	 * die erste Zeile ist die Titel-Zeile, wird ausgelassen
	 * 
	 * in jeder Zeile folgen dann jeweils 8 Daten-Zellen
	 */
	private static final String URL_DE						= "http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste";
	private static final String WIKI_LAND_STARTINFO_MARKER	= "<table class=" + "\"" + "wikitable sortable" + "\"";
	private static final String WIKI_LAND_STARTROWS_MARKER	= "<tr>";
	private static final String WIKI_LAND_STARTCELLS_MARKER	= "<td";
	private static final String WIKI_LAND_ENDINFO_MARKER	= "</table>";
	private static final String WIKI_LAND_SKIPDATA_MARKER	= "<td><span style";
	
	private static final String TABLEDATA_STARTMARKER	= "<td>";
	private static final String TABLEDATA_ENDMARKER	= "</td>";
	
	private List<LandIsoEntry> landIsoEntries = new Vector<LandIsoEntry>();
		
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
	private static final int COL_LAND            = 0;
	private static final int COL_LANDISO3        = 1;
	private static final int COL_PLZ             = 2;
	private static final int COL_ORT             = 3;
	private static final int COL_STRASSE         = 4;
	private static final int COL_KANTON          = 5;
	private static final int COL_KANTONKUERZEL   = 6;
	
	private static final String[] COLUMN_TEXT = {
		"Land",
		"LandISO3",
		"PLZ",
		"Ort",
		"Strasse",
		"Kanton",
		"Kantonkuerzel",
	};
	
	private static final String[] DB_COLUMN_TEXT = {
		"Land",
		"LandISO3",
		"Plz",
		"Ort",
		"Strasse",
		"Kanton",
		"Kantonkuerzel",
	};
	
	private static final int[] COLUMN_WIDTH = {
		80, // Land
		80, // LandISO3
		80, // PLZ
		80, // Ort
		80, // Strasse
		80, // Kanton
		80, // Kantonkürzel
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
		
		// Titel setzen
		form.setText("Harrys Postleitzahlen");
		
		// Tabelle erstellen
		plzViewer = new TableViewer(form.getBody(), SWT.SINGLE | SWT.FULL_SELECTION);
		Table table = plzViewer.getTable();
		table.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		tk.adapt(table);
		
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// Tabellen-Überschriften erstellen
		TableColumn[] tc = new TableColumn[COLUMN_TEXT.length];
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			tc[i] = new TableColumn(table, SWT.NONE);
			tc[i].setText(COLUMN_TEXT[i]);
			tc[i].setWidth(COLUMN_WIDTH[i]);
		}
		
		// Tabellen-Inhalt
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
				case COL_KANTONKUERZEL:
					text = plz.getFieldData("Kantonkuerzel");
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
		menu.createViewerContextMenu(plzViewer, newAction, null, copyAction, deleteAction, null, importAction);

		menu.createToolbar(newAction, deleteAction);
		
		GlobalEvents.getInstance().addActivationListener(this, this);
		plzViewer.addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
		
		// Doppelclick öffnen Eingabe-Dialog
		plzViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event){
				IStructuredSelection sel = (IStructuredSelection) plzViewer.getSelection();
				if (!sel.isEmpty()) {
					try {
						tester();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
		//Plz selectedPlz = (Plz) obj;
		//	plzViewer.refresh();
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
	
	private void makeActions(){
		// Alle Postleitzahlen in die Zwischenablage kopieren
		exportToClipboardAction = new Action("Export (Zwischenablage)") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_EXPORT));
				setToolTipText("Alle Postleitzahlen in Zwischenablage kopieren");
			}			
			public void run(){
				exportToClipboard();
			}
		};
		exportToClipboardAction.setActionDefinitionId("exportToClipboardAction");
		GlobalActions.registerActionHandler(this, exportToClipboardAction);
		
		// Ausgewählten PLZ-Eintrag in die Zwischenablage kopieren
		copyAction = new Action("Kopieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_CLIPBOARD));
				setToolTipText("Ausgewählten Datensatz in die Zwischenablage kopieren");
			}
			public void run(){
			//	exportToClipboard();
			}
		};
		copyAction.setActionDefinitionId(COMMAND_COPY);
		GlobalActions.registerActionHandler(this, copyAction);
		
		// Ausgewählten PLZ-Eintrag löschen
		deleteAction = new Action("Löschen...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_DELETE));
				setToolTipText("Ausgewählte Postleitzahl löschen");
			}
			public void run(){
			// TODO
			}
		};
		deleteAction.setActionDefinitionId(COMMAND_DELETE);
		GlobalActions.registerActionHandler(this, deleteAction);
		
		// Erstellen eines neuen PLZ-Eintrages
		newAction = new Action("Neue Postleitzahl...") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_NEW));
				setToolTipText("Neue Postleitzahl erfassen");
			}
			
			public void run(){
//				new PlzDialog(getSite().getShell(), true).open();
				if (new PlzDialog(getSite().getShell()).open() == Dialog.OK) {
					plzViewer.refresh();}
			}
		};
		newAction.setActionDefinitionId("NEUE_POSTLEITZAHL");
		GlobalActions.registerActionHandler(this, newAction);
		
		// Importieren von PLZ-Definitionen aus einer csv-Datei
		importAction = new Action("Importieren...")	{
			{
			setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
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
	
	/**
	 * Exportiert alle Postleitzahlen in die Zwischenablage. 
	 * Format: tab-delimited, die erste Zeile enthält die Feldnamen
	 * @param -
	 * @return -
	 */
	private void exportToClipboard(){
		String clipboardText = "";
		String lineSeparator = System.getProperty("line.separator");
		
		List<Plz> plzs = Plz.getShownPostleitzahlen();
		StringBuffer sbTable = new StringBuffer();
		StringBuffer sbHeader = new StringBuffer();
		
		// Tabellen-Überschriften erstellen
		String delimiter = "";
		for (int i = 0; i < COLUMN_TEXT.length; i++) {
			sbHeader.append(delimiter);
			sbHeader.append(COLUMN_TEXT[i]);
			delimiter = "\t";
		}
		sbHeader.append(lineSeparator);
		sbTable.append(sbHeader);
		
		// Tabellen-Inhalt einlesen
		for (Plz plz : plzs) {
			delimiter = "";
			StringBuffer sbLine = new StringBuffer();
			for (int i = 0; i < COLUMN_TEXT.length; i++) {
				sbLine.append(delimiter);
				sbLine.append(plz.getFieldData(DB_COLUMN_TEXT[i]));
				delimiter = "\t";
			}
			sbLine.append(lineSeparator);
			sbTable.append(sbLine);
		}		
		clipboardText = sbTable.toString();
		
		// Daten ins Clipboard kopieren
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

	public void doImport() 	{
		FileDialog fd = new FileDialog(getSite().getShell(),SWT.OPEN);
		fd.setFilterExtensions(new String[]{"*.csv", "*.xls", "*.*"});
		fd.setFilterNames(new String[]{"Comma Separated Values", "Excel-Dateien", "Alle Dateien"});
		String fileName = fd.open();
		if (fileName == null) {
			return;
		}
		if (fileName.endsWith(".xls")) {
			importExcel(fileName);
		} else if (fileName.endsWith(".csv")) {
			importCSV(fileName);
		} else {
			// TODO
		}
	}
/*
		/////
		FileDialog fd = new FileDialog(getSite().getShell(),SWT.OPEN);
		fd.setFilterExtensions(new String[]{"*.csv", "*.xls", "*.*"});
		fd.setFilterNames(new String[]{"Comma Separated Values", "Excel-Dateien", "Alle Dateien"});
		String fileName = fd.open();
		if (fileName == null) {
			return;
		}
		InputStreamReader isr=new InputStreamReader(new FileInputStream(fileName),SRC_ENCODING);
		CSVReader reader = new CSVReader(isr);
	    String [] line;
		//monitor.subTask("Postleitzahlen einlesen");
	    while ((line = reader.readNext()) != null) {
	    	// TODO hier könnte man noch ggf ; durch , ersetzen (Excel!)
	    	//line = StringTool.convertEncoding(line, SRC_ENCODING);
			new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
			//monitor.worked(1);
		}
		//monitor.done();
		}
*/
	private void importExcel(final String file){
		ExcelWrapper xl = new ExcelWrapper();
		if (!xl.load(file, 0)) {
			return;
		}
		for (int i = xl.getFirstRow(); i <= xl.getLastRow(); i++) {
			List<String> row = xl.getRow(i);
			//importLine(row.toArray(new String[0]));
			String[] line;
			line = row.toArray(new String[0]);
	    	//line = StringTool.convertEncoding(line, SRC_ENCODING);
			new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
		}
		return;
	}
	
	private void importCSV(final String file){
		try {
			CSVReader cr = new CSVReader(new FileReader(file));
			String[] line;
			while ((line = cr.readNext()) != null) {
				//importLine(line);
		    	// TODO hier könnte man noch ggf ; durch , ersetzen (Excel -> csv !)
		    	//line = StringTool.convertEncoding(line, SRC_ENCODING);
				new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
			}
			return;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return;
		}
		
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/***************************************************************************/
	/**
	 * Liest Inhalt einer Web-Abfrage auf www.directories.ch/weisseseiten
	 */
	public static String readContent(final String name)
		throws IOException, MalformedURLException{
		//URL content = getURL(name, geo);
		URL content = new URL(URL_DE);
		
		InputStream input = content.openStream();
		
		StringBuffer sb = new StringBuffer();
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input);
		try {
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return cleanupUmlaute(cleanupText(sb.toString()));
	}

	private static URL getURL(String name, String geo) throws MalformedURLException	{
		name = name.replace(' ', '+');
		geo = geo.replace(' ', '+');
		
		String urlPattern = "http://tel.local.ch/{0}/q/?what={1}&where={2}"; //$NON-NLS-1$

		return new URL(MessageFormat.format(urlPattern, new Object[] {
				Locale.getDefault().getLanguage(), name, geo
//		return new URL(MessageFormat.format(urlPattern, new Object[] {
//				Locale.getDefault().getLanguage(), name, geo
			}));
		}

	private static String cleanupText(String text){
		text = text.replace("</nobr>", "").replace("<nobr>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		text = text.replace("&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("<b class=\"searchWords\">", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("</b>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace((char) 160, ' '); // Spezielles Blank Zeichen wird
												// ersetzt
		return text;
	}
	
	private static String cleanupUmlaute(String text) {
		text = text.replace("&#xE4;", "ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xC4;", "Ä");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xF6;", "ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xD6;", "Ö");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xFC;", "ü");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xDC;", "Ü");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE8;", "è");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xE9;", "é");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("&#xEA;", "ê");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xE0;", "à");//$NON-NLS-1$ //$NON-NLS-2$
		
		text = text.replace("&#xA0;", " ");//$NON-NLS-1$ //$NON-NLS-2$
		text = text.replace("%C3%9F", "ss");//$NON-NLS-1$ //$NON-NLS-2$
		
		return text;
    }
	
	private void tester() throws MalformedURLException, IOException	{
		String wholeHTMLPage = readContent(URL_DE);
//		System.out.print(tmp222);
		
		// Tabellen-Inhalt aus Text extrahieren
		int tableStartPos			= wholeHTMLPage.indexOf(WIKI_LAND_STARTINFO_MARKER, 0);
		int tableContentStartPos	= wholeHTMLPage.indexOf(WIKI_LAND_STARTROWS_MARKER, tableStartPos);
		int tableDataStartPos		= wholeHTMLPage.indexOf(WIKI_LAND_STARTROWS_MARKER, tableStartPos);
		int tableEndPos				= wholeHTMLPage.indexOf(WIKI_LAND_ENDINFO_MARKER,   tableDataStartPos);
		String landTableContent 	= wholeHTMLPage.substring(tableContentStartPos, tableEndPos);
		//System.out.print(landTablePart);
		
		// durch die Tabelle loopen und die einzelnen Datensätze einlesen
		int currRowPos	= 0;
		int nextRowPos	= 0;
		String rowData = "";
		while (currRowPos != -1)	{
			nextRowPos = landTableContent.indexOf(WIKI_LAND_STARTROWS_MARKER, currRowPos + WIKI_LAND_STARTROWS_MARKER.length());
			rowData = landTableContent.substring(currRowPos, nextRowPos);
			currRowPos = nextRowPos;
			//System.out.print(rowData);
			exctractRowData(rowData);
			//System.out.print(landTableContent);
		}
	}

	private void exctractRowData(final String rowData)	{
		// erste Zelle enthält den Namen des Landes und dessen Wikipedia-Link
		int currDataPos	= 0;
		int nextDataPos	= 0;
		String cellData;
		
		// wenn kein <td vorhanden ist, dann ist es wohl ein header - skip
		currDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		if (currDataPos == -1)	{
			return;
		}
		
		// Land
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = rowData.substring(currDataPos, nextDataPos);
		
		// wenn die erste Zelle den WIKI_LAND_SKIPDATA_MARKER enthält, dann überspringen
		String theString = cellData.substring(0, WIKI_LAND_SKIPDATA_MARKER.length());
		if (theString.equals(WIKI_LAND_SKIPDATA_MARKER))	{
			return;
		}
		
		System.out.println("****************************************************************");
		
		cellData = extractCellData(cellData);
		String[] linkAndText;
		
		linkAndText = splitHyperlinkCell(cellData);
		System.out.println(linkAndText[0]);	// link
		System.out.println(linkAndText[1]);	// text
				
		currDataPos = nextDataPos;
		
		// Alpha 2
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		myOutput(cellData, 2);
		currDataPos = nextDataPos;
		
		// Alpha 3
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		myOutput(cellData, 3);
		currDataPos = nextDataPos;

		// Numeric
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		myOutput(cellData, 3);
		currDataPos = nextDataPos;

		// TopLevelDomain
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		myOutput(cellData, 3);
		currDataPos = nextDataPos;

		// IOC
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));
		myOutput(cellData, 3);
		currDataPos = nextDataPos;

		// ISO3166-2
		nextDataPos = rowData.indexOf(WIKI_LAND_STARTCELLS_MARKER, currDataPos + WIKI_LAND_STARTCELLS_MARKER.length());
		cellData = extractCellData(rowData.substring(currDataPos, nextDataPos));

		linkAndText = splitHyperlinkCell(cellData);
		System.out.println(linkAndText[0]);	// link
		String linkText = linkAndText[1];
		myOutput(linkText, 2);
		//System.out.println(cellData);
		currDataPos = nextDataPos;

	}
	
	private String extractCellData(final String cellDataWithEnclosings)	{
		String cellContents = null;
		
		// falls leer
		if ((cellDataWithEnclosings == null) || (cellDataWithEnclosings == ""))	{
			return cellContents;
		}
		int contentStart = cellDataWithEnclosings.indexOf(TABLEDATA_STARTMARKER, 0);
		if (contentStart != -1)	{
			// has <td>[Content]</td>
			int contentEnd	= cellDataWithEnclosings.indexOf(TABLEDATA_ENDMARKER, TABLEDATA_STARTMARKER.length());
			cellContents = cellDataWithEnclosings.substring(contentStart + TABLEDATA_STARTMARKER.length(), contentEnd);
		} else	{
			// has <td [Content] /td>
			// TO DO
			cellContents = "";
		}
		return cellContents;
	}

	private String[] splitHyperlinkCell(final String cellData)	{
		String linkPart = "";
		String textPart = "";
		
		// bei mehreren links ist der erste der Landes-Link, die restlichen werden ignoriert
		final String hrefStartMarker	= "<a href=\"";
		final String hrefEndMarker		= "\">";
		
		int hrefStart = cellData.indexOf(hrefStartMarker, 0);
		
		// es gibt keinen Link
		if (hrefStart == -1){
			linkPart = "";
			textPart = cellData;
			return new String[] {linkPart, textPart};
		}
		
		// Teil links des href ist Text-Teil
		textPart = textPart + cellData.substring(0, hrefStart);
		
		// href extrahieren
		int hrefEnd = cellData.indexOf(hrefEndMarker, hrefStart) + hrefEndMarker.length();
		String hrefPart = cellData.substring(hrefStart, hrefEnd);
		
		// Link aus href extrahieren
		linkPart = extractHrefLink(hrefPart);
		
		// Text rechts des hrefs gehört zum Text-Teil, es müssen alle </a> entfernt werden
		textPart = textPart + cellData.substring(hrefEnd, cellData.length()).replace("</a>", "");
		
		// jetzt werden noch weitere Links entfernt, werden ignoriert
		// jetzt werden alle weiteren <x> entfernt
        Pattern pattern = Pattern.compile("<.*>");
        //Pattern pattern = Pattern.compile("<a href=\".*\">");
        Matcher matcher = pattern.matcher(textPart);
        textPart = matcher.replaceAll("");
        textPart = textPart.replace("\n\r", " ");
        textPart = textPart.replace("\r", " ");
        textPart = textPart.replace("\n", " ");
        
		// Rückgabe
		return new String[] {linkPart, textPart};
	}
	
	private String extractHrefLink(final String href)	{
		final String hrefLinkStartMarker	= "<a href=\"";
		final String hrefLinkEndMarker	= "\"";
		
		int hrefLinkStart = href.indexOf(hrefLinkStartMarker,	0);
		if (hrefLinkStart == -1)	{
			return "";
		}
		hrefLinkStart =  + hrefLinkStartMarker.length();
		int hrefLinkEnd = href.indexOf(hrefLinkEndMarker, hrefLinkStart);
		
		return href.substring(hrefLinkStart, hrefLinkEnd);
	}
	
	private void myOutput(final String str, final int len){
		if ((str == null) || (str.equals("")))	{
			System.out.println("");
		} else	{
			System.out.println(str.substring(0, len));
		}
	}
}