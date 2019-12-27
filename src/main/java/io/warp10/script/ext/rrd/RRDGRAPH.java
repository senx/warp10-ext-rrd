//
//   Copyright 2019  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.ext.rrd;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Util;
import org.rrd4j.data.Variable;
import org.rrd4j.graph.ElementsNames;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphConstants;
import org.rrd4j.graph.RrdGraphConstants.FontTag;
import org.rrd4j.graph.RrdGraphDef;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDGRAPH extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private static final Map<String,Integer> DayOfWeek;
  
  static {
    DayOfWeek = new HashMap<String,Integer>();
    DayOfWeek.put("MONDAY", RrdGraphConstants.MONDAY);
    DayOfWeek.put("TUESDAY", RrdGraphConstants.TUESDAY);
    DayOfWeek.put("WEDNESDAY", RrdGraphConstants.WEDNESDAY);
    DayOfWeek.put("THURSDAY", RrdGraphConstants.THURSDAY);
    DayOfWeek.put("FRIDAY", RrdGraphConstants.FRIDAY);
    DayOfWeek.put("SATURDAY", RrdGraphConstants.SATURDAY);
    DayOfWeek.put("SUNDAY", RrdGraphConstants.SUNDAY);
  }
  
  private static final String DEFAULT_SIGNATURE = "WarpScript RRD Extension";
  
  public RRDGRAPH(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    Object top = stack.pop();
    
    if (!(top instanceof Map)) {
      throw new WarpScriptException(getName() + " expects a parameter map.");
    }
    
    RrdGraphDef gdef = params2def((Map) top);
    
    try {
      RrdGraph graph = new RrdGraph(gdef);
      byte[] img = graph.getRrdGraphInfo().getBytes();
      String imgurl = "data:image/png;base64," + new String(Base64.encodeBase64(img), StandardCharsets.US_ASCII);
      stack.push(imgurl);
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while generating graph.", ioe);
    }
    
    return stack;
  }
  
  private RrdGraphDef params2def(Map params) throws WarpScriptException {
    RrdGraphDef def = new RrdGraphDef();
    // Instruct RrdGraph to generate the image in memory
    def.setFilename(RrdGraphConstants.IN_MEMORY_IMAGE);
    // Do not use pool
    def.setPoolUsed(false);

    def.setAltAutoscale(Boolean.TRUE.equals(params.getOrDefault("alt-autoscale", false)));
    def.setAltAutoscaleMax(Boolean.TRUE.equals(params.getOrDefault("alt-autoscale-max", false)));
    def.setAltAutoscaleMin(Boolean.TRUE.equals(params.getOrDefault("alt-autoscale-min", false)));
    def.setAltYGrid(Boolean.TRUE.equals(params.getOrDefault("alt-y-grid", false)));
    def.setAltYMrtg(Boolean.TRUE.equals(params.getOrDefault("alt-y-mrtg", false)));
    def.setAntiAliasing(Boolean.TRUE.equals(params.getOrDefault("anti-aliasing", false)));
    def.setDrawXGrid(Boolean.TRUE.equals(params.getOrDefault("draw-x-grid", true)));
    def.setDrawYGrid(Boolean.TRUE.equals(params.getOrDefault("draw-y-grid", true)));
    def.setFontSet(Boolean.TRUE.equals(params.getOrDefault("rrdtool", false)));
    def.setForceRulesLegend(Boolean.TRUE.equals(params.getOrDefault("force-rules-legend", false)));
    def.setInterlaced(Boolean.TRUE.equals(params.getOrDefault("interlaced", false)));
    def.setLazy(Boolean.TRUE.equals(params.getOrDefault("lazy", false)));
    def.setLogarithmic(Boolean.TRUE.equals(params.getOrDefault("logarithmic", false)));
    def.setNoLegend(Boolean.TRUE.equals(params.getOrDefault("no-legend", false)));
    def.setNoMinorGrid(Boolean.TRUE.equals(params.getOrDefault("no-minor-grid", false)));
    def.setOnlyGraph(Boolean.TRUE.equals(params.getOrDefault("only-graph", false)));
    def.setRigid(Boolean.TRUE.equals(params.getOrDefault("rigid", false)));
    def.setTextAntiAliasing(Boolean.TRUE.equals(params.getOrDefault("text-anti-aliasing", false)));
    def.setShowSignature(Boolean.TRUE.equals(params.getOrDefault("show-signature", true)));
    
    def.setBase(Double.parseDouble(String.valueOf(params.getOrDefault("base", RrdGraphConstants.DEFAULT_BASE))));
    if (params.containsKey("first-day-of-week")) {
      def.setFirstDayOfWeek(DayOfWeek.get(params.get("first-day-of-week")));
    }
    // grid-stroke ignored
    def.setHeight(Integer.parseInt(String.valueOf(params.getOrDefault("height", RrdGraphConstants.DEFAULT_HEIGHT))));
    def.setImageFormat("PNG");
    def.setImageQuality(Float.parseFloat(String.valueOf(params.getOrDefault("image-quality", RrdGraphConstants.DEFAULT_IMAGE_QUALITY))));
    def.setMaxValue(Double.parseDouble(String.valueOf(params.getOrDefault("upper-limit", params.getOrDefault("maxvalue", Double.NaN)))));
    def.setMinValue(Double.parseDouble(String.valueOf(params.getOrDefault("lower-limit", params.getOrDefault("minvalue", Double.NaN)))));
    def.setSignature(String.valueOf(params.getOrDefault("signature", DEFAULT_SIGNATURE)));
    String endspec = String.valueOf(params.getOrDefault("end", RrdGraphConstants.DEFAULT_END));
    String startspec = String.valueOf(params.getOrDefault("start", RrdGraphConstants.DEFAULT_START));
    long[] startend = Util.getTimestamps(startspec, endspec);
    def.setStartTime(startend[0]);
    def.setEndTime(startend[1]);
    long step = Long.parseLong(String.valueOf(params.getOrDefault("step", 0)));
    def.setStep(step);
    // tick-stroke ignored
    if (params.containsKey("title")) {
      def.setTitle(String.valueOf(params.get("title")));
    }
    if (params.containsKey("unit")) {
      def.setUnit(String.valueOf(params.get("unit")));
    }
    def.setUnitsExponent(Integer.parseInt(String.valueOf(params.getOrDefault("units-exponent", Integer.MAX_VALUE))));
    def.setUnitsLength(Integer.parseInt(String.valueOf(params.getOrDefault("units-length", RrdGraphConstants.DEFAULT_UNITS_LENGTH))));
    if (params.containsKey("vertical-label")) {
      def.setVerticalLabel(String.valueOf(params.get("vertical-label")));
    }
    def.setWidth(Integer.parseInt(String.valueOf(params.getOrDefault("width", RrdGraphConstants.DEFAULT_WIDTH))));
    
    if (params.containsKey("color")) {
      Object color = params.get("color");
      List<Object> colors = new ArrayList<Object>();
      if (!(color instanceof List)) {
        colors.add(String.valueOf(color));
      } else {
        colors.addAll((List) color);
      }
      for (Object col: colors) {
        String[] tokens = String.valueOf(col).split("#");
        if (2 != tokens.length) {
          throw new WarpScriptException("Invalid syntax for color specification '" + col + "'.");
        }
        ElementsNames colorTag = ElementsNames.valueOf(tokens[0].toLowerCase());
        int rgba = Integer.parseInt(tokens[1], 16);
        if (8 == tokens[1].length()) {
          // Shift alpha to upper 8 bits
          int rgb = (rgba & 0xFFFFFF00) >>> 8;
          rgb |= (rgba << 24);
          rgba = rgb;
        }
        Paint co = new Color(rgba, 6 < tokens[1].length());
        def.setColor(colorTag, co);
      }
    }
    
    if (params.containsKey("font")) {
      Object font = params.get("font");
      List<Object> fonts = new ArrayList<Object>();
      if (!(font instanceof List)) {
        fonts.add(String.valueOf(font));
      } else {
        fonts.addAll((List) font);
      }
      for (Object fo: fonts) {
        String[] tokens = String.valueOf(fo).split(":");
        if (3 != tokens.length) {
          throw new WarpScriptException("Invalid syntax for font specification '" + fo + "'.");
        }
        if ("DEFAULT".equalsIgnoreCase(tokens[2])) {
          tokens[2] = null;
        }
        Font f = new Font(tokens[2], Font.PLAIN, Integer.parseInt(tokens[1]));
        def.setFont(FontTag.valueOf(tokens[0].toUpperCase()), f);
      }
    }
    
    if (params.containsKey("time-axis")) {
      Object spec = params.get("time-axis");
      
      if (!(spec instanceof List)) {
        throw new WarpScriptException("Invalid time axis specification, expected a list.");
      }
      
      List<Object> tspec = (List<Object>) spec;
      
      if (8 != tspec.size()) {
        throw new WarpScriptException("Invalid time axis specification, expected a list with 8 elements.");
      }
      
      //def.setTimeAxis(minorUnit, minorUnitCount, majorUnit, majorUnitCount, labelUnit, labelUnitCount, labelSpan, simpleDateFormat);
    }
    
    if (params.containsKey("comment")) {
      Object comment = params.get("comment");
      
      if (comment instanceof List) {
        for (Object c: (List) comment) {
          def.comment(String.valueOf(c));
        }
      } else {
        def.comment(String.valueOf(comment));
      }
    }
    
    Map<String,RrdDb> rrd = new HashMap<String,RrdDb>();
    
    if (params.containsKey("rrd")) {
      Object elt = params.get("rrd");
      if (!(elt instanceof Map)) {
        throw new WarpScriptException("Missing RRD definitions under key 'rrd'.");
      }
      for (Entry<Object,Object> entry: ((Map<Object,Object>) elt).entrySet()) {
        if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof RrdDb)) {
          throw new WarpScriptException("Expected 'rrd' to be associated with a map of database name to RRD DB.");
        }
        rrd.put(String.valueOf(entry.getKey()), (RrdDb) entry.getValue());
      }
    } else {
      throw new WarpScriptException("Missing RRD definitions under key 'rrd'.");
    }
    
    if (params.containsKey("data")) {
      Object data = params.get("data");
      
      if (!(data instanceof List)) {
        throw new WarpScriptException("The 'data' key should be associated with a list.");
      }
      
      Matcher DEF_PATTERN = Pattern.compile("DEF:(?<vname>[^=]+)=(?<rrdfile>[^:]+):(?<dsname>[^:]+):(?<CF>AVERAGE|FIRST|LAST|MAX|MIN|TOTAL)(:step=(?<step>[0-9]+))?(:start=(?<start>[^:]+))?(:end=(?<end>[^:]+))?").matcher("");
      for (Object spec: (List) data) {
        String s = String.valueOf(spec);
        
        if (s.startsWith("DEF:")) {
          
          DEF_PATTERN.reset(s);
          
          if (!DEF_PATTERN.matches()) {
            throw new WarpScriptException("Invalid syntax for DEF spec '" + s + "'.");
          }

          String name = DEF_PATTERN.group("vname");
          String rrdfile = DEF_PATTERN.group("rrdfile");
          String dsname = DEF_PATTERN.group("dsname");
          
          if (!rrd.containsKey(rrdfile)) {
            throw new WarpScriptException("Unknown RRD DB '" + rrdfile + "'.");
          }
          
          RrdDb db = rrd.get(rrdfile);
          
          try {
            if (!(db.containsDs(dsname))) {
              throw new WarpScriptException("RRD DB '" + rrdfile + "' does not contain a data source named '" + dsname + "'.");
            }
          } catch (IOException ioe) {
            throw new WarpScriptException("Error while checking for '" + dsname + "' in '" + rrdfile + "'.");
          }
          
          String cfname = DEF_PATTERN.group("CF");

          if ("".equals(cfname)) {
            throw new WarpScriptException("DEF specification is missing a consolidation function.");
          }
          
          ConsolFun cf = null;
          
          if ("AVERAGE".equals(cfname)) {
            cf = ConsolFun.AVERAGE;
          } else if ("FIRST".equals(cfname)) {
            cf = ConsolFun.FIRST;
          } else if ("LAST".equals(cfname)) {
            cf = ConsolFun.LAST;
          } else if ("MAX".equals(cfname)) {
            cf = ConsolFun.MAX;
          } else if ("MIN".equals(cfname)) {
            cf = ConsolFun.MIN;
          } else if ("TOTAL".equals(cfname)) {
            cf = ConsolFun.TOTAL;
          }
          
          long defstep = step;
          String defstart = startspec;
          String defend = endspec;
                    
          if (null != DEF_PATTERN.group("step")) {
            defstep = Long.parseLong(DEF_PATTERN.group("step"));
          }
          if (null != DEF_PATTERN.group("start")) {
            defstart = DEF_PATTERN.group("start");
          }
          if (null != DEF_PATTERN.group("end")) {
            defend = DEF_PATTERN.group("end");
          }

          startend = Util.getTimestamps(defstart, defend);
                    
          FetchRequest req = null;
          
          if (0 != defstep) {
            req = db.createFetchRequest(cf, startend[0], startend[1], defstep);
          } else {
            req = db.createFetchRequest(cf, startend[0], startend[1]);
          }
          
          try {
            def.datasource(name, dsname, req.fetchData());
          } catch (IOException ioe) {
            throw new WarpScriptException("Error while fetching data for '" + name + "'.");
          }
        } else if (s.startsWith("CDEF:")) {
          String name = s.substring(5).replaceAll("=.*", "");
          String rpn = s.substring(5 + name.length() + 1);
          def.datasource(name, rpn);
        } else if (s.startsWith("VDEF:")) {
          String name = s.substring(5).replaceAll("=.*", "");
          String rpn = s.substring(5 + name.length() + 1);
          String defname = rpn.replaceAll(",.*", "");
          String func = rpn.substring(defname.length() + 1);
          
          Variable var = null;
          
          if ("AVERAGE".equals(func)) {
            var = new Variable.AVERAGE();
          } else if ("FIRST".equals(func)) {
            var = new Variable.FIRST();
          } else if ("LAST".equals(func)) {
            var = new Variable.LAST();
          } else if ("LSLCORREL".equals(func)) {
            var = new Variable.LSLCORREL();
          } else if ("LSLINT".equals(func)) {
            var = new Variable.LSLINT();
          } else if ("LSLSLOPE".equals(func)) {
            var = new Variable.LSLSLOPE();
          } else if ("MAXIMUM".equals(func)) {
            var = new Variable.MAX();
          } else if ("MINIMUM".equals(func)) {
            var = new Variable.MIN();
          } else if ("STDDEV".equals(func)) {
            var = new Variable.STDDEV();
          } else if ("TOTAL".equals(func)) {
            var = new Variable.TOTAL();
          } else if (func.endsWith(",PERCENT")) {
            double pct = Double.parseDouble(func.replaceAll(",.*", ""));
            var = new Variable.PERCENTILE(pct);
          } else if (func.endsWith(",PERCENTNAN")) {
            double pct = Double.parseDouble(func.replaceAll(",.*", ""));
            var = new Variable.PERCENTILENAN(pct);
          } else {
            throw new WarpScriptException("Invalid VDEF specification.");
          }
          
          def.datasource(name,defname, var);
        } else {
          throw new WarpScriptException("Invalid data definition, expected DEF:, CDEF: or VDEF: prefix.");
        }
      }
    } else {
      throw new WarpScriptException("Missing 'data' key.");
    }

    if (params.containsKey("graph")) {
      Object graph = params.get("graph");
      
      if (!(graph instanceof List)) {
        throw new WarpScriptException("Key 'graph' must be associated with a list of graph specifications.");
      }
      
      Matcher AREA_PATTERN = Pattern.compile("AREA:(?<value>[^#:]+)(#(?<color>[0-9a-fA-F]{6,8}))?(:(?<legend>.*?))?(:(?<stack>STACK))?").matcher("");
      Matcher LINE_PATTERN = Pattern.compile("LINE(?<width>[^:]+)?:(?<value>[^#:]+)(#(?<color>[0-9a-fA-F]{6,8}))?(:(?<legend>.*?))?(:(?<stack>STACK))?").matcher("");
      Matcher VRULE_PATTERN = Pattern.compile("VRULE(?<width>[^:]+)?:(?<timestamp>[0-9]+)(#(?<color>[0-9a-fA-F]{6,8}))?(:(?<legend>.*))?").matcher("");
      Matcher HRULE_PATTERN = Pattern.compile("HRULE(?<width>[^:]+)?:(?<value>[^#:]+)(#(?<color>[0-9a-fA-F]{6,8}))?(:(?<legend>.*))?").matcher("");
      Matcher SPAN_PATTERN = Pattern.compile("[HV]SPAN#(?<color>[0-9a-fA-F]{6,8}):(?<start>[^:]+):(?<end>[^:]+)(:(?<legend>.*))?").matcher("");

      for (Object g: (List) graph) {
        String spec = String.valueOf(g);
        
        if (spec.startsWith("AREA:")) {
          // AREA:value[#color][:[legend][:STACK]]
          AREA_PATTERN.reset(spec);
          
          if (!AREA_PATTERN.matches()) {
            throw new WarpScriptException("Invalid AREA specification '" + spec + "'.");
          }
          String value = AREA_PATTERN.group("value");
          String color = AREA_PATTERN.group("color");
          String legend = AREA_PATTERN.group("legend");
          String stack = AREA_PATTERN.group("stack");
          
          Paint col = null;
          
          if (null != color) {
            int rgba = Integer.parseInt(color, 16);
            if (8 == color.length()) {
              // Shift alpha to upper 8 bits
              int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
            }
            col = new Color(rgba, 6 < color.length());            
          } else {
            col = Color.BLACK;
          }
          
          try {
            double d = Double.parseDouble(value);
            def.area(d, col, "STACK".equals(stack));
          } catch (NumberFormatException nfe) {
            def.area(value, col, legend, "STACK".equals(stack));
          }
        } else if (spec.startsWith("GPRINT:")) {
          String vname = spec.substring(7).replaceAll(":.*", "");
          String format = spec.substring(7 + vname.length() + 1);
          def.gprint(vname, format);
        } else if (spec.startsWith("HRULE")) {
          // HRULE[width]:value[#color][:legend]
          HRULE_PATTERN.reset(spec);
          
          if (!HRULE_PATTERN.matches()) {
            throw new WarpScriptException("Invalid HRULE specification '" + spec + "'.");
          }
          String value = HRULE_PATTERN.group("value");
          String color = HRULE_PATTERN.group("color");
          String width = HRULE_PATTERN.group("width");
          String legend = HRULE_PATTERN.group("legend");
          
          float fwidth = 1.0F;
          
          if (null != width) {
            fwidth = Float.parseFloat(width);
          }
          
          if (null != legend) {
            legend = null;
          }
          
          Paint col = null;
          
          if (null != color) {
            int rgba = Integer.parseInt(color, 16);
            if (8 == color.length()) {
              // Shift alpha to upper 8 bits
              int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
            }
            col = new Color(rgba, 6 < color.length());            
          } else {
            col = Color.BLACK;
          }

          def.hrule(Double.parseDouble(value), col, legend, fwidth);
        } else if (spec.startsWith("HSPAN#")) {
          // HSPAN#color:start:end[:legend]
          SPAN_PATTERN.reset(spec);
          
          if (!SPAN_PATTERN.matches()) {
            throw new WarpScriptException("Invalid SPAN specification '" + spec + "'.");
          }
          String sstart = SPAN_PATTERN.group("start");
          String send = SPAN_PATTERN.group("end");
          String color = SPAN_PATTERN.group("color");
          String legend = SPAN_PATTERN.group("legend");
                    
          Paint col = null;
          
          int rgba = Integer.parseInt(color, 16);
          if (8 == color.length()) {
            // Shift alpha to upper 8 bits
            int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
          }
          col = new Color(rgba, 6 < color.length());            

          def.hspan(Float.parseFloat(sstart), Float.parseFloat(send), col, legend);
        } else if (spec.startsWith("LINE")) {
          // LINE[width]:value[#color][:[legend][:STACK]]
          LINE_PATTERN.reset(spec);
          
          if (!LINE_PATTERN.matches()) {
            throw new WarpScriptException("Invalid LINE spec '" + spec + "'.");
          }
          String width = LINE_PATTERN.group("width");
          String value = LINE_PATTERN.group("value");
          String color = LINE_PATTERN.group("color");
          String legend = LINE_PATTERN.group("legend");
          String stack = LINE_PATTERN.group("stack");
          
          Paint col = null;
          
          if (null != color) {
            int rgba = Integer.parseInt(color, 16);
            if (8 == color.length()) {
              // Shift alpha to upper 8 bits
              int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
            }
            col = new Color(rgba, 6 < color.length());            
          } else {
            col = Color.BLACK;
          }

          float fwidth = 1.0F;
          
          if (null != width) {
            fwidth = Float.parseFloat(width);
          }
          
          try {
            double d = Double.parseDouble(value);
            def.line(d, col, fwidth, "STACK".equals(stack));
          } catch (NumberFormatException nfe) {
            def.line(value, col, legend, fwidth, "STACK".equals(stack));
          }
        } else if (spec.startsWith("PRINT:")) {
          String vname = spec.substring(6).replaceAll(":.*", "");
          String format = spec.substring(6 + vname.length() + 1);
          def.print(vname, format);
        } else if (spec.startsWith("VRULE")) {
          // VRULE[width]:time#color[:[legend]]
          VRULE_PATTERN.reset(spec);
          
          if (VRULE_PATTERN.matches()) {
            throw new WarpScriptException("Invalid VRULE specification '" + spec + "'.");
          }
          long timestamp = Long.parseLong(VRULE_PATTERN.group("timestamp"));
          String color = VRULE_PATTERN.group("color");
          String width = VRULE_PATTERN.group("width");
          String legend = VRULE_PATTERN.group("legend");
          
          float fwidth = 1.0F;
          
          if (null != width) {
            fwidth = Float.parseFloat(width);
          }
          
          Paint col = null;
          
          if (null != color) {
            int rgba = Integer.parseInt(color, 16);
            if (8 == color.length()) {
              // Shift alpha to upper 8 bits
              int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
            }
            col = new Color(rgba, 6 < color.length());            
          } else {
            col = Color.BLACK;
          }

          def.vrule(timestamp, col, legend, fwidth);
        } else if (spec.startsWith("VSPAN#")) {
          // VSPAN#color:start:end[:legend]
          // HSPAN#color:start:end[:legend]
          
          SPAN_PATTERN.reset(spec);
          
          if (!SPAN_PATTERN.matches()) {
            throw new WarpScriptException("Invalid SPAN specification '" + spec + "'.");
          }
          String sstart = SPAN_PATTERN.group("start");
          String send = SPAN_PATTERN.group("end");
          String color = SPAN_PATTERN.group("color");
          String legend = SPAN_PATTERN.group("legend");
          
          Paint col = null;
          
          int rgba = Integer.parseInt(color, 16);
          if (8 == color.length()) {
            // Shift alpha to upper 8 bits
            int rgb = (rgba & 0xFFFFFF00) >>> 8;
              rgb |= (rgba << 24);
              rgba = rgb;
          }
          col = new Color(rgba, 6 < color.length());            

          def.vspan(Long.parseLong(sstart), Long.parseLong(send), col, legend);
        } else {
          throw new WarpScriptException("Invalid graph spec '" + spec + "'.");
        }
      }
    } else {
      throw new WarpScriptException("Missing 'graph' key.");
    }

    return def;
  }
}
