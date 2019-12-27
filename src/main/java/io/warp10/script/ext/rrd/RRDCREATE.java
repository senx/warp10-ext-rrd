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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.ArcDef;
import org.rrd4j.core.DsDef;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDCREATE extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private static final String PARAM_STEP = "step";
  private static final String PARAM_DS = "ds";
  private static final String PARAM_RRA = "rra";
  
  public RRDCREATE(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {

    Object top = stack.pop();
    
    if (!(top instanceof Map)) {
      throw new WarpScriptException(getName() + " expects a parameter map.");
    }
    
    Map<Object,Object> params = (Map<Object, Object>) top;
    
    long step = Long.parseLong(params.getOrDefault(PARAM_STEP, "1").toString());
    
    RrdDef def = new RrdDef("", step);
    
    Object ds = params.get(PARAM_DS);
    
    if (!(ds instanceof List)) {
      throw new WarpScriptException(getName() + " encountered an invalid value for key '" + PARAM_DS + "', expected a list of datasource definitions.");
    }
    
    for (Object dsdef: (List) ds) {
      String[] tokens = dsdef.toString().split(":");
      
      if (6 != tokens.length) {
        throw new WarpScriptException(getName() + " expects datasource definitions to have the format DS:name:type:heartbeat:min:max.");
      }
      
      if (!"DS".equals(tokens[0])) {
        throw new WarpScriptException(getName() + " invalid datasource definition syntax '" + dsdef.toString() +"'.");
      }
      
      double min = Double.NaN;
      double max = Double.NaN;
      
      if (!"U".equals(tokens[4])) {
        min = Double.parseDouble(tokens[4]);
      }
      if (!"U".equals(tokens[5])) {
        max = Double.parseDouble(tokens[5]);
      }
      
      DsDef dsDef = new DsDef(tokens[1], DsType.valueOf(tokens[2]), Long.parseLong(tokens[3]), min, max);
      
      def.addDatasource(dsDef);
    }
    
    Object rra = params.get(PARAM_RRA);
    
    if (!(rra instanceof List)) {
      throw new WarpScriptException(getName() + " encountered an invalid value for key '" + PARAM_RRA + "', expected a list of archive definitions.");
    }
    
    for (Object rradef: (List) rra) {
      String[] tokens = rradef.toString().split(":");
      
      if (!"RRA".equals(tokens[0]) || 5 != tokens.length) {
        throw new WarpScriptException(getName() + " expects archive definitions to have the format RRA:function:xff:steps:rows");
      }
      
      double xff = Double.parseDouble(tokens[2]);
      int steps = Integer.parseInt(tokens[3]);
      int rows = Integer.parseInt(tokens[4]);
      
      ArcDef arcDef = new ArcDef(ConsolFun.valueOf(tokens[1]), xff, steps, rows);
      
      def.addArchive(arcDef);
    }

    try {
      RrdDb db = RrdDb.getBuilder().setBackendFactory(new RrdMemoryBackendFactory()).setRrdDef(def).build();
      stack.push(db);
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while creating RRD.", ioe);
    }
    
    return stack;
  }
}
