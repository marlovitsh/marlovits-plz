package ch.marlovits.plz;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.AttributedCharacterIterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalActions;
import ch.elexis.commands.Handler;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.ViewMenus;
import ch.marlovits.plz.MCCombo.MCComboDataProvider;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.JdbcLink.Stm;

public class PlzTesting extends ViewPart implements ISaveablePart2 {
	public  static final String ID = "ch.marlovits.plz.PLZView";
	
	// die Felder auf der ViewPart
	private Composite		top;
	private Composite		rightComposite;
	private GridLayout		tmpGrid;
	private Combo			cbLandCombo;
	private Text			landIso2Field;
	private ModifyListener	landModifyListener;
	private MCCombo			ortMCCombo;
	private MCCombo			plzMCCombo;
	private Text			popupping;
	private Menu			menuTester;
	
	// Actions für ViewMenu, etc
	private Action			testingAction;
	private Action			importNamesAction;
	private Action			importCountriesAction;
	private Action			importTabDelimitedAction;
	
	// constructor
	public PlzTesting() {
		super();
		landModifyListener  = new LandModifyListener();
	}
	
	// erstellen der ViewPart
	public void createPartControl(Composite parent){
		landModifyListener  = new LandModifyListener();
		
		top = new Composite(parent, SWT.NONE);
		top.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		top.setLayout(new GridLayout(2, false));
		
		// Land *************************************
		new Label(top, SWT.NONE).setText("Land");		
		
		// rightComposite für diese Zeile erstellen
		rightComposite = new Composite(top, SWT.NONE);
		rightComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tmpGrid = new GridLayout(2, false);
		tmpGrid.marginWidth  = 0;
		rightComposite.setLayout(tmpGrid);
		
		// LandCombo
		String[] laenderListeNamen = getLaenderListe("name", "landsorting, name", "en");
		String[] laenderListeIsos = getLaenderListe("iso2", "landsorting, name", "en");
		cbLandCombo = new Combo(rightComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbLandCombo.setItems(laenderListeNamen);
		cbLandCombo.setData("LandIso2", laenderListeIsos);
		cbLandCombo.setVisibleItemCount(20);
		
		// landIso2Field
		//new Label(landComposite, SWT.NONE).setText("Land Iso 3");
		landIso2Field = new Text(rightComposite, SWT.BORDER);
		
		cbLandCombo.  setText("Prefs Land");
		landIso2Field.setText("Prefs LandISO2");
		
		// Plz Label  *************************************
		new Label(top, SWT.NONE).setText("Postleitzahl");
		
		// brauch ich dann...
		String landIso2 = landIso2Field.getText();
		
		// *** PLZ MCCombo
		plzMCCombo = new MCCombo(top, SWT.BORDER);
		plzMCCombo.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String[] plzShowField = null;
		Object[] plzReturnFields = null;
		if (StringTool.isNothing(landIso2))	{
			plzShowField = new String[] {"plz", "ort27", "kanton", "land"};
			plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null, landIso2Field};
		} else {
			plzShowField = new String[] {"plz", "ort27", "kanton"};
			plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null};
		}
		plzMCCombo.setShowFields(plzShowField);
		plzMCCombo.setSortFields(plzShowField);
		plzMCCombo.setReturnFields(plzReturnFields);
		
		// Ort Label  *************************************
		new Label(top, SWT.NONE).setText("Ort");
		
		// *** Ort MCCombo
		ortMCCombo = new MCCombo(top, SWT.BORDER);
		ortMCCombo.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String[] ortShowField = {"ort27", "kanton", "plz", "land"};
		ortMCCombo.setShowFields(ortShowField);
		ortMCCombo.setSortFields(ortShowField);
		Object[] ortReturnFields = {ortMCCombo, null, plzMCCombo, landIso2Field};
		ortMCCombo.setReturnFields(ortReturnFields);
		
		// *** Testing again
		CCombo anotherOne = new CCombo(top, SWT.BORDER);
		anotherOne.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		String[] strList = {"Item1", "Item2", "Item3"};
		anotherOne.setItems(strList);
		
		Combo combo = new Combo(top, SWT.BORDER);
		combo.setItems(strList);
		combo.setItem(2, "");
		
		// Erstellen der Actions für die Menus, etc
		makeActions();
		
