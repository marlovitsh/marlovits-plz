/*******************************************************************************
 * Copyright (c) 2008 Harald Marlovits.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Harald Marlovits	 - initial implementation
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/


package ch.marlovits.plz;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;
import org.eclipse.swt.widgets.Widget;

/**
 * The CCombo class represents a selectable user interface object
 * that combines a text field and a list and issues notificiation
 * when an item is selected from the list.
 * <p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to add children to it, or set a layout on it.
 * </p>
 * <dl>
 * <dt><b>Styles:</b>
 * <dd>BORDER, READ_ONLY, FLAT</dd>
 * <dt><b>Events:</b>
 * <dd>Selection</dd>
 * </dl>
 */
public final class MyCComboSaved extends Composite {

	static final int ITEMS_SHOWING = 5;

	Text text;
	List list;
	Shell popup;
	boolean hasFocus;
	String theText;
	TableViewer	dropTable;
	Shell popup2;
	
/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 *
 * @see SWT#BORDER
 * @see SWT#READ_ONLY
 * @see SWT#FLAT
 * @see Widget#getStyle
 */	
public MyCComboSaved (Composite parent, int style) {
	super (parent, checkStyle (style));
	
	style = getStyle();
	
	// das Textfeld
	int textStyle = SWT.SINGLE;
	if ((style & SWT.READ_ONLY) != 0) textStyle |= SWT.READ_ONLY;
	if ((style & SWT.FLAT) != 0) textStyle |= SWT.FLAT;
	text = new Text (this, textStyle);
	
	// die Shell, in welcher die Liste erstellt wird
	popup = new Shell (getShell (), SWT.NO_TRIM);
	
	// die Popup-Liste, erstellt in Popup-Shell
	int listStyle = SWT.SINGLE | SWT.V_SCROLL;
	if ((style & SWT.FLAT) != 0) listStyle |= SWT.FLAT;
	if ((style & SWT.RIGHT_TO_LEFT) != 0) listStyle |= SWT.RIGHT_TO_LEFT;
	if ((style & SWT.LEFT_TO_RIGHT) != 0) listStyle |= SWT.LEFT_TO_RIGHT;
	list = new List (popup, listStyle);
	/*
	// meine eigene Popup-Liste, mehrere Spalten
	// die Shell, in welcher die Liste erstellt wird
	popup2 = new Shell (getShell (), SWT.NO_TRIM);
	
    TableViewer dropTable = new TableViewer(popup2, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    GridData gd = new GridData();
    gd.horizontalAlignment = SWT.FILL;
    gd.verticalAlignment = SWT.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;

    dropTable.getControl().setLayoutData(gd);
    Table myTable2 = dropTable.getTable();
    myTable2 .setHeaderVisible(false);
    myTable2 .setLinesVisible(false);
	// Die einzelnen Einträge abfragen und in einen String-Array und dann in die Liste schreiben
	//rs = stm.query("select ort27 from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(ort27) like lower(" + JdbcLink.wrap(currText + "%") + ")  and plztyp != 80 order by ort27");
	Stm stm = PersistentObject.getConnection().getStatement();
	String shownFields = "ort27, plz, land";
	String landClause = " lower(land) = lower(" + JdbcLink.wrap("CH") + ") "; 
	ResultSet rs = stm.query("select " + shownFields + " from " + PlzEintrag.getTableName2() + " where " + landClause + " and lower(ort27) like lower(" + JdbcLink.wrap("D" + "%") + ")  and plztyp != 80 order by ort27");
	for (int iiii = 0; iiii < 3; iiii++)	{
	      TableColumn col1 = new TableColumn(myTable2, SWT.NULL);
	      col1.setText("Column " + iiii);
	      col1.pack();			
	}
	try {
		while (rs.next())	{
		      TableItem tableItem1 = new TableItem(myTable2, SWT.NULL);
		      tableItem1.setText(new String[] {rs.getString("ort27"), rs.getString("plz"), rs.getString("land")});
		}
	} catch (SQLException e1) {
	}
     myTable2.setTopIndex(40);
     
     
     
     */
     
	Listener listener = new Listener () {
		public void handleEvent (Event event) {
			if (popup == event.widget) {
				System.out.println("event.widget == popup, calling popupEvent(event)");
				popupEvent (event);
				return;
			}
			if (text == event.widget) {
				//System.out.println("event.widget == text");
				textEvent (event);
				return;
			}
			if (list == event.widget) {
				System.out.println("event.widget == list, calling listEvent(event)");
				listEvent (event);
				return;
			}
			if (MyCComboSaved.this == event.widget) {
				System.out.println("event.widget == MyCCombo, calling comboEvent(event)");
				comboEvent (event);
				return;
			}

		}
	};
	
	int [] comboEvents = {SWT.Dispose, SWT.Move, SWT.Resize};
	for (int i=0; i<comboEvents.length; i++) this.addListener (comboEvents [i], listener);
	
	int [] popupEvents = {SWT.Close, SWT.Paint, SWT.Deactivate};
	for (int i=0; i<popupEvents.length; i++) popup.addListener (popupEvents [i], listener);
	
	int [] textEvents = {SWT.KeyDown, SWT.KeyUp, SWT.Modify, SWT.MouseDown, SWT.MouseUp, SWT.Traverse, SWT.FocusIn, SWT.FocusOut, SWT.DEL};
	for (int i=0; i<textEvents.length; i++) text.addListener (textEvents [i], listener);
	
	int [] listEvents = {SWT.MouseUp, SWT.Selection, SWT.Traverse, SWT.KeyDown, SWT.KeyUp, SWT.FocusIn, SWT.FocusOut};
	for (int i=0; i<listEvents.length; i++) list.addListener (listEvents [i], listener);
	
	initAccessible();
}
public List getList()	{
	return list;
}
public void setListVisible(boolean visibility)	{
	list.setVisible(visibility);
}
static int checkStyle (int style) {
	int mask = SWT.BORDER | SWT.READ_ONLY | SWT.FLAT | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
	return style & mask;
}
/**
* Adds an item.
* <p>
* The item is placed at the end of the list.
* Indexing is zero based.
*
* @param string the new item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when the string is null
* @exception SWTError(ERROR_ITEM_NOT_ADDED)
*	when the item cannot be added
*/
public void add (String string) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	list.add (string);
}
/**
* Adds an item at an index.
* <p>
* The item is placed at an index in the list.
* Indexing is zero based.
*
* This operation will fail when the index is
* out of range.
*
* @param string the new item
* @param index the index for the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when the string is null
* @exception SWTError(ERROR_ITEM_NOT_ADDED)
*	when the item cannot be added
*/
public void add (String string, int index) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	list.add (string, index);
}
/**	 
* Adds the listener to receive events.
* <p>
*
* @param listener the listener
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when listener is null
*/
public void addModifyListener (ModifyListener listener) {;
//System.out.println("addModifyListener");
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Modify, typedListener);
	//System.out.println("ModifyListener");
}
/**	 
* Adds the listener to receive events.
* <p>
*
* @param listener the listener
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when listener is null
*/
public void addSelectionListener(SelectionListener listener) {
	//System.out.println("addSelectionListener");
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	TypedListener typedListener = new TypedListener (listener);
	addListener (SWT.Selection,typedListener);
	addListener (SWT.DefaultSelection,typedListener);
}

