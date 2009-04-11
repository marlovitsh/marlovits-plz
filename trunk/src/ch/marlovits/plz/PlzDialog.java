package ch.marlovits.plz;

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
	Text	text;
	Plz		last, act;
	Combo	cbCats;
	Combo	cbLandCombo;
	Text	landIso3Field;
	Text	plzField;
	Text	ort;
	Text	strasse;
	Combo	cbKantonCombo;
	Text	kantonText;
	String	lang;
	
	/**
	 * Constructor für PlzDialog bei vorhandener Plz (PLZ editieren)
	 * @param shell
	 * @param plz
	 */
	PlzDialog(Shell shell, Plz plz){
		super(shell);
		act = plz;
		setLang();
	}
	
	/**
	 * Constructor für PlzDialog bei noch nicht vorhandener Plz (PLZ erfassen)
	 * @param shell
	 */
	PlzDialog(Shell shell){
		super(shell);
		act = null;
		setLang();
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
		// Darstellung in zwei Spalten, ganze Breite ausnutzen
		Composite top = new Composite(parent, SWT.NONE);
		top.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		top.setLayout(new GridLayout(2, false));
		
		// Land: Auswahl aus Menu
		new Label(top, SWT.NONE).setText("Land");
		String[] laenderListe = getLaenderListe("land", lang);
		cbLandCombo = new Combo(top, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbLandCombo.setItems(laenderListe);
		cbLandCombo.setToolTipText("Kuckuck_");
		String[] laenderIsoListe = getLaenderListe("landiso3", lang);
		cbLandCombo.setData("LandIso3", laenderIsoListe);
		cbLandCombo.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				int selected = cbLandCombo.getSelectionIndex();
				if (selected != -1)	{
					String[] returnStrings = (String[]) cbLandCombo.getData("LandIso3");
					landIso3Field.setText(returnStrings[selected]);
				}
			}
		});
		cbLandCombo.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// LandIso3: nur zur Anzeige
		new Label(top, SWT.NONE).setText("Land Iso 3");
		landIso3Field = new Text(top, SWT.BORDER);
		landIso3Field.setBounds(10,10,200,20);
		landIso3Field.setTextLimit(30);
		landIso3Field.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
				
		// Postleitzahl: je nach Land Zahl unterschiedlicher Länge oder Text
		new Label(top, SWT.NONE).setText("Postleitzahl");
		plzField = new Text(top, SWT.BORDER);
		plzField.setBounds(10,10,200,20);
		plzField.setTextLimit(30);
		plzField.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Ort: einfache Texteingabe
		new Label(top, SWT.NONE).setText("Ort");
		ort = new Text(top, SWT.BORDER);
		ort.setBounds(10,10,200,20);
		ort.setTextLimit(30);
		ort.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Strasse: einfache Texteingabe
		new Label(top, SWT.NONE).setText("Strasse");
		strasse = new Text(top, SWT.BORDER);
		strasse.setBounds(10,10,200,20);
		strasse.setTextLimit(30);
		strasse.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Kanton: Kürzel-Auswahl aus Combo
		new Label(top, SWT.NONE).setText("Kantonkuerzel");
		cbKantonCombo = new Combo(top, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbKantonCombo.setLocation(0,0);
		strasse.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Kanton: Volltext-Anzeige
		new Label(top, SWT.NONE).setText("Kanton");
		kantonText = new Text(top, SWT.BORDER);
		kantonText.setBounds(10,10,200,20);
		kantonText.setTextLimit(30);
		kantonText.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		
		// Einsetzen der Werte
		if (act != null)	{
			// die Werte aus der Selection aus PlzView einsetzen
			cbLandCombo.  setText(act.get("Land"));
			landIso3Field.setText(act.get("LandISO3"));
			plzField.     setText(act.get("Plz"));
			ort.          setText(act.get("Ort"));
			strasse.      setText(act.get("Strasse"));
			String[] kantonsListe = getKantonsListe("kantonkuerzel", act.get("LandISO3"), lang);
			if (kantonsListe != null)	{
				cbKantonCombo.setItems(kantonsListe);
			}
			cbKantonCombo.setText(act.get("Kantonkuerzel"));
			kantonText.   setText(act.get("Kanton"));			
		}
		else	{
			// neuer Eintrag wird erstellt: Default-Werte einsetzen
			// die Default-Werte werden in den Prefs gesetzt
			cbLandCombo.  setText("Prefs Land");
			landIso3Field.setText("Prefs LandISO3");
			plzField.     setText("Prefs Plz");
			ort.          setText("Prefs Ort");
			strasse.      setText("Prefs Strasse");
			String[] kantonsListe = getKantonsListe("kantonkuerzel", "CHE", lang);
			if (kantonsListe != null)	{
				cbKantonCombo.setItems(kantonsListe);
			}
			cbKantonCombo.setText("Prefs Kantonkuerzel");
			kantonText.   setText("Prefs Kanton");
		}
		
		String[] ktListe = getKantonsListe("kanton", landIso3Field.getText(), lang);
		cbKantonCombo.setData("kantonsListe", ktListe);
		cbKantonCombo.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				int selected = cbKantonCombo.getSelectionIndex();
				if (selected != -1)	{
					String[] returnStrings = (String[]) cbKantonCombo.getData("kantonsListe");
					kantonText.setText(returnStrings[selected]);
				}
			}
		});
		// und nun der Rückgabewert
		return top;
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
		// LandIso3 muss ausgewählt sein
		if ((err = isFieldEmpty(cbLandCombo, "Land")) != "")	{
			if (focusField == null) focusField = cbLandCombo;
			errMsg = errMsg + err + "\n";
			//return;
			}
		// Plz muss ein bestimmtes Format aufweisen, definiert in der DB-Tabelle land:plzregex
		// Die dazugehörige Fehlermeldung ist definiert in der DB-Tabelle land:plzregexmessage
		String plzRegex = getLandFieldValue(landIso3Field.getText(), "plzregex", lang);
		if ((plzRegex != null) && (plzRegex != ""))	{
			Pattern pattern = Pattern.compile(plzRegex);
			Matcher matcher = pattern.matcher(plzField.getText());
			boolean matchFound = matcher.matches();
			if (matchFound == false){
				if (focusField == null) focusField = plzField;
				err = getLandFieldValue(landIso3Field.getText(), "plzregexmessage", lang);
				errMsg = errMsg + err + "\n";
			}
		}
		// Ort muss ausgefüllt sein
		if ((err = isFieldEmpty(ort, "Ort")) != "")	{
			if (focusField == null) focusField = ort;
			errMsg = errMsg + err + "\n";
			}
		// Kantonkuerzel muss je nach Land in LandISO3 ausgefüllt sein
		// TODO kantonauswaehlen strasseerlaubt
		String kantonauswaehlen = getLandFieldValue(landIso3Field.getText(), "kantonauswaehlen", lang);
		if ((Integer.parseInt(kantonauswaehlen) != 0) && (cbKantonCombo.getText() == null))	{
			if (focusField == null) focusField = cbKantonCombo;
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
			act = new Plz(cbLandCombo.getText(), landIso3Field.getText(), plzField.getText(), ort.getText(), strasse.getText(), kantonText.getText(), cbKantonCombo.getText());
		} else {
			// bestehenden Eintrag ändern
			act.set("Land",          cbLandCombo.getText());
			act.set("LandISO3",      landIso3Field.getText());
			act.set("Plz",           plzField.getText());
			act.set("Ort",           ort.getText());
			act.set("Strasse",       strasse.getText());
			act.set("Kantonkuerzel", cbKantonCombo.getText());
			act.set("Kanton",        kantonText.getText());
		}
		super.okPressed();
	}
	
	/**
	 * Feldwert als String aus der DB auslesen
	 * @param landIso3: suche Eintrag mit dieser LandISO3
	 * @param feldName: lese Wert für dieses Feld
	 * @return eingelesener Wert oder null, wenn nicht gefunden
	 */
	private String getLandFieldValue(final String landIso3, final String feldName, String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Feld plzlaenge für die übermittelte id aus der Datenbank-Tabelle "land" einlesen
		String res = null;
		ResultSet rs = stm.query("select " + feldName + " from land where upper(landiso3) = '" + landIso3.toUpperCase() + "' and landlanguage = '" + locale.toUpperCase() + "'");
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

	public String[] getLaenderListe(final String fieldName, final String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Länder-Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from land where upper(landlanguage) = '" + locale.toUpperCase() + "'");
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		// Länder für die aktuelle SystemSprache aus der Datenbank-Tabelle "land" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select " + fieldName + " from land where upper(landlanguage) = '" + locale.toUpperCase() + "' order by land");
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

	public String[] getKantonsListe(final String fieldName, final String landIso3, final String locale){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Kantons-Datensätze ermitteln
		int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from kanton where upper(kantonlanguage) = '" + locale.toUpperCase() + "' and kantonland = " + "'" + landIso3 + "'");
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
	
		// Kanton/Staat/etc. für die aktuelle SystemSprache und das ausgewählte Land
		// aus der Datenbank-Tabelle "kanton" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select " + fieldName + " from kanton where upper(kantonlanguage) = '" + locale.toUpperCase() + "' and kantonland = " + "'" + landIso3 + "' order by kanton");
		if (rs == null)	{
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
			return null;
		}
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