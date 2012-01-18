package com.hudson.hibernatesynchronizer.editors.synchronizer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.hudson.hibernatesynchronizer.Plugin;

public class ColorManager {

	public static final String COLOR_XML_COMMENT = "Color.XMLComment";
	public static final String COLOR_STRING = "Color.XMLString";
	public static final String COLOR_TAG = "Color.XMLTag";
	public static final String COLOR_PROC_INSTR = "Color.XMLProcInstr";
	public static final String COLOR_DEFAULT = "Color.XMLDefault";
	public static final String COLOR_LINK = "Color.XMLLink";

	private RGB XML_COMMENT = new RGB(128, 0, 0);
	private RGB PROC_INSTR = new RGB(128, 128, 128);
	private RGB STRING = new RGB(0, 128, 0);
	private RGB DEFAULT = new RGB(0, 0, 0);
	private RGB TAG = new RGB(0, 0, 128);
	private RGB LINK = JFaceResources.getColorRegistry().get(JFacePreferences.HYPERLINK_COLOR).getRGB();

	protected Map fColorTable = new HashMap(10);
	private Map defaultColorMap = new HashMap();

	public ColorManager () {
		defaultColorMap.put(COLOR_XML_COMMENT, XML_COMMENT);
		defaultColorMap.put(COLOR_STRING, STRING);
		defaultColorMap.put(COLOR_TAG, TAG);
		defaultColorMap.put(COLOR_PROC_INSTR, PROC_INSTR);
		defaultColorMap.put(COLOR_DEFAULT, DEFAULT);
		defaultColorMap.put(COLOR_LINK, LINK);
	}

	public void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext()) ((Color) e.next()).dispose();
		fColorTable.clear();
	}

	public Color getColor(RGB rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}

	public Color getColor (String colorName) {
		RGB rgb = PreferenceConverter.getColor(Plugin.getDefault().getPreferenceStore(), colorName);
		if (null == rgb) rgb = (RGB) defaultColorMap.get(colorName);
		return getColor(rgb);
	}

	public Color getDefaultColor (String colorName) {
		return getColor((RGB) defaultColorMap.get(colorName));
	}
}
