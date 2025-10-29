<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.2" name="objects" tilewidth="80" tileheight="80" tilecount="6" columns="0">
 <tile id="0" x="160" y="80" width="80" height="80">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="10" x="0.395355" y="36.5703">
    <properties>
     <property name="body_type" value="static"/>
     <property name="is_sensor" type="bool" value="true"/>
     <property name="sensor_behavior" value="hide:0.5"/>
    </properties>
    <polygon points="0,0 19.3724,-30.64 36.3726,-36.1749 76.5011,-14.2328 78.4779,4.15122 50.0124,33.6051 30.4423,33.6051 -0.197677,5.93032"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="1" x="240" y="80" width="80" height="80">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="5" x="0.790709" y="32.0237">
    <properties>
     <property name="body_type" value="static"/>
     <property name="is_sensor" type="bool" value="true"/>
     <property name="sensor_behavior" value="hide:0.5"/>
    </properties>
    <polygon points="0.197677,-3.36051 26.4888,-26.2911 55.5473,-22.9306 77.6872,-0.988386 77.6872,15.0235 49.024,39.9308 31.0353,40.1285 -0.395355,6.72103"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="2" x="240" y="160" width="80" height="16">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="17" y="0" width="46" height="14">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="3" x="16" y="304" width="32" height="32">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="2" x="2" y="16" width="27" height="12">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
   <object id="3" x="3.75587" y="15.2212">
    <properties>
     <property name="body_type" value="static"/>
     <property name="is_sensor" type="bool" value="true"/>
     <property name="sensor_behavior" value="hide:0.5"/>
    </properties>
    <polygon points="0,0 0.197677,-2.96516 7.11638,-9.29083 17.3956,-9.09316 23.919,-2.37213 24.2155,0"/>
   </object>
  </objectgroup>
 </tile>
 <tile id="4" x="48" y="304" width="32" height="16">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="2" x="3" y="0" width="26" height="15">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="6" x="80" y="304" width="16" height="16">
  <image width="352" height="336" source="tf_jungle_tileset.png"/>
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="1" width="16" height="14">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
</tileset>
