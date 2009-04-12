package ch.marlovits.plz;

import java.lang.reflect.Method;

import ch.elexis.data.PersistentObject;
import ch.elexis.data.PersistentObjectFactory;

public class LandEintragFactory extends PersistentObjectFactory {
	
	public LandEintragFactory(){}
	
	@Override
	public PersistentObject createFromString(String code){
		try {
			String[] ci = code.split("::");
			Class clazz = Class.forName(ci[0]);
			Method load = clazz.getMethod("load", new Class[] {
				String.class
			});
			return (PersistentObject) (load.invoke(null, new Object[] {
				ci[1]
			}));
		} catch (Exception ex) {
			// ExHandler.handle(ex);
			return null;
		}
	}
	
	@Override
	protected PersistentObject doCreateTemplate(Class typ){
		try {
			return (PersistentObject) typ.newInstance();
		} catch (Exception e) {
			// ExHandler.handle(e);
			return null;
		}
	}
	
}
