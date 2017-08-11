# Introduction

OSM Plugin is a plugin for embedding OpenSteetMaps into a JSPWiki page.

# Installation

Download the OSM Plugin/osm-plugin-0.1.3.jar(info) and put into your JSPWiki's WEB-INF/lib directory.
restart tomcat
# Usage

Place the following line anywhere in a JSPWiki page.

[{OSM lat='coordinate latitude' lng='coordinate longitude'}]
# Parameters

* lat
Coordinate Latitude value from -90 to 90, must be enclosed in single quotation marks
* lng
Coordinate Longitude value from -180 to 180, must be enclosed in single quotation marks
* zoom
Zoom value from 0 to 19, where 0 is widest and 19 is closest
* width
plugin width. default 400px
* height
plugin height. default 400px

Heres an example of how the above image was created:

[{OSM lat='35.707214' 
      lng='139.619895'
      zoom='17' 
      width='600'
      height='300'
}]

* usage example: Location
you can check source code here(info). I will upload it on GitHub later.

# Known Issues
