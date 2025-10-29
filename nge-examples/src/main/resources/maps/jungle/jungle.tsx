<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.2" name="jungle" tilewidth="16" tileheight="16" tilecount="462" columns="22">
 <image source="tf_jungle_tileset.png" width="352" height="336"/>
 <tile id="32">
  <objectgroup draworder="index" id="2">
   <object id="2" x="0" y="0" width="16.0481" height="7.02103">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
   <object id="3" x="-0.0417918" y="6.97923" width="4.97323" height="9.02703">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="33">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="0" width="16" height="7">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="34">
  <objectgroup draworder="index" id="2">
   <object id="2" x="0" y="-0.0417918" width="16.0063" height="6.93744">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
   <object id="3" x="10.9495" y="6.93744" width="5.0986" height="9.02703">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="54">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="0" width="5" height="16">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="56">
  <objectgroup draworder="index" id="2">
   <object id="1" x="11" y="0" width="5" height="16">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="76">
  <objectgroup draworder="index" id="2">
   <object id="2" x="0" y="0" width="5.05681" height="15.9645">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
   <object id="3" x="4.97323" y="10.9495" width="10.9912" height="5.0986">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="77">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="11" width="16" height="5">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <tile id="78">
  <objectgroup draworder="index" id="3">
   <object id="3" x="11.033" y="-0.0417918" width="4.93144" height="16.0063">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
   <object id="4" x="0.0417918" y="10.9077" width="11.033" height="5.05681">
    <properties>
     <property name="body_type" value="static"/>
    </properties>
   </object>
  </objectgroup>
 </tile>
 <wangsets>
  <wangset name="Grass" type="corner" tile="-1">
   <wangcolor name="bush" color="#ff0000" tile="-1" probability="1"/>
   <wangcolor name="grass" color="#00ff00" tile="-1" probability="1"/>
   <wangcolor name="dirt" color="#0000ff" tile="-1" probability="1"/>
   <wangtile tileid="23" wangid="0,1,0,1,0,1,0,1"/>
   <wangtile tileid="32" wangid="0,1,0,2,0,1,0,1"/>
   <wangtile tileid="33" wangid="0,1,0,2,0,2,0,1"/>
   <wangtile tileid="34" wangid="0,1,0,1,0,2,0,1"/>
   <wangtile tileid="35" wangid="0,2,0,1,0,2,0,2"/>
   <wangtile tileid="36" wangid="0,2,0,2,0,1,0,2"/>
   <wangtile tileid="38" wangid="0,1,0,3,0,1,0,1"/>
   <wangtile tileid="39" wangid="0,1,0,3,0,3,0,1"/>
   <wangtile tileid="40" wangid="0,1,0,1,0,3,0,1"/>
   <wangtile tileid="41" wangid="0,3,0,1,0,3,0,3"/>
   <wangtile tileid="42" wangid="0,3,0,3,0,1,0,3"/>
   <wangtile tileid="45" wangid="0,3,0,3,0,3,0,3"/>
   <wangtile tileid="54" wangid="0,2,0,2,0,1,0,1"/>
   <wangtile tileid="55" wangid="0,2,0,2,0,2,0,2"/>
   <wangtile tileid="56" wangid="0,1,0,1,0,2,0,2"/>
   <wangtile tileid="57" wangid="0,1,0,2,0,2,0,2"/>
   <wangtile tileid="58" wangid="0,2,0,2,0,2,0,1"/>
   <wangtile tileid="60" wangid="0,3,0,3,0,1,0,1"/>
   <wangtile tileid="61" wangid="0,3,0,3,0,3,0,3"/>
   <wangtile tileid="62" wangid="0,1,0,1,0,3,0,3"/>
   <wangtile tileid="63" wangid="0,1,0,3,0,3,0,3"/>
   <wangtile tileid="64" wangid="0,3,0,3,0,3,0,1"/>
   <wangtile tileid="76" wangid="0,2,0,1,0,1,0,1"/>
   <wangtile tileid="77" wangid="0,2,0,1,0,1,0,2"/>
   <wangtile tileid="78" wangid="0,1,0,1,0,1,0,2"/>
   <wangtile tileid="82" wangid="0,3,0,1,0,1,0,1"/>
   <wangtile tileid="83" wangid="0,3,0,1,0,1,0,3"/>
   <wangtile tileid="84" wangid="0,1,0,1,0,1,0,3"/>
  </wangset>
 </wangsets>
</tileset>
