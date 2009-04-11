/*******************************************************************************
 * Copyright (c) 2009, Harald Marlovits
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    H. Marlovits
 *    
 * $Id: LandIsoEntry.java
 *******************************************************************************/

package ch.marlovits.plz;

import java.util.HashMap;

import ch.elexis.data.Patient;

public class LandIsoEntry {
	private final String bezeichnung;
	private final String iso3166Alpha2;
	private final String iso3166Alpha3;
	private final String iso3166Numeric;
	private final String topLevelDomain;
	private final String olympicCountryCodeIOC;
	private final String iso3166_2;
	private final String language;

	public LandIsoEntry(final String bezeichnung,
						final String iso3166Alpha2,
						final String iso3166Alpha3,
						final String iso3166Numeric,
						final String topLevelDomain,
						final String olympicCountryCodeIOC,
						final String iso3166_2,
						final String language) {
		super();
		this.bezeichnung			= bezeichnung;
		this.iso3166Alpha2			= iso3166Alpha2;
		this.iso3166Alpha3			= iso3166Alpha3;
		this.iso3166Numeric			= iso3166Numeric;
		this.topLevelDomain			= topLevelDomain;
		this.olympicCountryCodeIOC	= olympicCountryCodeIOC;
		this.iso3166_2				= iso3166_2;
		this.language				= language;
	}

	/**
	 * Fill all fields into a hashmap
	 * @return a hashmap with all non-empty fields with standard names
	 * @author gerry
	 */
	public HashMap<String,String> toHashmap(){
		HashMap<String, String> ret=new HashMap<String, String>();
		if(countValue(bezeichnung)>0){
			ret.put(Patient.NAME, bezeichnung);
		}
		if(countValue(iso3166Alpha2)>0){
			ret.put(Patient.FIRSTNAME, iso3166Alpha2);
		}
		if(countValue(iso3166Alpha3)>0){
			ret.put(Patient.STREET, iso3166Alpha3);
		}
		if(countValue(iso3166Numeric)>0){
			ret.put(Patient.ZIP, iso3166Numeric);
		}
		if(countValue(topLevelDomain)>0){
			ret.put(Patient.PLACE, topLevelDomain);
		}
		if(countValue(olympicCountryCodeIOC)>0){
			ret.put(Patient.PHONE1, olympicCountryCodeIOC);
		}
		if(countValue(iso3166_2)>0){
			ret.put(Patient.FAX, iso3166_2);
		}
		if(countValue(language)>0){
			ret.put(Patient.FAX, language);
		}
		return ret;
	}
	public String getBezeichnung() {
		return this.bezeichnung;
	}

	public String getIso3166Alpha2() {
		return this.iso3166Alpha2;
	}

	public String getIso3166Alpha3() {
		return this.iso3166Alpha3;
	}

	public String getIso3166Numeric() {
		return this.iso3166Numeric;
	}

	public String getTopLevelDomain() {
		return this.topLevelDomain;
	}

	public String getOlympicCountryCodeIOC() {
		return this.olympicCountryCodeIOC;
	}

	public String getLanguage() {
		return this.language;
	}

	private int countValue(String value) {
		if (value != null && value.length() > 0) {
			return 1;
		}
		return 0;
	}

	public int countNotEmptyFields() {
/*		return countValue(getVorname()) + countValue(getName())
				+ countValue(getZusatz()) + countValue(getAdresse())
				+ countValue(getPlz()) + countValue(getOrt())
				+ countValue(getTelefon()) + countValue(getFax())
				+ countValue(getEmail());
*/
		System.out.print("countNotEmptyFields() in LandIsoEntry not implemented");
		return 0;
	}

	public String toString() {
/*		return getName() + ", " + getZusatz() + ", " + getAdresse() + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ getPlz() + " " + getOrt() + " " + getTelefon(); //$NON-NLS-1$ //$NON-NLS-2$
	*/
		System.out.print("toString() in LandIsoEntry not implemented");
		return "toString not implemented";
	}
}
