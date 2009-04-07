package ch.marlovits.plz;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

import ch.elexis.Desk;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.GlobalEvents.ActivationListener;
import ch.elexis.actions.GlobalEvents.SelectionListener;
import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.elexis.util.LabeledInputField.AutoForm;
import ch.elexis.util.LabeledInputField.InputData;

public class PLZBlatt extends Composite implements SelectionListener, ActivationListener{
	static final String[] types={"istOrganisation","istLabor","istPerson","istPatient","istAnwender","istMandant"};
	private IViewSite site;
	private ScrolledForm form;
	private FormToolkit tk;
	AutoForm afDetails;
	
	static final InputData[] def=new InputData[]{
		new InputData("land"),
		new InputData("landiso3"),
		new InputData("plz"),
		new InputData("ort"),
		new InputData("kanton")
	};
	private Plz actPLZ;
	private Label lbAnschrift;
	
	public PLZBlatt(Composite parent, int style, IViewSite vs){
		super(parent,style);
		site=vs;
		tk=Desk.getToolkit();
		setLayout(new FillLayout());
		form=tk.createScrolledForm(this);
		Composite body=form.getBody();
		body.setLayout(new GridLayout());
		Composite cTypes=tk.createComposite(body,SWT.BORDER);
		cTypes.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		cTypes.setLayout(new FillLayout());
		
		Composite bottom=tk.createComposite(body);
		bottom.setLayout(new FillLayout());
		bottom.setLayoutData(SWTHelper.getFillGridData(1,true,1,true));
		actPLZ=(Plz)GlobalEvents.getInstance().getSelectedObject(Plz.class);
		afDetails=new AutoForm(bottom,def);
		Composite cAnschrift=tk.createComposite(body);
		cAnschrift.setLayout(new GridLayout(2, false));
		cAnschrift.setLayoutData(SWTHelper.getFillGridData(1,true,1,false));
		Hyperlink hAnschrift=tk.createHyperlink(cAnschrift,"Anschrift",SWT.NONE);
		hAnschrift.addHyperlinkListener(new HyperlinkAdapter(){

			// TODO: die folgenden 5 Zeilen
			//@Override
			//public void linkActivated(HyperlinkEvent e) {
			//	new AnschriftEingabeDialog(getShell(),actPLZ).open();
			//	GlobalEvents.getInstance().fireSelectionEvent(actPLZ);
			//}
			
		});
		lbAnschrift=tk.createLabel(cAnschrift,"",SWT.WRAP);
		lbAnschrift.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		//GlobalEvents.getInstance().addSelectionListener(this);
		GlobalEvents.getInstance().addActivationListener(this, site.getPart());
	}
	
	public void selectionEvent(PersistentObject obj) {
		// TODO
	}
	@Override
	public void dispose(){
		GlobalEvents.getInstance().removeActivationListener(this, site.getPart());
		super.dispose();
	}
	private final class TypButtonAdapter extends SelectionAdapter {
		ArrayList<String> alTypes=new ArrayList<String>();
		ArrayList<String> alValues=new ArrayList<String>();
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button b=(Button)e.getSource();
			String type=(String)b.getData();
			
			if(b.getSelection()==true){
				if(type.equals("istOrganisation")){
					select("1","x","0","0","0","0");
					def[0].setLabel("Bezeichnung");
					def[1].setLabel("Zusatz");
					def[2].setLabel("Ansprechperson");
					def[3].setText("");
					def[10].setLabel("Tel. direkt");
				}else if(type.equals("istLabor")){
					select("1","1","0","0","0","0");
					def[0].setLabel("Bezeichnung");
					def[1].setLabel("Zusatz");
					def[2].setLabel("Laborleiter");
					def[10].setLabel("Tel. direkt");
				}else{
					def[0].setLabel("Name");
					def[1].setLabel("Vorname");
					def[2].setLabel("Zusatz");
					def[10].setLabel("Mobil");
					if("istPerson".equals( type )){
						select("0","0","1","x","x","x");
					}else if(type.equals("istPatient")){
						select("0","0","1","1","x","x");
					}else if(type.equals("istAnwender")){
						select("0","0","1","x","1","x");
					}else if(type.equals("istMandant")){
						select("0","0","1","x","1","1");
					}
				}
			}else{
				actPLZ.set(type,"0");
			}
		}
		void select(String... fields){
			alTypes.clear();
			alValues.clear();
			for(int i=0;i<fields.length;i++){
				if(fields[i].equals("x")){
					continue;
				}
				alTypes.add(types[i]);
				alValues.add(fields[i]);
			}
			actPLZ.set(alTypes.toArray(new String[0]),alValues.toArray(new String[0]));
		}
	}
	public void activation(boolean mode) {
		if(GlobalEvents.getInstance().getSelectedObject(Plz.class)==null){
			setEnabled(false);
		}else{
			setEnabled(true);
		}
		
	}

	public void visible(boolean mode) {
		if(mode==true){
			selectionEvent(GlobalEvents.getInstance().getSelectedObject(Plz.class));
			GlobalEvents.getInstance().addSelectionListener(this);
		}else{
			GlobalEvents.getInstance().removeSelectionListener(this);
		}
		
	}

	public void clearEvent(Class template) {
		setEnabled(false);
		
	}

}
