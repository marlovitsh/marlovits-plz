/*******************************************************************************
 * Copyright (c) 2009, Harald Marlovits
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Harald Marlovits
 *    
 * $Id: PlzEintrag.java
 *******************************************************************************/

package ch.marlovits.plz;

import ch.elexis.data.PersistentObject;

public class PlzEintrag extends PersistentObject {
	// Tabellenname in der Datenbank
	private static final String TABLENAME = "CH_MARLOVITS_PLZ";
	// die Version der Tabelle
	public static final String VERSION = "2.0";
	// CreateScript für die Tabelle
	private static final String createDB =
		"CREATE TABLE " + TABLENAME + " (" +
		"id					character varying(25) NOT NULL,	" +
		"onrp				character(5),					" + // Ordnungsnummer Post
		"plztyp				character(2),					" + // PLZ-Typ,
																// 10 = Domizil- und Fachadressen,
																// 20 = Domiziladressen
																// 30 = Fachadressen
																// 40 = Firmenadressen
																// 80 = postinterne PLZ
		"plz				character(10),					" + // Postleitzahl
		"zusatzziffer		character(2),					" + // Zusatzziffer zur Unterscheidung
																// von gleichlautenden Postleitzahlen
		"ort18				character varying(18),			" + // Offizielle Ortsbezeichnung kurz
		"ort27				character varying(27),			" + // Offizielle Ortsbezeichnung lang
		"kanton				character(2),					" + // Kanton
																// DE für Büsingen
																// IT für Campione
																// FL für Fürstentum Lichtenstein
		"sprachcode			character(1),					" + // Hauptsprache/Sprachmehrheit
		"sprachcode2		character(1),					" + // Nebensprache/weitere Sprache
		"sortierfile		character(1),					" + // im Sortierfile vorhanden	
																// 0 = nein, 1 = ja
		"briefzustellung	character varying(5),			" + // Briefzustellung durch diese Betriebstelle
		"gemeindenr			character(5),					" + // Gemeindenr BfS
		"gueltigab			character(8),					" + // YYYYMMDD
		"entrylanguage		character(2),					" +	// Sprache dieses Eintrages
		"deleted			CHAR(1) default '0',			" +
		"lastupdate			bigint,							" +
		"CONSTRAINT	" + TABLENAME + "_pkey PRIMARY KEY (id)	" + // Primary key erstellen
		")													" +
		"WITH (OIDS=FALSE);									" +
		"ALTER TABLE " + TABLENAME + " OWNER TO elexisuser;	" + // den owner für die Tabelle setzen
		// die Indizes erstellen
		"CREATE INDEX ch_marlovits_plz0 ON CH_MARLOVITS_PLZ USING btree (plz);		" +
		"CREATE INDEX ch_marlovits_plz1 ON CH_MARLOVITS_PLZ USING btree (ort18);		" +
		"CREATE INDEX ch_marlovits_plz2 ON CH_MARLOVITS_PLZ USING btree (ort27);		";
	
	static	{
		// ziemlich einfaches Mapping - einheitlich einfach der erste Buchstaben der Feldbezeichnung in der Datenbank als Capital
		addMapping(	TABLENAME,
					"Onrp=onrp",
					"Plztyp=plztyp",
					"Plz=plz",
					"Zusatzziffer=zusatzziffer",
					"Ort18=ort18",
					"Ort27=ort27",
					"Kanton=kanton",
					"Sprachcode=sprachcode",
					"Sprachcode2=sprachcode2",
					"Sortierfile=sortierfile",
					"Briefzustellung=briefzustellung",
					"Gemeindenr=gemeindenr",
					"Gueltigab=gueltigab",
					"Entrylanguage=entrylanguage");
		
		// Erstellen der Tabelle in der Datenbank
		createOrModifyTable(createDB);
		/*
		LandEintrag version = LandEintrag.load("1");
		if (!version.exists()) {
			createOrModifyTable(createDB);
		} else {
			VersionInfo vi = new VersionInfo(version.getLandName());
			if (vi.isOlder(VERSION)) {
				// Update-Script für ältere Versionen in der Datenbank
				if (vi.isOlder("1.0.0")) {
					getConnection().exec("ALTER TABLE " + TABLENAME + " ADD deleted CHAR(1) default '0';");
				}
				// Update-Script für ältere Versionen in der Datenbank
				if (vi.isOlder("1.1.0")) {
					createOrModifyTable("ALTER TABLE " + TABLENAME + " ADD Category VARCHAR(80);");
				}
				version.set("Text", VERSION);
			}
		}
		*/
	}
		
	/**
	 * 
	 * 
	 */
	public PlzEintrag(	String onrp,
						String plzTyp,
						String plz,
						String zusatzZiffer,
						String ort18,
						String ort27,
						String kanton,
						String sprachCode,
						String sprachCode2,
						String sortierFile,
						String briefZustellung,
						String gemeindeNr,
						String gueltigAb,
						String entryLanguage)	{
		create(null);
		set(new String[]{"Onrp", "Plztyp", "Plz", "Zusatzziffer", "Ort18", "Ort27", "Kanton", "Sprachcode", "Sprachcode2", "Sortierfile", "Briefzustellung", "Gemeindenr", "Gueltigab", "Entrylanguage"},
			new String[]{ onrp,   plzTyp,   plz,   zusatzZiffer,   ort18,   ort27,   kanton,   sprachCode,   sprachCode2,   sortierFile,   briefZustellung,   gemeindeNr,   gueltigAb,   entryLanguage});
	}
	
	/**
	 * Constructor: erstellt einen neuen Eintrag in der Datenbank, lediglich mit der ID. 
	 * Die ID besteht aus dem Iso2-Ländercode und der Sprache, getrennt durch einen Underscore. 
	 * Diese Kombination darf jeweils nur einmal vorkommen. 
	 * Dies ist ein etwas "falscher"/seltsamer Constructor: falls die ID schon in der Datenbank vorhanden 
	 * ist, dann wird dieser CountryEintrag verwendet, falls noch nicht vorhanden, dann wird ein neuer Eintrag in der 
	 * Datenbank erstellt.
	 * @param id: Iso2_Language, zBsp: CH_de
	 */
	// TODO hier nötig???
	public PlzEintrag(final String id){
		super(id);
		if (!exists())	{
			create(id);
		}
	}
	
	public String toString() {
		return super.toString();
	}

	@Override
	public String getLabel() {
		String[] f = new String[9];
		get(new String[]{"Bezeichnung", "WikiLink", "Iso3166Alpha2", "Iso3166Alpha3", "Iso3166Numeric", "TopLevelDomain", "OlympicCountryCodeIOC", "Iso3166_2", "Language"},f);
		StringBuilder ret=new StringBuilder();
		ret.append(f[0]).append(" ").append(f[1]).append(" ").append(f[2]);
		return ret.toString();
	}

	@Override
	protected String getTableName() {
		return TABLENAME;
	}
}
