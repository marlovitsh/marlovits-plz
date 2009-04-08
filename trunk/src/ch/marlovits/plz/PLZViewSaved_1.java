package ch.marlovits.plz;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.Hub;
import ch.elexis.actions.GlobalActions;
import ch.elexis.actions.GlobalEvents;
import ch.elexis.actions.ListLoader;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Plz;
import ch.elexis.data.Query;
import ch.elexis.util.ViewMenus;
import ch.elexis.util.viewers.CommonViewer;
import ch.elexis.util.viewers.DefaultControlFieldProvider;
import ch.elexis.util.viewers.DefaultLabelProvider;
import ch.elexis.util.viewers.LazyContentProvider;
import ch.elexis.util.viewers.SimpleWidgetProvider;
import ch.elexis.util.viewers.ViewerConfigurer;
import ch.elexis.util.viewers.ViewerConfigurer.ControlFieldListener;
import ch.rgw.tools.ExHandler;


public abstract class PLZViewSaved_1 extends ViewPart implements ControlFieldListener, ISaveablePart2{
	public static final String ID="ch.marlovits.plzSaved";
	private CommonViewer cv;
	private ViewerConfigurer vc;
	private String[] fields={"Land","Plz","Ort","Kanton"};
	//private String[] fields={"Kuerzel","Bezeichnung1","Bezeichnung2","Strasse","Plz","Ort"};

	private ViewMenus menu;
	
	ListLoader dataloader=new ListLoader("PLZ",new Query<Plz>(Plz.class),new String[]{"Land","Plz","Ort","Kanton"});
	//ListLoader dataloader=new ListLoader("PLZ",new Query<Plz>(Plz.class),new String[]{"Bezeichnung1","Bezeichnung2"});

    public PLZViewSaved_1() {
        //++++++++Hub.jobPool.addJob(dataloader);
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		cv=new CommonViewer();
		 vc=new ViewerConfigurer(
	         		//new ViewerConfigurer.DefaultContentProvider(cv, Anschrift.class),
	         		new LazyContentProvider(cv,dataloader, null),
	         		//new LazyContentProvider(cv,dataloader, "PLZ/Anzeigen"),
	         		new DefaultLabelProvider(),
	         		new DefaultControlFieldProvider(cv, fields),
	         		new ViewerConfigurer.DefaultButtonProvider(cv,Plz.class),
	         		new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST, SWT.NONE,null)
	         );
         cv.create(vc,parent,SWT.NONE,getViewSite());
         menu=new ViewMenus(getViewSite());
         Action delPlz=new Action("Löschen"){
        	@Override
        	public void run(){
        		Object[] o=cv.getSelection();
        		if(o!=null){
        			Plz k=(Plz)o[0];
        			k.delete();
        			cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
        		}
        	}
         };
         Action dupPlz=new Action("Postleitzahl duplizieren"){
        	 @Override
         	public void run(){
         		Object[] o=cv.getSelection();
         		if(o!=null){
         			Plz k=(Plz)o[0];
         			Plz dup;
         				// TODO: hier muss das Duplizieren noch erstellt werden
         				//Person p=Person.load(k.getId());
         				//dup=new Person(p.getName(),p.getVorname(),p.getGeburtsdatum(),p.getGeschlecht());

         				//Organisation org=Organisation.load(k.getId());
         				//dup=new Organisation(org.get("Name"),org.get("Zusatz1"));

         			cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
         			//cv.getViewerWidget().refresh();
         		}
         	}
         };
         // TODO: ContextMenu für PLZ
         menu.createViewerContextMenu(cv.getViewerWidget(),delPlz,dupPlz);
         menu.createMenu(GlobalActions.printKontaktEtikette);
         // TODO: ToolBar für PLZ
         menu.createToolbar(GlobalActions.printKontaktEtikette);
         //cv.getViewerWidget().addSelectionChangedListener(GlobalEvents.getInstance().getDefaultListener());
         ((LazyContentProvider)vc.getContentProvider()).startListening();
         vc.getControlFieldProvider().addChangeListener(this);
         cv.addDoubleClickListener(new CommonViewer.DoubleClickListener(){
			public void doubleClicked(PersistentObject obj, CommonViewer cv) {
				try {
					// TODO: hier muss noch die Detail-View für PLZ erstellt werden
					PLZDetailView PLZdv=(PLZDetailView)getSite().getPage().showView(PLZDetailView.ID);
					//KontaktDetailView kdv=(KontaktDetailView)getSite().getPage().showView(KontaktDetailView.ID);
					//+++++++kdv.kb.selectionEvent(obj);
				} catch (PartInitException e) {
					ExHandler.handle(e);
				}
				
			}
         });
	}
	public void dispose(){
		((LazyContentProvider)vc.getContentProvider()).stopListening();
		vc.getControlFieldProvider().removeChangeListener(this);
		super.dispose();
	}
	
	@Override
	public void setFocus() {
		vc.getControlFieldProvider().setFocus();
	}

	public void changed(String[] fields, String[] values) {
		GlobalEvents.getInstance().clearSelection(Plz.class);
	}

	public void reorder(String field) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * ENTER has been pressed in the control fields, select the first listed patient
	 */
	// this is also implemented in PatientenListeView
	public void selected() {
    	StructuredViewer viewer = cv.getViewerWidget();
    	Object[] elements = cv.getConfigurer().getContentProvider().getElements(viewer.getInput());
    	
    	if (elements != null && elements.length > 0) {
    		Object element = elements[0];
    		/*
    		 * just selecting the element in the viewer doesn't work if the
    		 * control fields are not empty (i. e. the size of items changes):
    		 *   cv.setSelection(element, true);
    		 * bug in TableViewer with style VIRTUAL?
    		 * work-arount: just globally select the element without visual
    		 * representation in the viewer
    		 */
    		if (element instanceof PersistentObject) {
    			// globally select this object
    			GlobalEvents.getInstance().fireSelectionEvent((PersistentObject) element);
    		}
    	}
	}

	/* ******
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2
	 * Wir benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */ 
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}
	public void doSave(IProgressMonitor monitor) { /* leer */ }
	public void doSaveAs() { /* leer */}
	public boolean isDirty() {
		return true;
	}
	public boolean isSaveAsAllowed() {
		return false;
	}
	public boolean isSaveOnCloseNeeded() {
		return true;
	}

/*	@Override
	public void changed(HashMap<String, String> values) {
		// TODO Auto-generated method stub
		
	}
*/
}
