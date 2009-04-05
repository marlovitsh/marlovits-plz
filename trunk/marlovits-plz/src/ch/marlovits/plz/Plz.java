package ch.marlovits.plz;

import java.util.ArrayList;
import java.util.List;

import ch.elexis.data.AUF;
import ch.elexis.data.Eigenartikel;
import ch.elexis.data.Fall;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.PersistentObjectFactory;
import ch.elexis.data.Query;
import ch.elexis.data.Rechnung;
import ch.elexis.views.artikel.ArtikelContextMenu;
import ch.rgw.tools.StringTool;

public class Plz extends PersistentObject {

	static{
		addMapping("Plz","ID", "Land","LandISO3","Plz","Ort","Strasse","Kanton");
	}
	public Plz(String ID, String Land, String LandIso3, String plz, String Ort, String Strasse, String Kanton){
		create(null);
		set(new String[]{"ID", "Land","LandIso3","Plz","Ort","Strasse","Kanton"},new String[]{ID,Land,LandIso3,plz,Ort,Strasse,Kanton});
	}

	public static Plz load(String id){
		if(StringTool.isNothing(id)){
			return null;
		}
		return new Plz(id);
	}
	
	public Plz(String id) {
		super(id);
	}
	
	public String getLabel(){
		String[] f=new String[7];
		get(new String[]{"ID", "Land","LandIso3","Plz","Ort","Strasse","Kanton"},f);
		StringBuilder ret=new StringBuilder();
		ret.append(f[0]).append(" ").append(f[1]).append(" ").append(f[2]);
		return ret.toString();
	}

	protected Plz() { /* empty */}

	@Override
	protected String getTableName() {
		return "Plz";
	}

	@Override
	public int getCacheTime() {
		return Integer.MAX_VALUE;
	}
	
	public String getFieldData(String fieldName)	{
		//return "some data";
		return get(fieldName);
	}

	/**
	 * Gibt eine Liste  aller angezeigten Postleitzahlen zurück.
	 * @param -
	 * @return eine Liste der aktuell angezeigten Postleitzahlen
	 */
	public static List<Plz> getShownPostleitzahlen() {
		List<Plz> plzs = new ArrayList<Plz>(); 
		
		Query<Plz> query = new Query<Plz>(Plz.class);
		query.insertTrue();
		query.orderBy(false, "Land", "Plz", "Ort");
		List<Plz> plzList = query.execute();
		if (plzList != null) {
			plzs.addAll(plzList);
		}
		return plzs;
	}
	
}
