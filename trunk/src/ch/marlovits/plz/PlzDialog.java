package ch.marlovits.plz;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.JdbcLink.Stm;

public class PlzDialog extends TitleAreaDialog {
	private JdbcLink j=PersistentObject.getConnection();

	//LabeledInputField liBeleg, liDate, liBetrag;
	Text		text;
	Plz			last, act;
	Combo		cbCats;
	Combo		cbLandCombo;
	Text		landIso2Field;
	Text		plzField;
	Text		ort;
	Text		strasse;
	Composite	compKanton;
	Combo		cbKantonIso;
	Combo		cbKantonName;
	Composite	compKanton2;
	Combo		cbKantonIso2;
	Combo		cbKantonName2;
	Composite	compKanton3;
	Combo		cbKantonIso3;
	Combo		cbKantonName3;
	String		lang;
	ModifyListener	landModifyListener;
	ModifyListener	kantonIsoModifyListener;
	ModifyListener	kantonNameModifyListener;
	int				numOfRegions;
	Composite[]		compKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Label[]			labelKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Combo[]			comboIsoKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Combo[]			comboNameKantonArray = {null, null, null, null, null, null, null, null, null, null};
	Composite		top;
	String			currLandIso;
	
	/**
	 * Constructor für PlzDialog bei vorhandener Plz (PLZ editieren)
	 * @param shell
	 * @param plz
	 */
	PlzDialog(Shell shell, Plz plz){
		super(shell);
		act = plz;
		setLang();
		landModifyListener   = new LandModifyListener();
		kantonIsoModifyListener = new KantonIsoModifyListener();
		kantonNameModifyListener = new KantonNameModifyListener();
	}
	
	/**
	 * Constructor für PlzDialog bei noch nicht vorhandener Plz (PLZ erfassen)
	 * @param shell
	 */
	PlzDialog(Shell shell){
		super(shell);
		act = null;
		setLang();
		landModifyListener   = new LandModifyListener();
		kantonIsoModifyListener = new KantonIsoModifyListener();
		kantonNameModifyListener = new KantonNameModifyListener();
	}
	
	/**
	 * Setzen der internen Variablen lang für die Internationalization (DE/FR/IT/EN, etc)
	 * Falls irgendetwas schief geht, dann Default = "DE"
	 */
	private void setLang()	{
		// Lesen aus der Locale
		lang = Locale.getDefault().toString().substring(0, 2).toUpperCase();
		// wenn aus irgendwelchen Gründen leer...
		if ((lang == "") || (lang == null))	{
			lang = "DE";
			return;
		}
		// lang muss eine richtige Sprache sein, testen gegen ISO-Sprachen in Locale
		String [] languages = Locale.getISOLanguages();
		boolean found = false;
		for (int i = 0; i < languages.length; i++)	{
			if (lang.toUpperCase() == languages[i].toUpperCase())	{
				found = true;
			}
		}
		if (found == false)	lang = "DE";
	}
	
	/**
	 * Dialog für die Änderung vorhandener/Eingabe neuer Postleitzahlen
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		/*
		 * Layout:	- Composite top mit 2 Spalten, füllt ganzen Dialog
		 * 			  enthält immer Bezeichnungen/Labels
		 * 			- rechtes Panel von Composite top enthält:
		 * 				- einfaches Feld    			ODER
		 * 				- rightComposite mit 2 Spalten
		 * 
		 *        __top____________________________________________
		 * 		||		||	 ___rightComposite______________	||
		 * 		||		||	|			|					|	||
		 * 		||		|| 	 -------------------------------	||
		 * 		||		||										||
		 * 		||		||	 ___rightComposite______________	||
		 * 		||		||	|			|					|	||
		 *     	||		||	 -------------------------------	||
		 * 		||		||										||
		 *     	 -------------------------------------------------
		 */
		// für rechts benutzt
		GridLayout	tmpGrid;
		Composite	rightComposite = null;
		
		// top: Darstellung in zwei Spalten, ganze Breite und ganze Höhe ausnutzen
		top = new Composite(parent, SWT.NONE);
		top.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		top.setLayout(new GridLayout(2, false));
		
		// Land *************************************
		// Label::LandCombo:LandIso2Text
		// Label
		new Label(top, SWT.NONE).setText("Land");		
		