/**
* Clears the current selection.
* <p>
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public void clearSelection () {
	checkWidget();
	text.clearSelection ();
	list.deselectAll ();
}
void comboEvent (Event event) {
	switch (event.type) {
		case SWT.Dispose:
			if (popup != null && !popup.isDisposed ()) popup.dispose ();
			popup = null;  
			text = null;  
			list = null;  
			break;
		case SWT.Move:
			//System.out.println("SWT.Move");
			dropDown(false);
			break;
		case SWT.Resize:
			internalLayout();
			break;
	}
}

public Point computeSize (int wHint, int hHint, boolean changed) {
	checkWidget();
	int width = 0, height = 0;
	Point textSize  = text.computeSize (wHint, SWT.DEFAULT, changed);
	Point listSize  = list.computeSize (wHint, SWT.DEFAULT, changed);
	int borderWidth = getBorderWidth();
	
	height = Math.max (hHint, textSize.y  + 2*borderWidth);
	width = Math.max (wHint, Math.max(textSize.x + 2*borderWidth, listSize.x + 2)  );
	return new Point (width, height);
}
/**
* Deselects an item.
* <p>
* If the item at an index is selected, it is
* deselected.  If the item at an index is not
* selected, it remains deselected.  Indices
* that are out of range are ignored.  Indexing
* is zero based.
*
* @param index the index of the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public void deselect (int index) {
	checkWidget();
	list.deselect (index);
}
/**
* Deselects all items.
* <p>
*
* If an item is selected, it is deselected.
* If an item is not selected, it remains unselected.
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public void deselectAll () {
	checkWidget();
	list.deselectAll ();
}
public void dropDown (boolean drop) {
	if (drop == isDropped ()) return;
	if (!drop) {
		popup.setVisible (false);
		text.setFocus();
		return;
	}

	int index = list.getSelectionIndex ();
	if (index != -1) list.setTopIndex (index);
	Rectangle listRect = list.getBounds ();
	Display display = getDisplay ();
	Rectangle rect = display.map (getParent (), null, getBounds ());
	Point comboSize = getSize ();
	int width = Math.max (comboSize.x, listRect.width + 2);
	popup.setBounds (rect.x, rect.y + comboSize.y, width, listRect.height + 2);
	popup.setVisible (true);
	//popup2.setBounds (rect.x, rect.y + comboSize.y, width, listRect.height + 2);
	//popup2.setVisible (true);
	//list.setFocus ();
}
public Control [] getChildren () {
	checkWidget();
	return new Control [0];
}
boolean getEditable () {
	return text.getEditable ();
}
/**
* Gets an item at an index.
* <p>
* Indexing is zero based.
*
* This operation will fail when the index is out
* of range or an item could not be queried from
* the OS.
*
* @param index the index of the item
* @return the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_CANNOT_GET_ITEM)
*	when the operation fails
*/
public String getItem (int index) {
	checkWidget();
	return list.getItem (index);
}
/**
* Gets the number of items.
* <p>
* This operation will fail if the number of
* items could not be queried from the OS.
*
* @return the number of items in the widget
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_CANNOT_GET_COUNT)
*	when the operation fails
*/
public int getItemCount () {
	checkWidget();
	return list.getItemCount ();
}
/**
* Gets the height of one item.
* <p>
* This operation will fail if the height of
* one item could not be queried from the OS.
*
* @return the height of one item in the widget
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_CANNOT_GET_ITEM_HEIGHT)
*	when the operation fails
*/
public int getItemHeight () {
	checkWidget();
	return list.getItemHeight ();
}
/**
* Gets the items.
* <p>
* This operation will fail if the items cannot
* be queried from the OS.
*
* @return the items in the widget
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_CANNOT_GET_ITEM)
*	when the operation fails
*/
public String [] getItems () {
	checkWidget();
	return list.getItems ();
}
/**
* Gets the selection.
* <p>
* @return a point representing the selection start and end
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public Point getSelection () {
	checkWidget();
	return text.getSelection ();
}
/**
* Gets the index of the selected item.
* <p>
* Indexing is zero based.
* If no item is selected -1 is returned.
*
* @return the index of the selected item.
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public int getSelectionIndex () {
	checkWidget();
	return list.getSelectionIndex ();
}
/**
* Gets the widget text.
* <p>
* If the widget has no text, an empty string is returned.
*
* @return the widget text
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public String getText () {
	checkWidget();
	return text.getText ();
}
/**
* Gets the height of the combo's text field.
* <p>
* The operation will fail if the height cannot 
* be queried from the OS.

* @return the height of the combo's text field.
* 
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_ERROR_CANNOT_GET_ITEM_HEIGHT)
*	when the operation fails
*/
public int getTextHeight () {
	checkWidget();
	return text.getLineHeight();
}
/**
* Gets the text limit.
* <p>
* @return the text limit
* 
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public int getTextLimit () {
	checkWidget();
	return text.getTextLimit ();
}
/**
* Gets the index of an item.
* <p>
* The list is searched starting at 0 until an
* item is found that is equal to the search item.
* If no item is found, -1 is returned.  Indexing
* is zero based.
*
* @param string the search item
* @return the index of the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when string is null
*/
public int indexOf (String string) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	return list.indexOf (string);
}
/**
* Gets the index of an item.
* <p>
* The widget is searched starting at start including
* the end position until an item is found that
* is equal to the search itenm.  If no item is
* found, -1 is returned.  Indexing is zero based.
*
* @param string the search item
* @param index the starting position
* @return the index of the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when string is null
*/
public int indexOf (String string, int start) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	return list.indexOf (string, start);
}