		// Erstellen des ViewMenus
		ViewMenus menu = new ViewMenus(getViewSite());
		menu.createMenu(testingAction, importCountriesAction, null, importNamesAction, importTabDelimitedAction);
		
		// *** alle Listener, etc. intallieren
		cbLandCombo.addModifyListener(landModifyListener);
		plzMCCombo.installDataProvider(new PlzMCComboDataProvider());
		ortMCCombo.installDataProvider(new OrtMCComboDataProvider());
		
		// *** Testing real popupmenu
		popupping = new Text(top, SWT.BORDER);
		menuTester = new Menu(top);
		String[] itemsArray = {"Kuhland", "GehBittschönLand", "Teutonia"};
		for (int ij = 0; ij < itemsArray.length; ij++)	{
			MenuItem menuItem = new MenuItem(menuTester, 0);
			menuItem.setText(itemsArray[ij]);
			menuItem.addSelectionListener(new LandPopupSelectionListener());
		}
		MenuItem menuItem = new MenuItem(menuTester, 0);
		menuItem.setText("-");
		for (int ij = 0; ij < itemsArray.length; ij++)	{
			menuItem = new MenuItem(menuTester, 0);
			menuItem.setText(itemsArray[ij]);
			menuItem.addSelectionListener(new LandPopupSelectionListener());
		}
		
