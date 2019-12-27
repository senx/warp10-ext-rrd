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
import java.util.ArrayList;
import java.util.List;

import org.rrd4j.core.RrdDb;
import org.rrd4j.core.Sample;

import io.warp10.continuum.gts.GTSEncoder;
import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.store.Constants;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDUPDATE extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  public RRDUPDATE(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object top = stack.pop();
    
    if (!(top instanceof GeoTimeSerie) && !(top instanceof GTSEncoder) && !(top instanceof List)) {
      throw new WarpScriptException(getName() + " expects a Geo Time Series, a GTS Encoder or a list thereof.");
    }
    
    List<Object> series = new ArrayList<Object>();
    
    if (top instanceof GeoTimeSerie || top instanceof GTSEncoder) {
      series.add(top);
    } else {
      series.addAll((List) top);
    }
    
    top = stack.pop();
    
    if (!(top instanceof RrdDb)) {
      throw new WarpScriptException(getName() + " operated on an RRD DB.");
    }
    
    RrdDb rrd = (RrdDb) top;

    try {
      for (Object elt: series) {
        if (elt instanceof GeoTimeSerie || elt instanceof GTSEncoder) {
          
          GeoTimeSerie gts = null;
          
          if (elt instanceof GeoTimeSerie) {
            gts = (GeoTimeSerie) elt;
          } else {
            gts = ((GTSEncoder) elt).getDecoder(true).decode();            
          }
          if (!rrd.containsDs(gts.getName())) {
            continue;
          }
          int dsidx = rrd.getDsIndex(gts.getName());
          GTSHelper.sort(gts);
          for (int i = 0; i < GTSHelper.nvalues(gts); i++) {
            long ts = GTSHelper.tickAtIndex(gts, i) / Constants.TIME_UNITS_PER_S;
            Object value = GTSHelper.valueAtIndex(gts, i);
            
            double dvalue;
            
            if (value instanceof Number) {
              dvalue = ((Number) value).doubleValue();
            } else if (value instanceof Boolean) {
              dvalue = Boolean.TRUE.equals(value) ? 1.0D : 0.0D;
            } else {
              throw new WarpScriptException(getName() + " can only handle numeric or boolean Geo Time Series.");
            }
            
            Sample sample = rrd.createSample();
            sample.setTime(ts);
            sample.setValue(dsidx, dvalue);
            sample.update();
          }
        } else {
          throw new WarpScriptException(getName() + " invalid list element, expected Geo Time Series or GTS Encoder instances.");
        }
      }      
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while updating RRD DB.");
    }
    
    stack.push(rrd);
    return stack;
  }
  
}
