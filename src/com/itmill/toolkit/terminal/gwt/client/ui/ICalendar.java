package com.itmill.toolkit.terminal.gwt.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.itmill.toolkit.terminal.gwt.client.ApplicationConnection;
import com.itmill.toolkit.terminal.gwt.client.DateTimeService;
import com.itmill.toolkit.terminal.gwt.client.UIDL;
import com.itmill.toolkit.terminal.gwt.client.ui.calendar.ICalendarEntry;
import com.itmill.toolkit.terminal.gwt.client.ui.datefield.ICalendarPanel;

public class ICalendar extends IDateField {

    private ICalendarPanel calPanel;

    private HTMLTable hourTable;

    private EntrySource entrySource;

    private TableListener ftListener = new HourTableListener();

    private int realResolution = RESOLUTION_DAY;

    private static final String CLASSNAME = IDateField.CLASSNAME
	    + "-entrycalendar";

    public ICalendar() {
	super();
	setStyleName(CLASSNAME);
	calPanel = new ICalendarPanel(this);
	add(calPanel);
	this.entrySource = new EntrySource();
	calPanel.setCalendarEntrySource(this.entrySource);
	calPanel.addTableListener(new TableListener() {
	    public void onCellClicked(SourcesTableEvents sender, int row,
		    int cell) {
		buildDayView(date);
	    }
	});
    }

    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
	super.updateFromUIDL(uidl, client);
	// We want to draw our own hour list
	this.realResolution = currentResolution;
	this.currentResolution = RESOLUTION_DAY;
	if (uidl.hasAttribute("min")) {
	    String mins = uidl.getStringAttribute("min");
	    long min = (mins != null ? Long.parseLong(mins) : 0);
	    String maxs = uidl.getStringAttribute("max");
	    long max = (maxs != null ? Long.parseLong(maxs) : 0);
	    Date minDate = (min > 0 ? new Date(min) : null);
	    Date maxDate = (max > 0 ? new Date(max) : null);
	    calPanel.setLimits(minDate, maxDate);
	}
	this.entrySource.clear();
	for (Iterator cit = uidl.getChildIterator(); cit.hasNext();) {
	    UIDL child = (UIDL) cit.next();
	    if (child.getTag().equals("items")) {
		for (Iterator iit = child.getChildIterator(); iit.hasNext();) {
		    UIDL item = (UIDL) iit.next();
		    this.entrySource.addItem(item);
		}
		break;
	    }
	}
	calPanel.updateCalendar();
	buildDayView(this.date);
    }

    protected void buildDayView(Date date) {
	boolean firstRender = false;
	if (this.hourTable == null) {
	    hourTable = new FlexTable();
	    firstRender = true;
	    hourTable.addTableListener(this.ftListener);
	    SimplePanel p = new SimplePanel();
	    p.add(hourTable);
	    p.setStyleName(CLASSNAME + "-hours");
	    this.calPanel.getFlexCellFormatter().setColSpan(8, 0, 7);
	    this.calPanel.setWidget(8, 0, p);
	}
	Date curr = new Date(date.getTime());
	for (int i = 0; i < 24; i++) {
	    curr.setHours(i);
	    String style = (i % 2 == 0 ? "even" : "odd");
	    if (realResolution >= RESOLUTION_HOUR) {
		if (this.date != null && this.date.getHours() == i) {
		    style = "selected";
		}
	    }
	    hourTable.getRowFormatter().setStyleName(i,
		    CLASSNAME + "-row-" + style);
	    if (firstRender) {
		String hstr = (i < 10 ? "0" : "") + i + ":00";
		if (this.dts.isTwelveHourClock()) {
		    String ampm = (i < 12 ? "am" : "pm");
		    hstr = (i <= 12 ? i : i - 12) + ":00 " + ampm;
		}
		hourTable.setHTML(i, 0, "<span>" + hstr + "</span>");
		hourTable.getCellFormatter().setStyleName(i, 0,
			CLASSNAME + "-time");
	    }
	    List entries = this.entrySource.getEntries(curr,
		    DateTimeService.RESOLUTION_HOUR);
	    String text = "";
	    if (entries != null) {
		for (Iterator it = entries.iterator(); it.hasNext();) {
		    ICalendarEntry entry = (ICalendarEntry) it.next();
		    String title = entry.getTitle();
		    String desc = entry.getDescription();
		    text += (text == "" ? "" : ", ");
		    String e = "<span"
			    + (desc != null ? " title=\"" + desc + "\"" : "")
			    + ">";
		    e += (title != null ? title : "?");
		    e += "</span>";
		    text += e;
		}
	    }
	    hourTable.setHTML(i, 1, text);
	    hourTable.getCellFormatter().setStyleName(i, 1,
		    CLASSNAME + "-title");
	}

    }

    private class HourTableListener implements TableListener {

	public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
	    if (realResolution < RESOLUTION_HOUR || date == null) {
		return;
	    }
	    date.setHours(row);
	    client.updateVariable(id, "hour", row, immediate);
	}

    }

    private class EntrySource implements ICalendarPanel.CalendarEntrySource {

	private HashMap items = new HashMap();

	public void addItem(UIDL item) {
	    Integer id = new Integer(item.getIntAttribute("id"));
	    long start = Long.parseLong(item.getStringAttribute("start"));
	    Date startDate = new Date(start);
	    long end = Long.parseLong(item.getStringAttribute("end"));
	    Date endDate = (end > 0 && end != start ? new Date(end) : new Date(
		    start));
	    String title = item.getStringAttribute("title");
	    String desc = item.getStringAttribute("description");
	    boolean notime = item.getBooleanAttribute("notime");
	    if (items.containsKey(id)) {
		items.remove(id);
	    }
	    items.put(id, new ICalendarEntry(startDate, endDate, title, desc,
		    notime));
	}

	public List getEntries(Date date, int resolution) {
	    ArrayList res = new ArrayList();
	    for (Iterator it = this.items.values().iterator(); it.hasNext();) {
		ICalendarEntry item = (ICalendarEntry) it.next();
		if (DateTimeService.isInRange(date, item.getStart(), item
			.getEnd(), resolution)) {
		    res.add(item);
		}
	    }

	    return res;
	}

	public void clear() {
	    items.clear();
	}

    }

}
