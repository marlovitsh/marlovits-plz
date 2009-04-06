package ch.marlovits.plz;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ch.elexis.data.PersistentObject;
import ch.elexis.util.Log;
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
	
	PlzDialog(Shell shell){
		super(shell);
		act = null;
	}
	
	PlzDialog(Shell shell, Plz plz){
		super(shell);
		act = plz;
	}
	
	/**
	 * Dialog für die Änderung vorhandener/Eingabe neuer Postleitzahlen
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite ret = new Composite(parent, SWT.NONE);
		ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		ret.setLayout(new GridLayout());
		
		// Land: Auswahl aus Menu
		Composite cbLandComposite = new Composite(ret, SWT.NONE);
		cbLandComposite.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		cbLandComposite.setLayout(new GridLayout(2, false));
		new Label(cbLandComposite, SWT.NONE).setText("Land");
		String[] laenderListe = getLaenderListe();
		cbLandCombo = new Combo(cbLandComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbLandCombo.setItems(laenderListe);
		cbLandCombo.setLocation(0,0);
		
		// LandIso3: nur zur Anzeige
		new Label(cbLandComposite, SWT.NONE).setText("Land Iso 3");
		landIso3Field = new Text(cbLandComposite, SWT.BORDER);
		landIso3Field.setBounds(10,10,200,20);
		landIso3Field.setTextLimit(30);
		
		// Postleitzahl: je nach Land Zahl unterschiedlicher Länge oder Text
		new Label(cbLandComposite, SWT.NONE).setText("Postleitzahl");
		plzField = new Text(cbLandComposite, SWT.BORDER);
		plzField.setBounds(10,10,200,20);
		plzField.setTextLimit(30);
		
		// Ort: einfache Texteingabe
		new Label(cbLandComposite, SWT.NONE).setText("Ort");
		ort = new Text(cbLandComposite, SWT.BORDER);
		ort.setBounds(10,10,200,20);
		ort.setTextLimit(30);
		
		// Strasse: einfache Texteingabe
		new Label(cbLandComposite, SWT.NONE).setText("Strasse");
		strasse = new Text(cbLandComposite, SWT.BORDER);
		strasse.setBounds(10,10,200,20);
		strasse.setTextLimit(30);
		
		// Kanton: Kürzel-Auswahl aus Combo
		new Label(cbLandComposite, SWT.NONE).setText("Kantonkuerzel");
		cbKantonCombo = new Combo(cbLandComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		cbKantonCombo.setLocation(0,0);
		
		// Kanton: Volltext-Anzeige
		new Label(cbLandComposite, SWT.NONE).setText("Kanton");
		kantonText = new Text(cbLandComposite, SWT.BORDER);
		kantonText.setBounds(10,10,200,20);
		kantonText.setTextLimit(30);
		
		// Einsetzen der Werte
		if (act != null)	{
			// die Werte aus der Selection aus PlzView einsetzen
			cbLandCombo.  setText(act.get("Land"));
			landIso3Field.setText(act.get("LandISO3"));
			plzField.     setText(act.get("Plz"));
			ort.          setText(act.get("Ort"));
			strasse.      setText(act.get("Strasse"));
			String[] kantonsListe = getKantonsListe(act.get("LandISO3"));
			if (kantonsListe != null)	{
				cbKantonCombo.setItems(kantonsListe);
			}
			cbKantonCombo.setText(act.get("Kantonkuerzel"));
			kantonText.   setText(act.get("Kanton"));			
		}
		else	{
			// neuer Eintrag wird erstellt: Default-Werte einsetzen
			// die Default-Werte werden in den Prefs gesetzt
			cbLandCombo.  setText("Land");
			landIso3Field.setText("LandISO3");
			plzField.     setText("Plz");
			ort.          setText("Ort");
			strasse.      setText("Strasse");
			String[] kantonsListe = getKantonsListe("CHE");
			if (kantonsListe != null)	{
				cbKantonCombo.setItems(kantonsListe);
			}
			cbKantonCombo.setText("Kantonkuerzel");
			kantonText.   setText("Kanton");
		}
		
		// und nun der Rückgabewert
		return ret;
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
		// *** falls nicht, wird eine Fehlermeldung angezeigt und der Dialog nicht geschlossen
		// LandIso3 muss ausgewählt sein
		if (isFieldEmpty(cbLandCombo, "Land"))	{
			return;
			}
		// Plz muss ausgefüllt sein
		if (isFieldEmpty(plzField, "Postleitzahl"))	{
			return;
			}
		// Plz muss die Bedingungen für das Land in LandISO3 erfüllen
		// TODO
		String neededLength = getLandFieldValue(landIso3Field.getText(), "plzlaenge");
		int actualLength = plzField.getText().length();
		if (Long.parseLong(neededLength) != actualLength){
			SWTHelper.alert("Felder nicht korrekt ausgefüllt", "Das Feld 'Postleitzahl' muss eine Länge von " + neededLength + " Zeichen aufweisen");
			plzField.setFocus();
			return;
		}
		// Ort muss ausgefüllt sein
		if (isFieldEmpty(ort, "Ort"))	{
			return;
			}
		// Kantonkuerzel muss je nach Land in LandISO3 ausgefüllt sein
		// TODO
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
	private String getLandFieldValue(final String landIso3, final String feldName){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Feld plzlaenge für die übermittelte id aus der Datenbank-Tabelle "land" einlesen
		String res = null;
		ResultSet rs = stm.query("select " + feldName + " from land where landiso3 = '" + landIso3 + "'");
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

	public String[] getLaenderListe(){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Länder-Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from land where landlanguage = 'DE'");
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}

		// Länder für die aktuelle SystemSprache aus der Datenbank-Tabelle "land" einlesen
		String[] tmpStringArray = new String[numOfRows];
		rs = stm.query("select land from land where landlanguage = 'DE' order by land");
    	try {
			int i = 0;
			for (i = 0; i < numOfRows; i++)	{
				rs.next();
				tmpStringArray[i] = rs.getString("land");
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return tmpStringArray;
	}

	public String[] getKantonsListe(final String land){
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Kantons-Datensätze ermitteln
		int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from kanton where kantonlanguage = 'DE' and kantonland = " + "'" + land + "'");
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
		rs = stm.query("select kantonkuerzel from kanton where kantonlanguage = 'DE' and kantonland = " + "'" + land + "' order by kanton");
		if (rs == null)	{
			return null;
		}
		try {
			int i = 0;
			for (i = 0; i < numOfRows; i++)	{
				rs.next();
				tmpStringArray[i] = rs.getString("kantonkuerzel");
			}
			rs.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return tmpStringArray;
	}
	
	/**
	 * Testet, ob ein Feld leer ist. Zeigt eine Fehlermeldung, dass das Feld ausgefüllt werden muss.
	 * @param fieldControl: Feld, das getestet werden soll
	 * @param fieldName: Name für das Feld, das in der Fehlermeldung angezeigt werden soll
	 * @return true, wenn leer; false, wenn nicht leer
	 */
	private boolean isFieldEmpty(final Object fieldControl, final String fieldName) {
		String str;
		// Trick, damit alle Casts ohne Fehlermeldung abgeklappert werden können
		try	{
			Combo ctrl = (Combo)fieldControl;
			str = ctrl.getText();
			ctrl.setFocus();
		} catch	(java.lang.Exception e) {
			Text ctrl = (Text)fieldControl;
			str = ctrl.getText();
			ctrl.setFocus();
		}
		if ((str == null) || (str == ""))	{
			SWTHelper.alert("Felder nicht korrekt ausgefüllt", "Das Feld '" + fieldName + "' muss ausgefüllt sein");
			return true;
		}
		else	{
			return false;
		}
	}
	
}