void initAccessible() {
	getAccessible().addAccessibleListener(new AccessibleAdapter() {
		public void getHelp(AccessibleEvent e) {
			e.result = getToolTipText();
		}
	});
		
	getAccessible().addAccessibleControlListener(new AccessibleControlAdapter() {
		public void getChildAtPoint(AccessibleControlEvent e) {
			Point testPoint = toControl(new Point(e.x, e.y));
			if (getBounds().contains(testPoint)) {
				e.childID = ACC.CHILDID_SELF;
			}
		}
		
		public void getLocation(AccessibleControlEvent e) {
			Rectangle location = getBounds();
			Point pt = toDisplay(new Point(location.x, location.y));
			e.x = pt.x;
			e.y = pt.y;
			e.width = location.width;
			e.height = location.height;
		}
		
		public void getChildCount(AccessibleControlEvent e) {
			e.detail = 0;
		}
		
		public void getRole(AccessibleControlEvent e) {
			e.detail = ACC.ROLE_COMBOBOX;
		}
		
		public void getState(AccessibleControlEvent e) {
			e.detail = ACC.STATE_NORMAL;
		}

		public void getValue(AccessibleControlEvent e) {
			e.result = getText();
		}
	});
}
boolean isDropped () {
	return popup.getVisible ();
}
public boolean isFocusControl () {
	checkWidget();
	if (text.isFocusControl() || list.isFocusControl() || popup.isFocusControl()) {
		return true;
	} else {
		return super.isFocusControl();
	}
}