		menuTester.setVisible(true);
		Point pt = new Point(popupping.getBounds().x, popupping.getBounds().y);
		pt = popupping.toDisplay(pt);
		menuTester.setLocation(pt.x, pt.y);
		popupping.setMenu(menuTester);
		menuTester.setVisible(true);
		popupping.addModifyListener(new PopupModifyListener());
		popupping.addMouseListener(new PopupMouseListener());
		cbLandCombo.addControlListener(new landComboControlListener());
	}
	
	class landComboControlListener implements ControlListener	{
		public void controlMoved(ControlEvent e) {
		}
		public void controlResized(ControlEvent e) {
			System.out.println("cbLandCombo.getItemCount(): " + cbLandCombo.getItemCount());
			int oneCharWidth = stringWidth(cbLandCombo, "-");
			System.out.println("oneCharWidth: " + oneCharWidth);
			for (int i = 0; i < cbLandCombo.getItemCount(); i++){
				//System.out.println("item: " + cbLandCombo.getItem(i));
				if (cbLandCombo.getItem(i).substring(1, 3).equals("––––".substring(1, 3)))	{
					System.out.println("found divider item: " + i);
					System.out.println("cbLandCombo.getBounds().width: " + cbLandCombo.getBounds().width);
					int numOfChars = (cbLandCombo.getBounds().width - 30) / oneCharWidth;
					String fillString = StringTool.filler("-", numOfChars);
					fillString = "";
					System.out.println("numOfChars: " + numOfChars);
					for (int j = 0; j < numOfChars; j++){
						fillString = fillString + "-";
					}
					System.out.println("fullLengthWidth: " + stringWidth(cbLandCombo, fillString));
					cbLandCombo.setItem(i, fillString);
					System.out.println("cbLandCombo.getVerticalBar(): " + cbLandCombo.getVerticalBar());
				}
			}
		}
	}
	class MyGraphics extends Graphics	{
		@Override
		public void clearRect(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void clipRect(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void copyArea(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		}
		@Override
		public Graphics create() {
			return null;
		}
		@Override
		public void dispose() {
		}
		@Override
		public void drawArc(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, ImageObserver arg3) {
			return false;
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, Color arg3, ImageObserver arg4) {
			return false;
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, int arg3, int arg4, ImageObserver arg5) {
			return false;
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, int arg3, int arg4, Color arg5, ImageObserver arg6) {
			return false;
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, ImageObserver arg9) {
			return false;
		}
		@Override
		public boolean drawImage(Image arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7, int arg8, Color arg9, ImageObserver arg10) {
			return false;
		}
		@Override
		public void drawLine(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void drawOval(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void drawPolygon(int[] arg0, int[] arg1, int arg2) {
		}
		@Override
		public void drawPolyline(int[] arg0, int[] arg1, int arg2) {
		}
		@Override
		public void drawRoundRect(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		}
		@Override
		public void drawString(String arg0, int arg1, int arg2) {
		}
		@Override
		public void drawString(AttributedCharacterIterator arg0, int arg1, int arg2) {
		}
		@Override
		public void fillArc(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		}
		@Override
		public void fillOval(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void fillPolygon(int[] arg0, int[] arg1, int arg2) {
		}
		@Override
		public void fillRect(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void fillRoundRect(int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		}
		@Override
		public Shape getClip() {
			return null;
		}
		@Override
		public Rectangle getClipBounds() {
			return null;
		}
		@Override
		public Color getColor() {
			return null;
		}
		@Override
		public Font getFont() {
			return null;
		}
		@Override
		public FontMetrics getFontMetrics(Font arg0) {
			return null;
		}
		@Override
		public void setClip(Shape arg0) {
		}
		@Override
		public void setClip(int arg0, int arg1, int arg2, int arg3) {
		}
		@Override
		public void setColor(Color arg0) {
		}
		@Override
		public void setFont(Font arg0) {
		}
		@Override
		public void setPaintMode() {
		}
		@Override
		public void setXORMode(Color arg0) {
		}
		@Override
		public void translate(int arg0, int arg1) {
		}
	}
	/**
	 * Länge eines Strings in Pixeln ermitteln. Es wird der Zeichensatz benutzt, der im Feld
	 * composite verwendet wird.
	 * @param composite: Berechnung für dieses Feld
	 * @param str: die Länge dieses Strings wird berechnet
	 * @return
	 */
	@SuppressWarnings("serial")
	public static int stringWidth(Composite composite, final String str)	{
		MyGraphics g = new MyGraphics();
		Graphics2D gfx = (Graphics2D)g;
		
		org.eclipse.swt.graphics.Font currentFont = composite.getFont();
		FontData[] fontData = currentFont.getFontData();
		String fontName  = fontData[0].getName();
		int    fontStyle = fontData[0].getStyle();
		int    fontSize  = fontData[0].getHeight();
		Font font = new Font(fontName, fontStyle, fontSize);
		gfx.setFont(font);
		FontMetrics fm = gfx.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(str, gfx);
        return (int) rect.getWidth();
//		org.eclipse.swt.graphics.Font currentFont = composite.getFont();
//		FontData[] fontData = currentFont.getFontData();
//		String fontName  = fontData[0].getName();
//		int    fontStyle = fontData[0].getStyle();
//		int    fontSize  = fontData[0].getHeight();
//		System.out.println("fontName: " + fontName);
//		System.out.println("fontSize: " + fontSize);
//		Font font = new Font(fontName, fontStyle, fontSize);
//        FontMetrics metrics = new FontMetrics(font) {};
//        Rectangle2D bounds = metrics.getStringBounds(str, null);
//        return (int) bounds.getWidth();
	}
	class LandPopupSelectionListener implements SelectionListener	{
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		public void widgetSelected(SelectionEvent e) {
			//System.out.println(e.widget.);
			MenuItem menuItem = (MenuItem)e.item;
			System.out.println(menuItem.getText());
			popupping.setText(e.text);
		}
	}
	class PopupMouseListener implements MouseListener	{
		public void mouseDoubleClick(MouseEvent e) {
		}
		public void mouseDown(MouseEvent e) {
			Point pt = new Point(popupping.getBounds().x, popupping.getBounds().y);			
			pt = top.toDisplay(pt);
			menuTester.setLocation(pt.x, pt.y + popupping.getBounds().height);
			menuTester.setVisible(!menuTester.getVisible());
		}
		public void mouseUp(MouseEvent e) {
		}
	}
	
	/**
	 * Die Einträge ermitteln, die in der Plz-Liste angezeigt werden sollen. 
	 * Diese Routine wird aufgerufen, wenn der Text im Textfeld geändert wurde.
	 * @return die anzuzeigenden Einträge für die Liste als zweidimensionaler String array
	 */
	class PlzMCComboDataProvider implements MCComboDataProvider	{
		// ausnahmsweise direkte Abfrage auf der Datenbank aufgrund der Geschwindigkeit,
		// die hier relevant ist.
		// via Persistent/Query unsäglich langsam (Buchstabe A hat mehrere hundert
		// Einträge nur schon für die Schweiz...)
		public String[][] mCComboDataProvider(MCCombo lThis) {
		// das Resultat initialisieren
		String[][] plzStrings = null;
		
		// wenn Feld leer, dann keine Auswahl anzeigen
		String currText = plzMCCombo.getText();
		if (StringTool.isNothing(currText))	{
			return plzStrings;
		}
		
		// die anzuzeigenden/Sortierungs- Felder einlesen
		String[] showFields  = lThis.getShowFields();
		String[] queryFields = lThis.getQueryFields();
		String[] sortFields  = lThis.getSortFields();
		// umwandeln in comma-delimited Strings für sql
		String showFieldsString  = stringArrayToString(showFields,  ",");
		String queryFieldsString = showFieldsString;
		if (queryFields != null)	{
			queryFieldsString = stringArrayToString(queryFields, ",");
		}
		String sortFieldsString  = stringArrayToString(sortFields,  ",");
		
		// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
		int numOfFields = showFields.length;
		
		// alle passenden Einträge aus der Datenbank auslesen
		// Datenbank anzapfen - oozapft is...
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Anzahl Einträge ermitteln, die passen
		int numOfEntries = 0;
		// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
		String landClause = " 1=1 ";
		if (!StringTool.isNothing(landIso2Field.getText()))	{
			landClause = " lower(land) = lower(" + JdbcLink.wrap(landIso2Field.getText()) + ") "; 
		}
		ResultSet rs = stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(plz) like lower(" + JdbcLink.wrap(currText + "%") + ") and plztyp != 80");
		try {
			rs.next();
			numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
		} catch (SQLException exc) {
			System.out.println("SQLException 1 in OrtMCComboDataProvider:");
			exc.printStackTrace();
		}
		if (numOfEntries > 0)	{
			// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste schreiben
			rs = stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(plz) like lower(" + JdbcLink.wrap(currText + "%") + ")  and plztyp != 80 order by " + sortFieldsString);
			try {
				plzStrings = new String[numOfEntries][numOfFields];
				int iii = 0;
				while (rs.next())	{
					String[] rowData = new String[numOfFields];
					for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++)	{
						rowData[showFieldsIx] = rs.getString(showFieldsIx+1);
					}
					plzStrings[iii] = rowData;
					iii++;
				}
			} catch (SQLException e1) {
				System.out.println("SQLException 2 in OrtMCComboDataProvider:");
				e1.printStackTrace();
			}
		}
		return plzStrings;
		}
	}
	
	/**
	 * Die Einträge ermitteln, die in der Ort-Liste angezeigt werden sollen. 
	 * Diese Routine wird aufgerufen, wenn der Text im Textfeld geändert wurde.
	 * @return die anzuzeigenden Einträge für die Liste als zweidimensionaler String array
	 */
	class OrtMCComboDataProvider implements MCComboDataProvider	{
		// ausnahmsweise direkte Abfrage auf der Datenbank aufgrund der Geschwindigkeit,
		// die hier relevant ist.
		// via Persistent/Query unsäglich langsam (Buchstabe A hat mehrere hundert
		// Einträge nur schon für die Schweiz...)
		public String[][] mCComboDataProvider(MCCombo lThis) {
		// das Resultat initialisieren
		String[][] plzStrings = null;
		
		// wenn Feld leer, dann keine Auswahl anzeigen
		String currText = ortMCCombo.getText();
		if (StringTool.isNothing(currText))	{
			return plzStrings;
		}
		
		// die anzuzeigenden/Sortierungs- Felder einlesen
		String[] showFields  = lThis.getShowFields();
		String[] queryFields = lThis.getQueryFields();
		String[] sortFields  = lThis.getSortFields();
		// umwandeln in comma-delimited Strings für sql
		String showFieldsString  = stringArrayToString(showFields,  ",");
		String queryFieldsString = showFieldsString;
		if (queryFields != null)	{
			queryFieldsString = stringArrayToString(queryFields, ",");
		}
		String sortFieldsString  = stringArrayToString(sortFields,  ",");
		
		// Anzahl anzuzeigender=abzufragender Felder ist in showFields definiert
		int numOfFields = showFields.length;
		
		// alle passenden Einträge aus der Datenbank auslesen
		// Datenbank anzapfen - oozapft is...
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Anzahl Einträge ermitteln, die passen
		int numOfEntries = 0;
		// wenn kein Land ausgewählt ist, dann via sql ALLE Länder abfragen
		String landClause = " 1=1 ";
		if (!StringTool.isNothing(landIso2Field.getText()))	{
			landClause = " lower(land) = lower(" + JdbcLink.wrap(landIso2Field.getText()) + ") "; 
		}
		ResultSet rs = stm.query("select count(*) as cnt from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(currText + "%") + ") and plztyp != 80");
		try {
			rs.next();
			numOfEntries = Integer.decode(rs.getString("cnt"));
			rs.close();
		} catch (SQLException exc) {
			System.out.println("SQLException 1 in OrtMCComboDataProvider:");
			exc.printStackTrace();
		}
		// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste schreiben
		if (numOfEntries > 0)	{
			rs = stm.query("select " + queryFieldsString + " from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(currText + "%") + ")  and plztyp != 80 order by " + sortFieldsString);
			try {
				plzStrings = new String[numOfEntries][numOfFields];
				int iii = 0;
				while (rs.next())	{
					String[] rowData = new String[numOfFields];
					for (int showFieldsIx = 0; showFieldsIx < numOfFields; showFieldsIx++)	{
						rowData[showFieldsIx] = rs.getString(showFieldsIx+1);
					}
					plzStrings[iii] = rowData;
					iii++;
				}
			} catch (SQLException e1) {
				System.out.println("SQLException 2 in OrtMCComboDataProvider:");
				e1.printStackTrace();
			}
		}
		return plzStrings;
		}
	}
	
	class LandModifyListener implements ModifyListener	{
		public void modifyText(ModifyEvent arg0) {
			int selected = cbLandCombo.getSelectionIndex();
			if (selected != -1)	{
				// setzen des isoStrings
				String[] returnStrings = (String[]) cbLandCombo.getData("LandIso2");
				landIso2Field.setText(returnStrings[selected]);
			}
			String landIso2 = landIso2Field.getText();
			// plz
			String[] plzShowField = null;
			Object[] plzReturnFields = null;
			if (StringTool.isNothing(landIso2))	{
				plzShowField = new String[] {"plz", "ort27", "kanton", "land"};
				plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null, landIso2Field};
			} else {
				plzShowField = new String[] {"plz", "ort27", "kanton"};
				plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null};
			}
			plzMCCombo.setShowFields(plzShowField);
			plzMCCombo.setSortFields(plzShowField);
			plzMCCombo.setReturnFields(plzReturnFields);
			// ort
			String[] ortShowField = null;
			Object[] ortReturnFields = null;
			if (StringTool.isNothing(landIso2))	{
				ortShowField = new String[] {"ort27", "plz", "kanton", "land"};
				ortReturnFields = new Object[] {ortMCCombo, plzMCCombo, null, landIso2Field};
			} else {
				ortShowField = new String[] {"ort27", "plz", "kanton"};
				ortReturnFields = new Object[] {ortMCCombo, plzMCCombo, null};
			}
			ortMCCombo.setShowFields(ortShowField);
			ortMCCombo.setSortFields(ortShowField);
			ortMCCombo.setReturnFields(ortReturnFields);
		}
	}
	class PopupModifyListener implements ModifyListener	{
		public void modifyText(ModifyEvent arg0) {
			int selected = cbLandCombo.getSelectionIndex();
			if (selected != -1)	{
				// setzen des isoStrings
				String[] returnStrings = (String[]) cbLandCombo.getData("LandIso2");
				landIso2Field.setText(returnStrings[selected]);
			}
			String landIso2 = landIso2Field.getText();
			String[] plzShowField = null;
			Object[] plzReturnFields = null;
			if (StringTool.isNothing(landIso2))	{
				plzShowField = new String[] {"plz", "ort27", "kanton", "land"};
				plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null, landIso2Field};
			} else {
				plzShowField = new String[] {"plz", "ort27", "kanton"};
				plzReturnFields = new Object[] {plzMCCombo, ortMCCombo, null};
			}
			plzMCCombo.setShowFields(plzShowField);
			plzMCCombo.setSortFields(plzShowField);
			plzMCCombo.setReturnFields(plzReturnFields);
		}
	}
	
	/**
	 * WorkbenchPart Methods
	 */	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus(){
		//plzViewer.getControl().setFocus();
	}
		
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
	
	/**
	 * Converts a StringArray to a string, delimited by {delimiter}
	 * @param strArray
	 * @param delimiter
	 * @return
	 */
	protected static String stringArrayToString(String[] strArray, String delimiter)	{
		String str = "";
		String delim = "";
	    for (int i = 0; i < strArray.length; i++) {
	      str = str + delim + strArray[i];
	      delim = delimiter;
	    }
	    return str;
	}
	
	/**
	 * Erstellen der Actions, die in ViewMenu/PopupMenu, etc benutzt werden
	 */
	private void makeActions(){
		// Tester Menu Item
		testingAction = new Action("Testing") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_REFRESH));
				setToolTipText("Testing Methods");
			}			
			public void run(){
				// ENVIRONS, PROPERTIES
				if (1==0) {
				System.out.println(System.getenv());
				System.out.println(System.getProperties());
				}
				
				// PROGRESS MONITOR ***********************************************
				if (1==1)	{
				ExecutionEvent eev = new ExecutionEvent();
				IProgressMonitor monitor = Handler.getMonitor(eev);
				monitor.beginTask("theName", 100);
				for (int i=0; i< 100; i++)	{
					monitor.worked(i);
				}
				}
				}
		};
		testingAction.setActionDefinitionId("testingAction");
		GlobalActions.registerActionHandler(this, testingAction);
		
		// alle Names importieren
		importNamesAction = new Action("Names importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Übersetzungen importieren (dauert lange!!!)");
			}			
			public void run(){
				DataImporter.importNameData();
			}
		};
		importNamesAction.setActionDefinitionId("importNamesAction");
		GlobalActions.registerActionHandler(this, importNamesAction);
		
		// alle Names importieren
		importCountriesAction = new Action("Länder importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Länder Iso-Daten importieren (dauert lange!!!)");
			}			
			public void run(){
				DataImporter.importCountryData("en,de,it,fr");
			}
		};
		importCountriesAction.setActionDefinitionId("importCountriesAction");
		GlobalActions.registerActionHandler(this, importCountriesAction);
		
		// alle Names importieren
		importTabDelimitedAction = new Action("Postleitzahlen importieren") {
			{
				setImageDescriptor(Desk.getImageDescriptor(Desk.IMG_IMPORT));
				setToolTipText("Importieren PLZ Schweiz");
			}			
			public void run(){
				DataImporter.importTabDelimited();
			}
		};
		importTabDelimitedAction.setActionDefinitionId("importTabDelimitedAction");
		GlobalActions.registerActionHandler(this, importTabDelimitedAction);
	}	
	/**
	 * nur Länder mit allen 3 Isos dürfen auswählbar sein: iso2, iso3 isonum! Muss schnell sein, deshalb
	 * @param fieldName dieses DB-Feld wird ausgelesen
	 * @param orderBy   sortieren nach diesen Feldern, comma-delimited
	 * @param locale
	 * @return
	 */
	public String[] getLaenderListe(final String fieldName, final String orderBy, final String locale){
		// Datenbank anzapfen
		Stm stm = PersistentObject.getConnection().getStatement();
		
		// Bedingung für isos erstellen
		String isoQuery = "";
		isoQuery = isoQuery + "and iso2   is not null ";
		isoQuery = isoQuery + "and iso3   is not null ";
		isoQuery = isoQuery + "and isonum is not null ";
		isoQuery = isoQuery + "and iso2   != " + JdbcLink.wrap("") + " ";
		isoQuery = isoQuery + "and iso3   != " + JdbcLink.wrap("") + " ";
		
		// Anzahl Länder-Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from CH_MARLOVITS_COUNTRY where upper(entrylanguage) = '" + locale.toUpperCase() + "' " + isoQuery);
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		
		// Länder für die aktuelle SystemSprache aus der Datenbank-Tabelle "land" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select " + fieldName + " from CH_MARLOVITS_COUNTRY where upper(entrylanguage) = '" + locale.toUpperCase() + "' " + isoQuery + " order by " + orderBy);
    	try {
			int i = 0;
			for (i = 0; i < numOfRows; i++)	{
				rs.next();
				tmpStringArray[i] = rs.getString(fieldName);
			}
			//Array arr = rs.getArray(1);
			//System.out.println(arr.toString());
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return tmpStringArray;
	}
}
