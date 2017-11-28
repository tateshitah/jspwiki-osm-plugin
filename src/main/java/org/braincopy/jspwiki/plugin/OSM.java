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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.PluginContent;
import org.braincopy.Information;
import org.braincopy.Location;

/**
 * Open Street Map Plugin for JSPWiki
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

		int zoom = 12;
		if (params.get("zoom") != null)
			zoom = Integer.parseInt((String) params.get("zoom"));

		int width = 400;
		if (params.get("width") != null)
			width = Integer.parseInt((String) params.get("width"));

		int height = 300;
		if (params.get("height") != null)
			height = Integer.parseInt((String) params.get("height"));

		// 'pagename1/pagename2/pagename3'
		String[] pages = null;
		if (params.get("pages") != null) {
			pages = params.get("pages").split("/");
		}

		// ArrayList<Location> locations = new ArrayList<Location>();
		TreeSet<Information> geoInfoSet = new TreeSet<Information>();

		WikiEngine engine = context.getEngine();
		getLocations(geoInfoSet, engine, pages, context);
		/*
		 * if (pages != null) { WikiPage wikipage = null; String pureText = null;
		 * Location tempLocation = null;
		 * 
		 * for (int i = 0; i < pages.length; i++) { if (engine.pageExists(pages[i])) {
		 * wikipage = engine.getPage(pages[i]); pureText = engine.getPureText(wikipage);
		 * tempLocation = getLocation(locations, pureText, context); if (tempLocation !=
		 * null) { locations.add(tempLocation); } // result += wikipage.getName() +
		 * " exists!<br>"; } else { // result += pages[i] + "does not exist!<br>"; } } }
		 */
		result += "  <div id=\"map\" style=\"width: " + width + "px; height: " + height + "px;\"></div>\n";
		result += "<script src=\"https://openlayers.org/en/v4.2.0/build/ol.js\"></script>\n";
		result += "<script>\n";
		result += "function convertCoordinate(longitude, latitude) {\n";
		result += "return ol.proj.transform([ longitude,latitude ], \"EPSG:4326\",\"EPSG:900913\");}\n";

		result += "function pointStyleFunction(feature, resolution) {\n";
		result += "\treturn new ol.style.Style({\n";
		result += "\timage: new ol.style.Circle({\n";
		result += "\t\tradius: 20,\n";
		result += "\t\tfill: new ol.style.Fill({color: 'rgba(255, 255, 0, 0.1)'}),\n";
		result += "\t\tstroke: new ol.style.Stroke({color: 'blue', width: 3})}),\n";
		result += "\t\ttext: new ol.style.Text({\n";
		result += "\t\t\ttextAlign: 'center',\n";
		result += "\t\t\ttextBaseline: 'middle',\n";
		result += "\t\t\tfont: 'Arial',\n";
		result += "\t\t\ttext: feature.get('name'),\n";
		result += "\t\t\tfill: new ol.style.Fill({color: 'yellow'}),\n";
		result += "\t\t\tstroke: new ol.style.Stroke({color: 'blue', width: 5}),\n";
		result += "\t\t\toffsetX: 0,\n";
		result += "\t\t\toffsetY: 0,\n";
		result += "\t\t\trotation: 0})});}\n";

		result += "\tvar marker_array = [];\n";

		if (pages != null) {
			Iterator<Information> ite = geoInfoSet.iterator();
			Information tempInfo = null;
			Location tempLocation = null;
			int cnt = 1;
			while (ite.hasNext()) {
				tempInfo = (Information) ite.next();
				tempLocation = tempInfo.getLocation();
				result += "\tvar marker_" + cnt + "= new ol.Feature({\n";
				result += "\t\tgeometry : new ol.geom.Point(convertCoordinate(" + tempLocation.getLon() + ","
						+ tempLocation.getLat() + ")),\n";
				result += "\t\tname: '" + tempInfo.getName() + "'});\n";
				result += "\tmarker_array.push(marker_" + cnt + ");\n";
				cnt++;
			}
		} else {
			result += "\tvar marker = new ol.Feature({\n";
			result += "\t\tgeometry : new ol.geom.Point(convertCoordinate(" + longtitude + "," + latitude + ")),\n";
			result += "\t\tname: '" + context.getPage().getName() + "'});\n";
			result += "\tmarker_array.push(marker);\n";
		}
		result += "\tvar markerSource = new ol.source.Vector({\n";
		result += "\t\tfeatures : marker_array});\n";

		result += "\tvar rabelLayer = new ol.layer.Vector({\n";
		result += "\t\tsource : markerSource,\n";
		result += "\t\tstyle : pointStyleFunction});\n";

		result += "\tvar osmLayer = new ol.layer.Tile({\n";
		result += "\t\tsource : new ol.source.OSM()});\n";

		result += "\tvar map = new ol.Map({\n";
		result += "\t\tlayers : [ osmLayer, rabelLayer ],\n";
		result += "\t\ttarget : document.getElementById('map'),\n";
		result += "\t\tview : new ol.View({\n";
		result += "\t\t\tcenter : convertCoordinate(" + longtitude + "," + latitude + "),\n";
		result += "\t\t\tzoom : " + zoom + "})});\n";

		result += "\tvar select = new ol.interaction.Select();\n";
		result += "\tmap.addInteraction(select);\n";
		result += "\tselect.on('select',function(e){\n";
		result += "\t\tif(e.target.getFeatures().getLength()>0){\n";
		result += "\t\t\tvar pagename = e.target.getFeatures().item(0).get('name');\n";
		result += "\t\t\twindow.location.href='Wiki.jsp?page='+pagename;\n\t\t}\n\t});\n";

		result += "</script>\n";

		return result;
	}

	/**
	 * 
	 * @param geoInfoSet
	 * @param engine
	 * @param pages
	 * @param context
	 * @throws PluginException
	 */
	protected void getLocations(TreeSet<Information> geoInfoSet, WikiEngine engine, String[] pages, WikiContext context)
			throws PluginException {
		if (pages != null) {
			WikiPage wikipage = null;
			String pureText = null;

			for (int i = 0; i < pages.length; i++) {
				if (engine.pageExists(pages[i]) && !geoInfoSet.contains(new Information(pages[i]))) {
					wikipage = engine.getPage(pages[i]);
					pureText = engine.getPureText(wikipage);
					String pluginText = "";
					PluginContent pluginContent = null;
					double lat = Double.MIN_VALUE, lon = Double.MIN_VALUE;
					String[] sub_pages = null;
					if (pureText.contains("[{OSM")) {
						pluginText = pureText.substring(pureText.indexOf("[{OSM"));
						pluginText = pluginText.substring(0, pluginText.indexOf("}]") + 3);
						try {
							pluginContent = PluginContent.parsePluginLine(context, pluginText, 0);
							if (pluginContent.getParameter("pages") != null) {
								sub_pages = pluginContent.getParameter("pages").split("/");
								getLocations(geoInfoSet, engine, sub_pages, context);
							} else {
								if (pluginContent.getParameter("lat") != null) {
									lat = Double.parseDouble(pluginContent.getParameter("lat"));
								}
								if (pluginContent.getParameter("lng") != null) {
									lon = Double.parseDouble(pluginContent.getParameter("lng"));
								}
								if (pluginContent.getParameter("lon") != null) {
									lon = Double.parseDouble(pluginContent.getParameter("lon"));
								}
								geoInfoSet.add(new Information(pages[i], new Location(lat, lon)));
							}
						} catch (PluginException e) {
							throw e;
						}
					}
				}
			}
		}
	}
	/*
	 * protected Location getLocation(ArrayList<Location> locations, String
	 * pureText, WikiContext context) throws PluginException { Location result =
	 * null;
	 * 
	 * String pluginText = ""; PluginContent pluginContent = null; double lat =
	 * Double.MIN_VALUE, lon = Double.MIN_VALUE; String[] pages = null; if
	 * (pureText.contains("[{OSM")) { pluginText =
	 * pureText.substring(pureText.indexOf("[{OSM")); pluginText =
	 * pluginText.substring(0, pluginText.indexOf("}]") + 3); try { pluginContent =
	 * PluginContent.parsePluginLine(context, pluginText, 0); if
	 * (pluginContent.getParameter("pages") != null) { pages =
	 * pluginContent.getParameter("pages").split("/"); }
	 * 
	 * if (pluginContent.getParameter("lat") != null) { lat =
	 * Double.parseDouble(pluginContent.getParameter("lat")); } if
	 * (pluginContent.getParameter("lng") != null) { lon =
	 * Double.parseDouble(pluginContent.getParameter("lng")); } if
	 * (pluginContent.getParameter("lon") != null) { lon =
	 * Double.parseDouble(pluginContent.getParameter("lon")); } result = new
	 * Location(lat, lon); } catch (PluginException e) { throw e; } }
	 * 
	 * return result; }
	 */
}