/**
 * Neu Berechnen/Zeichnen des ganzen Konstrukts
 */
void internalLayout () {
	if (isDropped ()) dropDown (false);
	
	Rectangle rect = getClientArea();
	int width = rect.width;
	int height = rect.height;
	text.setBounds (0, 0, width, height);
	
	Point size = getSize();
	int itemHeight = list.getItemHeight () * ITEMS_SHOWING;
	Point listSize = list.computeSize (SWT.DEFAULT, itemHeight);
	list.setBounds (1, 1, Math.max (size.x - 2, listSize.x), listSize.y);
}

void listEvent (Event event) {
	System.out.println("listEvent called");
	switch (event.type) {
		case SWT.FocusIn: {
			if (hasFocus) return;
			hasFocus = true;
			if (getEditable ()) text.selectAll ();
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.FocusIn, e);
			break;
		}
		case SWT.FocusOut: {
			//System.out.println("SWT.FocusOut");
			event.display.asyncExec(new Runnable() {
				public void run() {
					if (MyCComboSaved.this.isDisposed()) return;
					Control focusControl = getDisplay().getFocusControl();
					if (focusControl == text) return;
					hasFocus = false;
					Event e = new Event();
					notifyListeners(SWT.FocusOut, e);
				}
			});
			break;
		}
		case SWT.MouseUp: {
			if (event.button != 1) return;
			dropDown (false);
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.DefaultSelection, e);
			break;
		}
		case SWT.Selection: {
			//System.out.println("SWT.Selection");
			int index = list.getSelectionIndex ();
			if (index == -1) return;
			text.setText (list.getItem (index));
			text.selectAll ();
			list.setSelection(index);
			Event e = new Event();
			e.time = event.time;
			e.stateMask = event.stateMask;
			e.doit = event.doit;
			notifyListeners(SWT.Selection, e);
			event.doit = e.doit;
			break;
		}
		case SWT.Traverse: {
			//System.out.println("SWT.Traverse: before switch");
			switch (event.detail) {
				case SWT.TRAVERSE_TAB_NEXT:
				case SWT.TRAVERSE_RETURN:
				case SWT.TRAVERSE_ESCAPE:
				case SWT.TRAVERSE_ARROW_PREVIOUS:
				case SWT.TRAVERSE_ARROW_NEXT:
					event.doit = false;
					break;
			}
			//System.out.println("SWT.Traverse: after switch");
			Event e = new Event();
			e.time = event.time;
			e.detail = event.detail;
			e.doit = event.doit;
			e.keyCode = event.keyCode;
//			if	((event.keyCode == SWT.ARROW_LEFT) || (event.keyCode == SWT.ARROW_RIGHT))	{
//				event.doit = false;
//				e.doit = false;
//				System.out.println("traverse cancel");
//				break;
//			}
			notifyListeners(SWT.Traverse, e);
			System.out.println("event.keyCode: " + event.keyCode);
			event.doit = e.doit;
			break;
		}
		case SWT.KeyUp: {		
			//System.out.println("SWT.KeyUp");
			Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			if	((event.keyCode == SWT.ARROW_LEFT) || (event.keyCode == SWT.ARROW_RIGHT))	{
				event.doit = false;
				e.doit = false;
				//System.out.println("keyUp cancel");
				break;
			}
			notifyListeners(SWT.KeyUp, e);
			break;
		}
		case SWT.KeyDown: {
			//System.out.println("SWT.KeyDown");
			if (event.character == SWT.ESC) { 
				// escape key cancels popup list
				//dropDown (false);
			}
			if (event.character == SWT.CR || event.character == '\t') {
				// Enter and Tab cause default selection
				dropDown (false);
				Event e = new Event();
				e.time = event.time;
				e.stateMask = event.stateMask;
				notifyListeners(SWT.DefaultSelection, e);
			}
			//At this point the widget may have been disposed.
			// If so, do not continue.
			if (isDisposed()) break;
			Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			notifyListeners(SWT.KeyDown, e);
			break;
			
		}
	}
}
void popupEvent(Event event) {
	switch (event.type) {
		case SWT.Paint:
			// draw black rectangle around list
			System.out.println("popup: draw");
			Rectangle listRect = list.getBounds();
			Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
			event.gc.setForeground(black);
			event.gc.drawRectangle(0, 0, listRect.width + 1, listRect.height + 1);
			break;
		case SWT.Close:
			//System.out.println("popup: close");
			event.doit = false;
			//dropDown (false);
			break;
		case SWT.Deactivate:
			//System.out.println("popup: deactivate");
			//dropDown (false);
			break;
		default:
			//System.out.println("popup: alles andere");
	}
}
public void redraw (int x, int y, int width, int height, boolean all) {
	checkWidget();
	if (!all) return;
	Point location = text.getLocation();
	text.redraw(x - location.x, y - location.y, width, height, all);
	location = list.getLocation();
	list.redraw(x - location.x, y - location.y, width, height, all);
}