		// rightComposite für diese Zeile erstellen
		rightComposite = new Composite(top, SWT.NONE);
		rightComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tmpGrid = new GridLayout(2, false);
		tmpGrid.marginWidth  = 0;
		rightComposite.setLayout(tmpGrid);
		
		// LandCombo
		String[] laenderListeNamen = getLaenderListe("landname", lang);
		String[] laenderListeIsos  = getLaenderListe("landiso2", lang);
		cbLandCombo = new Combo(rightComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbLandCombo.setItems(laenderListeNamen);
		cbLandCombo.setData("LandIso2", laenderListeIsos);
		
		// landIso2Field
		//new Label(landComposite, SWT.NONE).setText("Land Iso 3");
		landIso2Field = new Text(rightComposite, SWT.BORDER);
		
		// Postleitzahl *****************************
		// Label::plzField
		new Label(top, SWT.NONE).setText("Postleitzahl");
		plzField = new Text(top, SWT.BORDER);
		plzField.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));

		// Ort **************************************
		// Label::Ort
		new Label(top, SWT.NONE).setText("Ort");
		ort = new Text(top, SWT.BORDER);
		ort.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Strasse **********************************
		// Label::Strasse
		new Label(top, SWT.NONE).setText("Strasse");
		strasse = new Text(top, SWT.BORDER);
		strasse.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		strasse.moveBelow(top);
		
		// Kanton ***********************************
		
		
		// Anzahl Regionsmenus für aktuelles Land ermitteln
		numOfRegions = getNumOfRegions("RU", "DE");
		addKantonsFields(top);
		
		
		// Label::KantonSubIso:KantonName
		// Label
		new Label(top, SWT.NONE).setText("Kanton***");
		
		// rightComposite für diese Zeile erstellen
		compKanton = new Composite(top, SWT.NONE);
		compKanton.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		tmpGrid = new GridLayout(2, false);
		tmpGrid.marginWidth  = 0;
		compKanton.setLayout(tmpGrid);
		
		// Combo für KantonSubIso
		cbKantonIso = new Combo(compKanton, SWT.DROP_DOWN|SWT.READ_ONLY);
		
		// Combo für KantonName
		cbKantonName = new Combo(compKanton, SWT.DROP_DOWN|SWT.READ_ONLY);
		
		// Einsetzen der Werte
		if (act != null)	{
			// die Werte aus der Selection aus PlzView einsetzen
			cbLandCombo.  setText(act.get("Land"));
			landIso2Field.setText(act.get("Landiso2"));
			plzField.     setText(act.get("Plz"));
			ort.          setText(act.get("Ort"));
			strasse.      setText(act.get("Strasse"));
			String[] kantonsListe = getKantonsListe("kantonsubcode", act.get("LandISO2"), lang);
			if (kantonsListe != null)	{
				cbKantonIso.setItems(kantonsListe);
			}
			String[] ktListe      = getKantonsListe("kantonname",    act.get("LandISO2"), lang);
			if (ktListe != null)	{
				cbKantonName.setItems(kantonsListe);
			}
			cbKantonIso.  setText(act.get("Kantonkuerzel"));
			cbKantonName. setText(act.get("Kanton"));
		}
		else	{
			// neuer Eintrag wird erstellt: Default-Werte einsetzen
			// TO DO die Default-Werte werden in den Prefs gesetzt
			cbLandCombo.  setText("Prefs Land");
			landIso2Field.setText("Prefs LandISO2");
			plzField.     setText("Prefs Plz");
			ort.          setText("Prefs Ort");
			strasse.      setText("Prefs Strasse");
			String[] kantonsListe = getKantonsListe("kantonsubocde", "CH", lang);
			if (kantonsListe != null)	{
				cbKantonIso.setItems(kantonsListe);
			}
			String[] ktListe      = getKantonsListe("kantonname",    "CH", lang);
			if (ktListe != null)	{
				cbKantonName.setItems(kantonsListe);
			}
			cbKantonIso.  setText("Prefs Kantonkuerzel");
			cbKantonName. setText("Prefs Kanton");
		}
		
		// alle Listeners installieren *******************************************
		// Land Combo
		
		cbLandCombo.addModifyListener(landModifyListener);
		// Kantonskürzel Combo
		String[] ktListe = getKantonsListe("kantonname", landIso2Field.getText(), lang);
		cbKantonIso.setData("kantonsListe", ktListe);
		cbKantonIso.addModifyListener(kantonIsoModifyListener);
		cbKantonName.addModifyListener(kantonNameModifyListener);
		
		// und nun der Rückgabewert
		return top;
	}
	
	private void addKantonsFields(final Composite top)	{
		GridLayout	tmpGrid;
		
		// eventuell vorhandene Felder entfernen
		try	{
			for (int i=0; i<labelKantonArray.length; i++)	{
				labelKantonArray[i].dispose();
				comboIsoKantonArray[i].dispose();
				comboNameKantonArray[i].dispose();	
				compKantonArray[i].dispose();
			}
		}
		catch (java.lang.Exception e)	{
			// nix - will einfach keine Fehlermeldungen...
		}
		// alle Felder neu anlegen
		for (int i=0; i<numOfRegions; i++)	{
			// Label
			Label tmpLabel = new Label(top, SWT.NONE);
			tmpLabel.setText("Kanton***");
			labelKantonArray[i] = tmpLabel;
			
			// rightComposite für diese Zeile erstellen
			Composite tmpComposite = new Composite(top, SWT.NONE);
			tmpComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			tmpGrid = new GridLayout(2, false);
			tmpGrid.marginWidth  = 0;
			tmpComposite.setLayout(tmpGrid);
			compKantonArray[i] = tmpComposite;
			
			// Combo für KantonSubIso
			Combo tmpCombo1 = new Combo(tmpComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
			comboIsoKantonArray[i] = tmpCombo1;
			
			// Combo für KantonName
			Combo tmpCombo2 = new Combo(tmpComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
			comboNameKantonArray[i] = tmpCombo2;
		}
		top.pack();
	}
	
	class KantonIsoModifyListener implements ModifyListener	{
		public void modifyText(ModifyEvent arg0) {
			int selected = cbKantonIso.getSelectionIndex();
			if (selected != -1)	{
				String[] returnStrings = (String[]) cbKantonIso.getData("kantonsListe");
				String[] ktListe2      = getKantonsListe("kantonname",    landIso2Field.getText(), lang);
				cbKantonName.setText(ktListe2[selected]);
				cbKantonIso.pack();
			} else	{
				cbKantonName.setText(null);
			}
		}		
	}
	
	class KantonNameModifyListener implements ModifyListener	{
		public void modifyText(ModifyEvent arg0) {
			int selected = cbKantonName.getSelectionIndex();
			if (selected != -1)	{
				String[] returnStrings = (String[]) cbKantonName.getData("kantonsListe");
				String[] ktListe2      = getKantonsListe("kantonsubcode",    landIso2Field.getText(), lang);
				cbKantonIso.setText(ktListe2[selected]);
				cbKantonName.pack();
			} else	{
				cbKantonIso.setText(null);
			}
		}		
	}
	
	class LandModifyListener implements ModifyListener	{
		public void modifyText(ModifyEvent arg0) {
			int selected = cbLandCombo.getSelectionIndex();
			if (selected != -1)	{
				cbKantonIso.removeModifyListener(kantonIsoModifyListener);
				cbKantonName.removeModifyListener(kantonNameModifyListener);
				String[] returnStrings = (String[]) cbLandCombo.getData("LandIso2");
				landIso2Field.setText(returnStrings[selected]);
				String[] kantonsListe = getKantonsListe("kantonsubcode", returnStrings[selected], lang);
				String[] ktListe      = getKantonsListe("kantonname",    returnStrings[selected], lang);
				numOfRegions = getNumOfRegions(landIso2Field.getText(), lang);
				addKantonsFields(top);
				cbKantonIso.setItems(kantonsListe);
				cbKantonIso.setData("kantonsListe", ktListe);
				cbKantonName.setItems(ktListe);
				cbKantonName.setData("kantonsListeIso", kantonsListe);
				// die Grösse der FormItems neu berechnen lassen
				cbKantonIso.pack();
				cbKantonName.pack();
				compKanton.pack();
				cbKantonIso.addModifyListener(kantonIsoModifyListener);
				cbKantonName.addModifyListener(kantonNameModifyListener);
				int numOfMenus = getNumOfRegions(returnStrings[selected], lang);
				System.out.println("numOfMenus: " + numOfMenus);
			}
		}
	}
	
	
	/**
	 * Dialog für die Erfassung neuer PLZ-Daten/Änderung von PLZ-Daten
	 */
	@Override
	public void create(){
		super.create();
		if (act == null) {
			setTitle("Postleitzahl erfassen");
		} else {
			setTitle("Postleitzahl editieren");
		}
		setMessage("Bitte geben Sie alle benötigten Daten ein");
		getShell().setText("Postleitzahl editieren");
		cbLandCombo.setFocus();
	}
	
	@Override
	protected void okPressed(){
		// *** testen, ob alle Bedingungen für korrekte PLZ erfüllt sind
		// *** falls nicht, wird eine Liste aller Fehler angezeigt und der Dialog nicht geschlossen
		// *** der Fokus wird auf das erste Feld mit Fehler gesetzt
		String	errMsg		= "";
		String	err			= "";
		Object	focusField	= null;
		// LandIso2 muss ausgewählt sein
		if ((err = isFieldEmpty(cbLandCombo, "Land")) != "")	{
			if (focusField == null) focusField = cbLandCombo;
			errMsg = errMsg + err + "\n";
			//return;
			}
		// Plz muss ein bestimmtes Format aufweisen, definiert in der DB-Tabelle land:plzregex
		// Die dazugehörige Fehlermeldung ist definiert in der DB-Tabelle land:plzregexmessage
		String plzRegex = getLandFieldValue(landIso2Field.getText(), "plzregex", lang);
		if ((plzRegex != null) && (plzRegex != ""))	{
			Pattern pattern = Pattern.compile(plzRegex);
			Matcher matcher = pattern.matcher(plzField.getText());
			boolean matchFound = matcher.matches();
			if (matchFound == false){
				if (focusField == null) focusField = plzField;
				err = getLandFieldValue(landIso2Field.getText(), "plzregexmessage", lang);
				errMsg = errMsg + err + "\n";
			}
		}
		// Ort muss ausgefüllt sein
		if ((err = isFieldEmpty(ort, "Ort")) != "")	{
			if (focusField == null) focusField = ort;
			errMsg = errMsg + err + "\n";
			}
		// Kantonkuerzel muss je nach Land in LandISO2 ausgefüllt sein
		// TODO kantonauswaehlen strasseerlaubt
		String kantonauswaehlen = getLandFieldValue(landIso2Field.getText(), "kantonauswaehlen", lang);
		if ((Integer.parseInt(kantonauswaehlen) != 0) && (cbKantonIso.getText() == null))	{
			if (focusField == null) focusField = cbKantonIso;
			errMsg = errMsg + "Der Kanton muss ausgewählt werden.\n";
		}
		
		// Anzeige Zusammenfassung Fehler, falls hasErrors = true;
		if (errMsg != ""){
			try	{
				((Text)focusField).setFocus();
			} catch	(java.lang.Exception e) {
				((Combo)focusField).setFocus();
			}
			SWTHelper.alert("Felder nicht korrekt ausgefüllt", errMsg);
			return;
		}
		
		if (act == null) {
			// neuen Eintrag erstellen
			act = new Plz(cbLandCombo.getText(), landIso2Field.getText(), plzField.getText(), ort.getText(), strasse.getText(), cbKantonName.getText(), cbKantonIso.getText());
		} else {
			// bestehenden Eintrag ändern
			act.set("Land",          cbLandCombo.getText());
			act.set("LandISO2",      landIso2Field.getText());
			act.set("Plz",           plzField.getText());
			act.set("Ort",           ort.getText());
			act.set("Strasse",       strasse.getText());
			act.set("Kantonkuerzel", cbKantonIso.getText());
			act.set("Kanton",        cbKantonName.getText());
		}
		super.okPressed();
	}
	
	private int getNumOfRegions(final String landIso, final String locale)	{
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		String sql = "select count(*) as regionCount from (select kantonindex from ch_marlovits_kanton where upper(kantonlanguage) = '" + locale.toUpperCase() + "' and upper(kantonland) = '" + landIso.toUpperCase() + "' and deleted = '0' group by kantonindex) as foo";
		// Anzahl Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query(sql);
		try {
			rs.next();
			numOfRows = rs.getInt("regionCount");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return 0;
		}
		return numOfRows;
	}
	
	/**
	 * Feldwert als String aus der DB auslesen
	 * @param landIso2: suche Eintrag mit dieser LandISO2
	 * @param feldName: lese Wert für dieses Feld
	 * @return eingelesener Wert oder null, wenn nicht gefunden
	 */
	private String getLandFieldValue(final String landIso2, final String feldName, String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Feld plzlaenge für die übermittelte id aus der Datenbank-Tabelle "land" einlesen
		String res = null;
		ResultSet rs = stm.query("select " + feldName + " from land where upper(landiso2) = '" + landIso2.toUpperCase() + "' and landlanguage = '" + locale.toUpperCase() + "'");
		try {
			rs.next();
			res = rs.getString(feldName);
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
	
	/**
	 * nur Länder mit allen 3 Isos dürfen aswählbar sein: iso2, iso3 isonum!
	 * @param fieldName
	 * @param locale
	 * @return
	 */
	public String[] getLaenderListe(final String fieldName, final String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Bedingung für isos erstellen
		String isoQuery = "";
		isoQuery = isoQuery + "and landiso2   is not null ";
		isoQuery = isoQuery + "and landiso3   is not null ";
		isoQuery = isoQuery + "and landisonum is not null ";
		isoQuery = isoQuery + "and landiso2   != '' ";
		isoQuery = isoQuery + "and landiso3   != '' ";
		isoQuery = isoQuery + "and landisonum != '' ";
		
		// Anzahl Länder-Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from CH_MARLOVITS_LAND where upper(landlanguage) = '" + locale.toUpperCase() + "' " + isoQuery);
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		// Länder für die aktuelle SystemSprache aus der Datenbank-Tabelle "land" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select " + fieldName + " from CH_MARLOVITS_LAND where upper(landlanguage) = '" + locale.toUpperCase() + "' " + isoQuery + " order by landsorting, landname");
    	try {
			int i = 0;
			for (i = 0; i < numOfRows; i++)	{
				rs.next();
				tmpStringArray[i] = rs.getString(fieldName);
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return tmpStringArray;
	}

	public String[] getKantonsListe(final String fieldName, final String landIso2, final String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Kantons-Datensätze ermitteln
		int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from CH_MARLOVITS_KANTON where upper(kantonlanguage) = '" + locale.toUpperCase() + "' and kantonland = " + "'" + landIso2 + "'");
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			j.releaseStatement(stm);
			return null;
		}
		
		// Kanton/Staat/etc. für die aktuelle SystemSprache und das ausgewählte Land
		// aus der Datenbank-Tabelle "kanton" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select " + fieldName + " from CH_MARLOVITS_KANTON where upper(kantonlanguage) = '" + locale.toUpperCase() + "' and kantonland = " + "'" + landIso2 + "' order by " + fieldName);
		if (rs == null)	{
			j.releaseStatement(stm);
			return null;
		}
		
		try {
			int i = 0;
			for (i = 0; i < numOfRows; i++)	{
				rs.next();
				tmpStringArray[i] = rs.getString(fieldName);
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			j.releaseStatement(stm);
			return null;
		}
		j.releaseStatement(stm);
		
		return tmpStringArray;
	}
	
	/**
	 * Testet, ob ein Feld leer ist.
	 * @param fieldControl: Feld, das getestet werden soll
	 * @param fieldName: Name für das Feld, das in der Fehlermeldung angezeigt werden soll
	 * @return String: Fehlermeldung bei Fehler, sonst ""
	 */
	private String isFieldEmpty(final Object fieldControl, final String fieldName) {
		String str;
		// Trick, damit alle Casts ohne Fehlermeldung abgeklappert werden können
		try	{
			Combo ctrl = (Combo)fieldControl;
			str = ctrl.getText();
		} catch	(java.lang.Exception e) {
			Text ctrl = (Text)fieldControl;
			str = ctrl.getText();
		}
		if ((str == null) || (str == ""))	{
			return "Das Feld '" + fieldName + "' muss ausgefüllt sein";
		}
		else	{
			return "";
		}
	}
	
}