package ch.marlovits.plz;

import java.io.FileReader;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;

import au.com.bytecode.opencsv.CSVReader;
import ch.elexis.data.Query;
import ch.elexis.util.ImporterPage;

public class PlzImporter extends ImporterPage {
	
	@Override
	public Composite createPage(Composite parent){
		return new FileBasedImporter(parent, this);
	}
	
	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception{
		CSVReader reader = new CSVReader(new FileReader(results[0]), ';');
		monitor.beginTask("Importiere Postleitzahlen", 100);
		String[] line = reader.readNext();
		while ((line = reader.readNext()) != null) {
			if (line.length < 3) {
				continue;
			}
			monitor.subTask(line[1]);
			String id =
				new Query<Plz>(Plz.class).findSingle("Ziffer", "=", line[0]);
			if (id != null) {
				Plz pl = Plz.load(id);
				pl.set(new String[] {
					"Titel", "TP"
				}, line[1], line[2]);
			} else {
				/* Plz pl = */new Plz(line[0], line[1], line[2], line[3], line[4], line[5], line[6]);
			}
			
		}
		monitor.done();
		return Status.OK_STATUS;
	}
	
	@Override
	public String getDescription(){
		return "Postleitzahlen";
	}
	
	@Override
	public String getTitle(){
		return "Postleitzahlen";
	}
	
}
