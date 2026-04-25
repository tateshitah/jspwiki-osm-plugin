/*

Copyright (c) 2011-2026 Hiroaki Tateshita

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
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
//import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.PluginContent;
import org.braincopy.Information;
import org.braincopy.Location;

/**
 * Open Street Map Plugin for JSPWiki
 * 
 * @author Hiroaki Tateshita
 * 
 */
public class OSM implements Plugin {

	final static String OPENLAYER_JS_URL = "https://openlayers.org/en/v4.6.5/build/ol.js";

	@Override
	public String execute(Context context, Map<String, String> params) throws PluginException {
		// String result = "";
		StringBuilder out = new StringBuilder(4096);

		double latitude = 35.684444;
		if (params.get("lat") != null)
			latitude = Double.parseDouble((String) params.get("lat"));

		double longitude = 139.77437;
		if (params.get("lng") != null)
			longitude = Double.parseDouble((String) params.get("lng"));
		if (params.get("lon") != null)
			longitude = Double.parseDouble((String) params.get("lon"));

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

		WikiEngine engine = (WikiEngine) context.getEngine();
		try {
			getLocations(geoInfoSet, (WikiEngine) engine, pages, (WikiContext) context);
		} catch (NumberFormatException | ProviderException e) {
			// result += "something happens" + e.getLocalizedMessage();
			out.append("something happens" + e.getLocalizedMessage());
			e.printStackTrace();
		}
		String mapId = "map_" + System.nanoTime();

		out.append("<div id=\"")
				.append(mapId)
				.append("\" style=\"width: ")
				.append(width)
				.append("px; height: ")
				.append(height)
				.append("px;\"></div>\n");

		out.append("<script src=\"")
				.append(OPENLAYER_JS_URL)
				.append("\"></script>\n");

		out.append("<script>\n");

		out.append("function convertCoordinate(longitude, latitude) {\n")
				.append("  return ol.proj.transform([longitude, latitude], \"EPSG:4326\", \"EPSG:900913\");\n")
				.append("}\n");

		out.append("function pointStyleFunction(feature, resolution) {\n")
				.append("  return new ol.style.Style({\n")
				.append("    image: new ol.style.Circle({\n")
				.append("      radius: 20,\n")
				.append("      fill: new ol.style.Fill({color: 'rgba(255, 255, 0, 0.1)'}),\n")
				.append("      stroke: new ol.style.Stroke({color: 'blue', width: 3})\n")
				.append("    }),\n")
				.append("    text: new ol.style.Text({\n")
				.append("      textAlign: 'center',\n")
				.append("      textBaseline: 'middle',\n")
				.append("      font: 'Arial',\n")
				.append("      text: feature.get('name'),\n")
				.append("      fill: new ol.style.Fill({color: 'yellow'}),\n")
				.append("      stroke: new ol.style.Stroke({color: 'blue', width: 5}),\n")
				.append("      offsetX: 0,\n")
				.append("      offsetY: 0,\n")
				.append("      rotation: 0\n")
				.append("    })\n")
				.append("  });\n")
				.append("}\n");

		out.append("var marker_array = [];\n");

		if (pages != null) {
			int cnt = 1;
			for (Information info : geoInfoSet) {
				Location loc = info.getLocation();
				out.append("var marker_").append(cnt).append(" = new ol.Feature({\n")
						.append("  geometry: new ol.geom.Point(convertCoordinate(")
						.append(loc.getLon()).append(", ").append(loc.getLat()).append(")),\n")
						.append("  name: '").append(jsString(info.getName())).append("'\n")
						.append("});\n")
						.append("marker_array.push(marker_").append(cnt).append(");\n");
				cnt++;
			}
		} else {
			out.append("var marker = new ol.Feature({\n")
					.append("  geometry: new ol.geom.Point(convertCoordinate(")
					.append(longitude).append(", ").append(latitude).append(")),\n")
					.append("  name: '").append(jsString(context.getPage().getName())).append("'\n")
					.append("});\n")
					.append("marker_array.push(marker);\n");
		}

		out.append("var markerSource = new ol.source.Vector({ features: marker_array });\n")
				.append("var labelLayer = new ol.layer.Vector({ source: markerSource, style: pointStyleFunction });\n")
				.append("var osmLayer = new ol.layer.Tile({ source: new ol.source.OSM() });\n")
				.append("var map_")
				.append(mapId)
				.append(" = new ol.Map({\n")
				.append("  layers: [ osmLayer, labelLayer ],\n")
				.append("  target: document.getElementById('")
				.append(mapId)
				.append("'),\n")
				.append("  view: new ol.View({ center: convertCoordinate(")
				.append(longitude).append(", ").append(latitude)
				.append("), zoom: ").append(zoom).append(" })\n")
				.append("});\n");

		out.append("var select_")
				.append(mapId)
				.append(" = new ol.interaction.Select();\n")
				.append("map_").append(mapId)
				.append(".addInteraction(select_")
				.append(mapId)
				.append(");\n")
				.append("select_")
				.append(mapId)
				.append(".on('select', function(e) {\n")
				.append("  if (e.target.getFeatures().getLength() > 0) {\n")
				.append("    var pagename = e.target.getFeatures().item(0).get('name');\n")
				.append("    window.location.href = 'Wiki.jsp?page=' + pagename;\n")
				.append("  }\n")
				.append("});\n");

		out.append("</script>\n");

		/*
		 * result += "  <div id=\"map\" style=\"width: " + width + "px; height: " +
		 * height + "px;\"></div>\n";
		 * result += "<script src=\"" + OPENLAYER_JS_URL + "\"></script>\n";
		 * result += "<script>\n";
		 * result += "function convertCoordinate(longitude, latitude) {\n";
		 * result +=
		 * "return ol.proj.transform([ longitude,latitude ], \"EPSG:4326\",\"EPSG:900913\");}\n"
		 * ;
		 * 
		 * result += "function pointStyleFunction(feature, resolution) {\n";
		 * result += "\treturn new ol.style.Style({\n";
		 * result += "\timage: new ol.style.Circle({\n";
		 * result += "\t\tradius: 20,\n";
		 * result +=
		 * "\t\tfill: new ol.style.Fill({color: 'rgba(255, 255, 0, 0.1)'}),\n";
		 * result += "\t\tstroke: new ol.style.Stroke({color: 'blue', width: 3})}),\n";
		 * result += "\t\ttext: new ol.style.Text({\n";
		 * result += "\t\t\ttextAlign: 'center',\n";
		 * result += "\t\t\ttextBaseline: 'middle',\n";
		 * result += "\t\t\tfont: 'Arial',\n";
		 * result += "\t\t\ttext: feature.get('name'),\n";
		 * result += "\t\t\tfill: new ol.style.Fill({color: 'yellow'}),\n";
		 * result += "\t\t\tstroke: new ol.style.Stroke({color: 'blue', width: 5}),\n";
		 * result += "\t\t\toffsetX: 0,\n";
		 * result += "\t\t\toffsetY: 0,\n";
		 * result += "\t\t\trotation: 0})});}\n";
		 * 
		 * result += "\tvar marker_array = [];\n";
		 * 
		 * if (pages != null) {
		 * Iterator<Information> ite = geoInfoSet.iterator();
		 * Information tempInfo = null;
		 * Location tempLocation = null;
		 * int cnt = 1;
		 * while (ite.hasNext()) {
		 * tempInfo = (Information) ite.next();
		 * tempLocation = tempInfo.getLocation();
		 * result += "\tvar marker_" + cnt + "= new ol.Feature({\n";
		 * result += "\t\tgeometry : new ol.geom.Point(convertCoordinate(" +
		 * tempLocation.getLon() + ","
		 * + tempLocation.getLat() + ")),\n";
		 * result += "\t\tname: '" + tempInfo.getName() + "'});\n";
		 * result += "\tmarker_array.push(marker_" + cnt + ");\n";
		 * cnt++;
		 * }
		 * } else {
		 * result += "\tvar marker = new ol.Feature({\n";
		 * result += "\t\tgeometry : new ol.geom.Point(convertCoordinate(" + longitude +
		 * "," + latitude + ")),\n";
		 * result += "\t\tname: '" + context.getPage().getName() + "'});\n";
		 * result += "\tmarker_array.push(marker);\n";
		 * }
		 * result += "\tvar markerSource = new ol.source.Vector({\n";
		 * result += "\t\tfeatures : marker_array});\n";
		 * 
		 * result += "\tvar rabelLayer = new ol.layer.Vector({\n";
		 * result += "\t\tsource : markerSource,\n";
		 * result += "\t\tstyle : pointStyleFunction});\n";
		 * 
		 * result += "\tvar osmLayer = new ol.layer.Tile({\n";
		 * result += "\t\tsource : new ol.source.OSM()});\n";
		 * 
		 * result += "\tvar map = new ol.Map({\n";
		 * result += "\t\tlayers : [ osmLayer, rabelLayer ],\n";
		 * result += "\t\ttarget : document.getElementById('map'),\n";
		 * result += "\t\tview : new ol.View({\n";
		 * result += "\t\t\tcenter : convertCoordinate(" + longitude + "," + latitude +
		 * "),\n";
		 * result += "\t\t\tzoom : " + zoom + "})});\n";
		 * 
		 * result += "\tvar select = new ol.interaction.Select();\n";
		 * result += "\tmap.addInteraction(select);\n";
		 * result += "\tselect.on('select',function(e){\n";
		 * result += "\t\tif(e.target.getFeatures().getLength()>0){\n";
		 * result +=
		 * "\t\t\tvar pagename = e.target.getFeatures().item(0).get('name');\n";
		 * result +=
		 * "\t\t\twindow.location.href='Wiki.jsp?page='+pagename;\n\t\t}\n\t});\n";
		 * 
		 * result += "</script>\n";
		 */
		return out.toString();
		// return result;
	}

