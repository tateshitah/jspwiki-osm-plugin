/*

Copyright (c) 2011-2017 Hiroaki Tateshita

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
copies of the Software, and to permit persons to whom the Software is furnished
to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION 
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 */

package org.braincopy.jspwiki.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.PluginContent;
import org.braincopy.Location;

/**
 * 
 * @author Hiroaki Tateshita
 * 
 */
public class OSM implements WikiPlugin {

	public String execute(WikiContext context, Map<String, String> params) throws PluginException {
		String result = "";

		double latitude = 35.684444;
		if (params.get("lat") != null)
			latitude = Double.parseDouble((String) params.get("lat"));

		double longtitude = 139.77437;
		if (params.get("lng") != null)
			longtitude = Double.parseDouble((String) params.get("lng"));
		if (params.get("lon") != null)
			longtitude = Double.parseDouble((String) params.get("lon"));

		int zoom = 6;
		if (params.get("zoom") != null)
			zoom = Integer.parseInt((String) params.get("zoom"));

		int width = 800;
		if (params.get("width") != null)
			width = Integer.parseInt((String) params.get("width"));

		int height = 600;
		if (params.get("height") != null)
			height = Integer.parseInt((String) params.get("height"));

		// 'pagename1/pagename2/pagename3'
		String pagesString = null;
		String[] pages = null;
		if (params.get("pages") != null) {
			pagesString = params.get("pages");
			pages = pagesString.split("/");
		}

		ArrayList<Location> locations = new ArrayList<Location>();
		if (pages != null) {
			WikiEngine engine = context.getEngine();
			WikiPage wikipage = null;
			String pureText = null;
			Location tempLocation = null;

			for (int i = 0; i < pages.length; i++) {
				if (engine.pageExists(pages[i])) {
					wikipage = engine.getPage(pages[i]);
					pureText = engine.getPureText(wikipage);
					tempLocation = getLocation(pureText, context);
					if (tempLocation != null) {
						locations.add(tempLocation);
					}
					// result += wikipage.getName() + " exists!<br>";
				} else {
					// result += pages[i] + "does not exist!<br>";
				}
			}
		}

		result += "  <div id=\"map\" style=\"width: " + width + "px; height: " + height + "px;\"></div>\n";
		result += "<script src=\"https://openlayers.org/en/v4.2.0/build/ol.js\"></script>\n";
		result += "<script>\n";
		result += "function convertCoordinate(longitude, latitude) {\n";
		result += "return ol.proj.transform([ longitude,latitude ], \"EPSG:4326\",\"EPSG:900913\");}\n";

		result += "var markerStyleDefault = new ol.style.Style({\n";
		result += "image : new ol.style.Icon(/** @type {olx.style.IconOptions} */{\n";
		result += "anchor : [ 0.5, 1 ],\n";
		result += "anchorXUnits : 'fraction',\n";
		result += "anchorYUnits : 'fraction',\n";
		result += "opacity : 0.75,\n";
		result += "src : 'https://braincopy.org/WebContent/assets/map-marker-red-th.png'})});\n";

		result += "var marker_array = [];\n";

		Iterator<Location> ite = locations.iterator();
		Location tempLocation = null;
		int cnt = 1;
		while (ite.hasNext()) {
			tempLocation = (Location) ite.next();
			result += "\tvar marker_" + cnt + "= new ol.Feature({\n";
			result += "geometry : new ol.geom.Point(convertCoordinate(" + tempLocation.getLon() + ","
					+ tempLocation.getLat() + "))});\n";
			result += "\tmarker_array.push(marker_" + cnt + ");\n";
			cnt++;
		}
		result += "var marker = new ol.Feature({\n";
		result += "geometry : new ol.geom.Point(convertCoordinate(" + longtitude + "," + latitude + "))});\n";
		result += "marker_array.push(marker);";

		result += "var markerSource = new ol.source.Vector({\n";
		result += "features : marker_array});\n";

		result += "var markerLayer = new ol.layer.Vector({\n";
		result += "source : markerSource,\n";
		result += "style : markerStyleDefault});\n";

		result += "var osmLayer = new ol.layer.Tile({\n";
		result += "source : new ol.source.OSM()});\n";

		result += "var map = new ol.Map({\n";
		result += "layers : [ osmLayer, markerLayer ],\n";
		result += "target : document.getElementById('map'),\n";
		result += "view : new ol.View({\n";
		result += "center : convertCoordinate(" + longtitude + "," + latitude + "),\n";
		result += "zoom : " + zoom + "})});\n";

		result += "</script>\n";

		return result;
	}

	protected Location getLocation(String pureText, WikiContext context) throws PluginException {
		Location result = null;

		String pluginText = "";
		PluginContent pluginContent = null;
		double lat = Double.MIN_VALUE, lon = Double.MIN_VALUE;
		if (pureText.contains("[{OSM")) {
			pluginText = pureText.substring(pureText.indexOf("[{OSM"));
			pluginText = pluginText.substring(0, pluginText.indexOf("}]") + 3);
			try {
				pluginContent = PluginContent.parsePluginLine(context, pluginText, 0);
				if (pluginContent.getParameter("lat") != null) {
					lat = Double.parseDouble(pluginContent.getParameter("lat"));
				}
				if (pluginContent.getParameter("lng") != null) {
					lon = Double.parseDouble(pluginContent.getParameter("lng"));
				}
				if (pluginContent.getParameter("lon") != null) {
					lon = Double.parseDouble(pluginContent.getParameter("lon"));
				}
				result = new Location(lat, lon);
			} catch (PluginException e) {
				throw e;
			}
		}

		return result;
	}

}
