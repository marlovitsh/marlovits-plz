package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ch.elexis.data.PersistentObject;
import ch.rgw.tools.JdbcLink;

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
	 * 
	 * Die geladene Iso-Liste enthält die offizielle aktuelle Version, allerdings in englisch
	 * Die benötigten weiteren Daten werden von geonames via Abfrage abgerufen (xml) und dann ebenfalls
	 * eingelesen
	 * http://ws.geonames.org/search?country=CH&featureCode=PCLI&lang=it
	 * 
	 * @param lang: die Sprache, für welche die Daten importiert werden sollen (de, en, fr, etc.)
	 */
	static public void importCountryData(final String lang)	{
		try {
			URL url = new URL(COUNTRYCODES_URL);
			URLConnection urlConnection = url.openConnection();
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			// grösstes lastupdate einlesen - alle Einträge, die <= lastUpdate sind, werden später als
			// deleted markiert, da alle noch vorhandnen anderen aktualisiert sind mit neuerem Zeitmarker
			StringBuffer sql = new StringBuffer(300);
			sql.append("select lastupdate from CH_MARLOVITS_COUNTRY order by lastupdate desc");
			String lastUpdate = PersistentObject.getConnection().queryString(sql.toString());
			//System.out.println(lastUpdate);
			// durch den Input gehen und alles einlesen
			String inputLine;
			while ((inputLine = inputStream.readLine()) != null)	{
				String firstChar = inputLine.substring(0, 1);
				if (!firstChar.equals("#"))	{	// skip comment lines
					//System.out.println(inputLine);
					String[] fields = inputLine.split(COUNTRYCODES_FIELDDELIMITER);
					// den bestehenden Eintrag einlesen oder einen neuen erstellen -> country
					String countryID = fields[CountryCodesInputFields.iso2.ordinal()] + "_en";
					CountryEintrag country = new CountryEintrag(countryID);
					// den gefundenen/erstellten Eintrag mit den Werten aktualisieren/füllen
					country.updateEntry(fields[CountryCodesInputFields.iso2.			ordinal()],
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
										"en");
					// nun den gewünschten Sprach-Eintrag einlesen
					// den bestehenden Eintrag einlesen oder einen neuen erstellen -> country
					countryID = fields[CountryCodesInputFields.iso2.ordinal()] + "_" + lang;
					country = new CountryEintrag(countryID);
					// den übersetzten Eintrag abfragen
					String countryNameLang = translateLandName(fields[CountryCodesInputFields.iso2.ordinal()], lang);
					// falls keine Übersetzung gefunden, wird der englische Name genommen
					if ((countryNameLang.equals("")) || (countryNameLang == null))	{
						countryNameLang = ""; //fields[CountryCodesInputFields.name.ordinal()];
					}
					// den gefundenen/erstellten Eintrag mit den Werten aktualisieren/füllen
					country.updateEntry(fields[CountryCodesInputFields.iso2.			ordinal()],
										fields[CountryCodesInputFields.iso3.			ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.isoNum.ordinal()]),
										fields[CountryCodesInputFields.fips.			ordinal()],
										countryNameLang,
										fields[CountryCodesInputFields.tld.				ordinal()],
										fields[CountryCodesInputFields.currencyCode.	ordinal()],
										fields[CountryCodesInputFields.currencyName.	ordinal()],
										fields[CountryCodesInputFields.phone.			ordinal()],
										fields[CountryCodesInputFields.postalCodeFormat.ordinal()],
										fields[CountryCodesInputFields.postalCodeRegex.	ordinal()],
										fields[CountryCodesInputFields.languages.		ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.geonameId.ordinal()]),
										(fields.length - 1 < CountryCodesInputFields.neigbours.ordinal()) ? null : fields[CountryCodesInputFields.neigbours.ordinal()],
										lang);
				}
			}
			// alle Einträge mit lastupdate <= lastUpdate als deleted markieren -
			// sind nicht mehr inder Liste vorhanden, ergo ab sofort ungültig
			sql = new StringBuffer(300);
			sql.append("UPDATE CH_MARLOVITS_COUNTRY set deleted = " + JdbcLink.wrap("1") + "where lastupdate <= " + lastUpdate + " and entrylanguage != " + JdbcLink.wrap("en") + " and entrylanguage != " + JdbcLink.wrap(lang));
			PersistentObject.getConnection().exec(sql.toString());
			// input schliessen
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 	}

	/**
	 * 
	 * @param landIso2: iso2-Länder-Code, für welches die Bezeichnung gefunden werden soll
	 * @param lang: in dieser Sprache soll die Landbezeichnung zurückgegeben werden
	 * 
	 * @return der übersetzte Name oder "", falls nicht gefunden
	 */
	public static String translateLandName(final String landIso2, final String lang)	{
		try {
			String[] strings = {"PCLF", "PCLI", "PCLIX", "PCLS"};
			for (String str : strings)	{
				// url errechnen und Seite (xml) einlesen
				String url = "http://ws.geonames.org/search?country=" + landIso2 + "&featureCode=" + str + "&lang=" + lang;
				String htmlPage = readHTMLPage(url);
				// ist zwar xml, es ist aber einfacher, einfach den Inhalt des ersten <name></name> einzulesen
				String startMarker = "<name>";
				String endMarker   = "</name>";
				int startOfName = htmlPage.indexOf(startMarker) + startMarker.length();
				if (startOfName != -1)	{
					int endOfName   = htmlPage.indexOf(endMarker, startOfName);
					if (endOfName != -1)	{
						return htmlPage.substring(startOfName, endOfName);
					}
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Liest Inhalt einer ganzen Seite einer übergebenen URL
	 * @param url: die einzulesende URL
	 * @return die ganze HTML-Seite als String
	 */
	public static String readHTMLPage(final String url) throws IOException, MalformedURLException{
		URL content = new URL(url);
		InputStream input = content.openStream();
		StringBuffer sb = new StringBuffer();
		int count = 0;
		char[] c = new char[10000];
		InputStreamReader isr = new InputStreamReader(input);
		try {
			while ((count = isr.read(c)) > 0) {
				sb.append(c, 0, count);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return sb.toString();
	}	
}