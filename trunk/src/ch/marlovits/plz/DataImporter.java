package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

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
	
	static public void importCountryData()	{
		try {
			URL url = new URL(COUNTRYCODES_URL);
			URLConnection urlConnection = url.openConnection();
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String inputLine;
			while ((inputLine = inputStream.readLine()) != null)	{
				if (!inputLine.substring(0, 1).equals("#"))	{	// skip comment lines
					System.out.println(inputLine);
					String[] fields = inputLine.split(COUNTRYCODES_FIELDDELIMITER);
					System.out.println(fields[CountryCodesInputFields.iso2.			ordinal()]);
					System.out.println(fields[CountryCodesInputFields.postalCodeFormat.ordinal()]);
							System.out.println(fields[CountryCodesInputFields.postalCodeRegex.	ordinal()]);
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
										fields[CountryCodesInputFields.neigbours.		ordinal()],
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