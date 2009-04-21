package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import ch.elexis.data.PersistentObject;

enum CountryCodesInputFields {	iso2,
								iso3,
								isoNum,
								fips,
								name,
								capital,
								area,
								population,
								continent,
								tld,
								currencyCode,
								currencyName,
								phone,
								postalCodeFormat,
								postalCodeRegex,
								languages,
								geonameId,
								neigbours,
								equivalentfips
								}


public class DataImporter {
	// *** Konstanten für die Iso-Land-Daten
	private static final String COUNTRYCODES_URL = "http://download.geonames.org/export/dump/countryInfo.txt";
	private static final String COUNTRYCODES_FIELDDELIMITER = "\t";
	
	public DataImporter() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Importiert von GeoNames die aktuelle Iso-Länderliste. 
	 * Falls schon Einträge vorhanden sind, werden alle als deleted markiert.
	 * Für jeden neuen Eintrag wird der alte gelöscht (Iso2 ist gleich)
	 * Auf diese Weise wird die Liste aktualisiert; wenn Länder in der Source-Liste
	 * gelöscht werden, dann verbleibt der Eintrag als gelöscht in der Datenbank.
	 * So bleiben Referenzen gültig, das Land kann aber nicht mehr neu ausgewählt werden.
	 */
	static public void importCountryData()	{
		try {
			URL url = new URL(COUNTRYCODES_URL);
			URLConnection urlConnection = url.openConnection();
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			// Alle bestehenden Einträge als deleted markieren
			String inputLine;
			while ((inputLine = inputStream.readLine()) != null)	{
				String firstChar = inputLine.substring(0, 1);
				if (!firstChar.equals("#"))	{	// skip comment lines
					System.out.println(inputLine);
					String[] fields = inputLine.split(COUNTRYCODES_FIELDDELIMITER);
					// den Eintrag mit der aktuellen iso2 aus der Datenbank löschen
					// zuvor die User-Werte zwischenspeichern, damit sie wieder rückgespielt werden könne
					// entrylanguage, postalcodemessage, landsorting, strasseerlaubt, kantonauswaehlen
					// fields[CountryCodesInputFields.iso2.			ordinal()]
					CountryEintrag tmpCountry = new CountryEintrag("");
					new CountryEintrag(	fields[CountryCodesInputFields.iso2.			ordinal()],
										fields[CountryCodesInputFields.iso3.			ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.isoNum.ordinal()]),
										fields[CountryCodesInputFields.fips.			ordinal()],
										fields[CountryCodesInputFields.name.			ordinal()],
										fields[CountryCodesInputFields.tld.				ordinal()],
										fields[CountryCodesInputFields.currencyCode.	ordinal()],
										fields[CountryCodesInputFields.currencyName.	ordinal()],
										fields[CountryCodesInputFields.phone.			ordinal()],
										fields[CountryCodesInputFields.postalCodeFormat.ordinal()],
										fields[CountryCodesInputFields.postalCodeRegex.	ordinal()],
										fields[CountryCodesInputFields.languages.		ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.geonameId.ordinal()]),
										(fields.length - 1 < CountryCodesInputFields.neigbours.ordinal()) ? null : fields[CountryCodesInputFields.neigbours.ordinal()],
										"de");
				}
			}
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        /*
        
        
		CountryEintrag(iso2, iso3,
				  isoNum,
				  fips,
				  name,
				  tld,
				  currencyCode,
				  currencyName,
				  phone,
				  postalCodeFormat,
				  postalCodeRegex,
				  languages,
				  geonameId,
				  neigbours,
				  entryLanguage) {
	*/
		}
	
}