	/**
	 * geoInfoSet will be updated by adding location information searched
	 * recursively from the pages which include OSM plugin.
	 * 
	 * @param geoInfoSet
	 * @param engine
	 * @param pages
	 * @param context
	 * @throws PluginException
	 * @throws ProviderException
	 * @throws NumberFormatException
	 */
	protected void getLocations(TreeSet<Information> geoInfoSet, WikiEngine engine, String[] pages, WikiContext context)
			throws PluginException, NumberFormatException, ProviderException {
		if (pages != null) {
			WikiPage wikipage = null;
			String pureText = null;
			PageManager pageManager = engine.getManager(PageManager.class);

			for (int i = 0; i < pages.length; i++) {
				if (pageManager.pageExists(pages[i]) && !geoInfoSet.contains(new Information(pages[i]))) {
					wikipage = (WikiPage) pageManager.getPage(pages[i]);
					pureText = pageManager.getPureText(wikipage);
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

								// Recursive point!
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

	/**
	 * Escapes a Java string so it is safe to embed inside a JavaScript
	 * single-quoted string literal.
	 */
	private static String jsString(String s) {
		if (s == null) {
			return "";
		}
		return s
				.replace("\\", "\\\\") // backslash
				.replace("'", "\\'") // single quote
				.replace("\"", "\\\"") // double quote (defensive)
				.replace("\r", "\\r")
				.replace("\n", "\\n")
				.replace("</", "<\\/"); // prevent </script> breaking out
	}
}
