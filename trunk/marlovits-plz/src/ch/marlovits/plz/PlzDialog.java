package ch.marlovits.plz;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.Money;
import ch.rgw.tools.JdbcLink.Stm;

public class PlzDialog extends TitleAreaDialog {
	private JdbcLink j=PersistentObject.getConnection();

	boolean bType;
	//LabeledInputField liBeleg, liDate, liBetrag;
	Text text;
	Plz last, act;
	Combo cbCats;
	Combo cbLandCombo;
	
	PlzDialog(Shell shell, boolean mode){
		super(shell);
		bType = mode;
		act = null;
	}
	
	PlzDialog(Shell shell, Plz plz){
		super(shell);
		act = plz;
	}
	
	/**
	 * Dialog für die Änderung vorhandener PLZ-Daten
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
		Combo cbLandCombo = new Combo(cbLandComposite, SWT.DROP_DOWN|SWT.READ_ONLY);
		
		// Datenbank anzapfen
		Stm stm = j.getStatement();
		
		// Anzahl Länder-Datensätze ermitteln
    	int numOfRows = 0;
		ResultSet rs = stm.query("select count(*) as cnt from land where landlanguage = 'DE'");
		try {
			rs.next();
			numOfRows = rs.getInt("cnt");
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// ausgelesene Länder in die ComboBox einsetzen
		cbLandCombo.setItems(tmpStringArray);
		cbLandCombo.select(0);
		cbLandCombo.setLocation(0,0);
		cbLandCombo.setText(act.get("Land"));
		
		// Postleitzahl: je nach Land Zahl unterschiedlicher Länge oder Text
		new Label(cbLandComposite, SWT.NONE).setText("Postleitzahl");
		Text plzField = new Text(cbLandComposite, SWT.BORDER);
		plzField.setText(act.get("Plz"));
		plzField.setBounds(10,10,200,20);
		plzField.setTextLimit(30);
		
		// Ort: einfache Texteingabe
		new Label(cbLandComposite, SWT.NONE).setText("Ort");
		Text ort = new Text(cbLandComposite, SWT.BORDER);
		ort.setText(act.get("Ort"));
		ort.setBounds(10,10,200,20);
		ort.setTextLimit(30);
		
		// Strasse: einfache Texteingabe
		new Label(cbLandComposite, SWT.NONE).setText("Strasse");
		Text strasse = new Text(cbLandComposite, SWT.BORDER);
		strasse.setText(act.get("Strasse"));
		strasse.setBounds(10,10,200,20);
		strasse.setTextLimit(30);
		
		// Kanton: einfache Texteingabe
		new Label(cbLandComposite, SWT.NONE).setText("Kanton");
		Text kanton = new Text(cbLandComposite, SWT.BORDER);
		kanton.setText(act.get("Kanton"));
		kanton.setBounds(10,10,200,20);
		kanton.setTextLimit(30);

		// und nun der Rückgabewert
		return ret;
	}
	
	/**
	 * Dialog für die Erfassung neuer PLZ-Daten
	 */
	@Override
	public void create(){
		super.create();
		if (act == null) {
			if (bType) {
				setTitle("Einnahme verbuchen");
			} else {
				setTitle("Ausgabe verbuchen");
			}
		} else {
			setTitle("Postleitzahl editieren");
		}
		setMessage("Bitte geben Sie alle benötigten Daten ein");
		getShell().setText("Postleitzahl editieren");
		//liBetrag.getControl().setFocus();
	}
	
	@Override
	protected void okPressed(){
		Money money = new Money();
		try {
		//	money.addAmount(liBetrag.getText());
		} catch (Exception ex) {
			ExHandler.handle(ex);
		}
		//TimeTool tt = new TimeTool(liDate.getText());
		//String bt = text.getText();
		
		if (act == null) {
			// *ç* neuen Eintrag erstellen
			if (!bType) {
				money = money.negate();
			}
		//	act =
		//		new Plz(liBeleg.getText(), tt.toString(TimeTool.DATE_GER), money, bt,
		//			last);
		} else {
			// *ç* bestehenden Eintrag ändern
		//	act.set(new String[] {
		//		"BelegNr", "Datum", "Betrag", "Text"
		//	}, liBeleg.getText(), tt.toString(TimeTool.DATE_GER), money.getCentsAsString(), text
		//		.getText());
			//Plz.recalc();
		}
		//act.setKategorie(cbCats.getText());
		super.okPressed();
	}

}