/**
* Removes an item at an index.
* <p>
* Indexing is zero based.
*
* This operation will fail when the index is out
* of range or an item could not be removed from
* the OS.
*
* @param index the index of the item
* @return the selection state
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_ITEM_NOT_REMOVED)
*	when the operation fails
*/
public void remove (int index) {
	checkWidget();
	list.remove (index);
}
/**
* Removes a range of items.
* <p>
* Indexing is zero based.  The range of items
* is from the start index up to and including
* the end index.
*
* This operation will fail when the index is out
* of range or an item could not be removed from
* the OS.
*
* @param start the start of the range
* @param end the end of the range
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_ITEM_NOT_REMOVED)
*	when the operation fails
*/
public void remove (int start, int end) {
	checkWidget();
	list.remove (start, end);
}
/**
* Removes an item.
* <p>
* This operation will fail when the item
* could not be removed from the OS.
*
* @param string the search item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when string is null
* @exception SWTError(ERROR_ITEM_NOT_REMOVED)
*	when the operation fails
*/
public void remove (String string) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	list.remove (string);
}
/**
* Removes all items.
* <p>
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public void removeAll () {
	checkWidget();
	text.setText (""); //$NON-NLS-1$
	list.removeAll ();
}
/**	 
* Removes the listener.
* <p>
*
* @param listener the listener
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when listener is null
*/
public void removeModifyListener (ModifyListener listener) {
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	removeListener(SWT.Modify, listener);	
}
/**	 
* Removes the listener.
* <p>
*
* @param listener the listener
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when listener is null
*/
public void removeSelectionListener (SelectionListener listener) {
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	removeListener(SWT.Selection, listener);
	removeListener(SWT.DefaultSelection,listener);	
}
/**
* Selects an item.
* <p>
* If the item at an index is not selected, it is
* selected. Indices that are out of
* range are ignored.  Indexing is zero based.
*
* @param index the index of the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
*/
public void select (int index) {
	checkWidget();
	if (index == -1) {
		list.deselectAll ();
		text.setText (""); //$NON-NLS-1$
		return;
	}
	if (0 <= index && index < list.getItemCount()) {
		if (index != getSelectionIndex()) {
			text.setText (list.getItem (index));
			text.selectAll ();
			list.select (index);
			list.showSelection ();
		}
	}
}
public void setBackground (Color color) {
	super.setBackground(color);
	if (text != null) text.setBackground(color);
	if (list != null) list.setBackground(color);
}
public boolean setFocus () {
	checkWidget();
	return text.setFocus ();
}
public void setFont (Font font) {
	super.setFont (font);
	text.setFont (font);
	list.setFont (font);
	internalLayout ();
}
public void setForeground (Color color) {
	super.setForeground(color);
	if (text != null) text.setForeground(color);
	if (list != null) list.setForeground(color);
}
/**
* Sets the text of an item; indexing is zero based.
*
* This operation will fail when the index is out
* of range or an item could not be changed in
* the OS.
*
* @param index the index for the item
* @param string the item
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when items is null
* @exception SWTError(ERROR_ITEM_NOT_MODIFIED)
*	when the operation fails
*/
public void setItem (int index, String string) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	list.setItem (index, string);
}
/**
* Sets all items.
*
* @param items the array of items
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when items is null
* @exception SWTError(ERROR_ITEM_NOT_ADDED)
*	when the operation fails
*/
public void setItems (String [] items) {
	checkWidget();
	if (items == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	int style = getStyle();
	if ((style & SWT.READ_ONLY) != 0) text.setText (""); //$NON-NLS-1$
	list.setItems (items);
}
/**
* Sets the new selection.
*
* @param selection point representing the start and the end of the new selection
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when selection is null
*/
public void setSelection (Point selection) {
	checkWidget();
	if (selection == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	text.setSelection (selection.x, selection.y);
}

/**
* Sets the widget text.
*
* @param string the widget text
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_NULL_ARGUMENT)
*	when string is null
*/
public void setText (String string) {
	checkWidget();
	if (string == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	int index = list.indexOf (string);
	if (index == -1) {
		list.deselectAll ();
		text.setText (string);
		return;
	}
	text.setText (string);
	text.selectAll ();
	list.setSelection (index);
	list.showSelection ();
}
/**
* Sets the text limit.
* 
* @param limit new text limit
*
* @exception SWTError(ERROR_THREAD_INVALID_ACCESS)
*	when called from the wrong thread
* @exception SWTError(ERROR_WIDGET_DISPOSED)
*	when the widget has been disposed
* @exception SWTError(ERROR_CANNOT_BE_ZERO)
*	when limit is 0
*/
public void setTextLimit (int limit) {
	checkWidget();
	text.setTextLimit (limit);
}

public void setToolTipText (String string) {
	checkWidget();
	super.setToolTipText(string);
	text.setToolTipText (string);		
}

public void setVisible (boolean visible) {
	super.setVisible(visible);
	if (!visible) popup.setVisible(false);
}

void textEvent (Event event) {
	switch (event.type) {
		case SWT.FocusIn: {
			System.out.println("textEvent: FocusIn");
			if (hasFocus) return;
			hasFocus = true;
			if (getEditable ()) text.selectAll ();
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.FocusIn, e);
			break;
		}
		case SWT.FocusOut: {
			System.out.println("textEvent: FocusOut");
			event.display.asyncExec(new Runnable() {
				public void run() {
					if (MyCComboSaved.this.isDisposed()) return;
					Control focusControl = getDisplay().getFocusControl();
					if (focusControl == list) return;
					hasFocus = false;
					Event e = new Event();
					notifyListeners(SWT.FocusOut, e);
				}
			});
			break;
		}
		case SWT.KeyDown: {
			System.out.println("textEvent: KeyDown");
			if (event.character == SWT.ESC) { // escape key cancels popup list
				dropDown (false);
				text.setText(theText);
				text.setSelection(text.getText().length());
				event.doit = false;
				break;
			}
			if (event.character == SWT.CR) {
				dropDown (false);
				Event e = new Event();
				e.time = event.time;
				e.stateMask = event.stateMask;
				notifyListeners(SWT.DefaultSelection, e);
				event.doit = false;
				break;
			}
			//At this point the widget may have been disposed.
			// If so, do not continue.
			if (isDisposed()) break;
			
			if (list.getTopIndex() != -1)dropDown (true);
			if (event.keyCode == SWT.ARROW_UP || event.keyCode == SWT.ARROW_DOWN) {
				int oldIndex = getSelectionIndex ();
				if (event.keyCode == SWT.ARROW_UP) {
					switch (oldIndex) {
					case -1: {
						theText = text.getText();
						select(getItemCount () - 1);
						text.setSelection(text.getText().length());
						event.doit = false;
						break;
					}
					case 0: {
						deselect (oldIndex);
						text.setText(theText);
						text.setSelection(text.getText().length());
						event.doit = false;
						break;
					}
					default: {
						select (Math.min (oldIndex - 1, getItemCount () - 1));
						text.setSelection(text.getText().length());
						event.doit = false;
					}
					}
				}
				if (event.keyCode == SWT.ARROW_DOWN) {
					switch (oldIndex) {
					case -1: {
						theText = text.getText();
						select(0);
						text.setSelection(text.getText().length());
						event.doit = false;
						break;
					}
					default: {
						if (oldIndex == getItemCount () - 1)	{
							deselect(oldIndex);
							text.setText(theText);
							text.setSelection(text.getText().length());
						} else {
							select (Math.min (oldIndex + 1, getItemCount () - 1));
							text.setSelection(text.getText().length());
						}
						event.doit = false;
						break;
					}
					}
				}
				
				if (oldIndex != getSelectionIndex ()) {
					Event e = new Event();
					e.time = event.time;
					e.stateMask = event.stateMask;
					//notifyListeners(SWT.Selection, e);
				}
				
				//At this point the widget may have been disposed.
				// If so, do not continue.
				if (isDisposed()) break;
			}
			
			// Further work : Need to add support for incremental search in 
			// pop up list as characters typed in text widget
			Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			//notifyListeners(SWT.KeyDown, e);
			//System.out.println("textEvent.KeyDown: " + e.character);
			break;
		}
		case SWT.KeyUp: {
			//if (1==1) break;
			System.out.println("textEvent: KeyUp");
			Event e = new Event();
			e.time = event.time;
			e.character = event.character;
			e.keyCode = event.keyCode;
			e.stateMask = event.stateMask;
			char ch = event.character;
			if (ch != SWT.DEL) {
				if (Character.isLetterOrDigit(ch) || (ch>= 32 && ch <= 126)) { 
					//text.insert("" + ch);
					notifyListeners(SWT.KeyUp, e);
				} else {
					text.setFocus();
					notifyListeners(SWT.KeyUp, e);
				}
			} else {
				//System.out.println("doDelete");
			}
			
			// erstes passendes Item immer schon automatisch auswählen
			long itemCount = list.getItemCount();
			if (itemCount > 0)	{
				System.out.println("first fitting item: " + list.getItem(0));
				//select(0);
			}
			//text.setSelection(5, 100);
			//System.out.println("textEvent.keyUp: " + e.character + "/" + e.keyCode);
			break;
		}
		case SWT.Modify: {
			//if (1==1) break;
			System.out.println("textEvent: Modify");
			list.deselectAll ();
			Event e = new Event();
			e.time = event.time;
			notifyListeners(SWT.Modify, e);
			//System.out.println("textEvent.Modify");
			break;
		}
		case SWT.MouseDown: {
			System.out.println("textEvent: MouseDown");
			if (event.button != 1) return;
			if (text.getEditable ()) return;
			boolean dropped = isDropped ();
			text.selectAll ();
			//if (!dropped) setFocus ();
			//dropDown (!dropped);
			dropDown(true);
			Event e = new Event();
			e.time = event.time;
			e.x = event.x;
			e.y = event.y;
			notifyListeners(SWT.MouseDown, event);
			break;
		}
		case SWT.MouseUp: {
			System.out.println("textEvent: FocusUp");
			if (event.button != 1) return;
			if (text.getEditable ()) return;
			text.selectAll ();
			break;
		}
		case SWT.Traverse: {		
			System.out.println("textEvent: Traverse");
			switch (event.detail) {
			case SWT.TRAVERSE_RETURN:
			case SWT.TRAVERSE_ARROW_PREVIOUS:
			case SWT.TRAVERSE_ARROW_NEXT:
			case SWT.ARROW_DOWN:
			case SWT.ARROW_UP:
				System.out.println("textEvent: first part");
				// The enter causes default selection and
				// the arrow keys are used to manipulate the list contents so
				// do not use them for traversal.
				event.doit = false;
				return;
				//break;
			}
			if (event.keyCode == SWT.TAB) {
				dropDown(false);
			}
			Event e = new Event();
			e.time = event.time;
			e.detail = event.detail;
			e.doit = event.doit;
			e.keyCode = event.keyCode;
			notifyListeners(SWT.Traverse, e);
			event.doit = e.doit;
			break;
		}
	}
	}
/*
public boolean isPrintableChar(char c)	{
	Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
	return (!Character.isISOControl(c)) &&
			c != KeyEvent.CHAR_UNDEFINED &&
			block != null &&
			block != Character.UnicodeBlock.SPECIALS;
	}
*/
}
