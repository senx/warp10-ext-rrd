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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.core.Archive;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RRD4JUtils;
import org.rrd4j.core.RrdDb;

import io.warp10.continuum.gts.GTSHelper;
import io.warp10.continuum.gts.GeoTimeSerie;
import io.warp10.continuum.store.Constants;
import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDTOGTS extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  
  private static final String LABEL_RRDTYPE = ".rrdtype";
  private static final String LABEL_RRDFUN = ".rrdfun";
  private static final String LABEL_RRDSTEPS = ".rrdsteps";
  private static final String LABEL_RRDROWS = ".rrdrows";
  
  private static final String ATTR_RRDXFF = ".rrdxff";
  private static final String ATTR_RRDTSTEPS = ".rrdtsteps";
  
  public RRDTOGTS(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object top = stack.pop();
    
    if (!(top instanceof RrdDb)) {
      throw new WarpScriptException(getName() + " operates on an RRD DB.");
    }
        
    RrdDb db = (RrdDb) top;
    
    try {
      List<GeoTimeSerie> series = new ArrayList<GeoTimeSerie>();
      
      for (int i = 0; i < db.getArcCount(); i++) {
        Archive archive = db.getArchive(i);
        
        FetchRequest frequest = db.createFetchRequest(archive.getConsolFun(), archive.getStartTime(), archive.getEndTime());
        FetchData data = RRD4JUtils.archiveFetch(archive, frequest);
        
        String rows = Integer.toString(archive.getRows());
        String xff = Double.toString(archive.getXff());
        String steps = Integer.toString(archive.getSteps());
        String tsteps = Long.toString(archive.getArcStep());
        String fun = archive.getConsolFun().name();

        long[] timestamps = data.getTimestamps();
        double[][] values = data.getValues();
        String[] names = data.getDsNames();
        
        for (int j = 0; j < names.length; j++) {
          GeoTimeSerie gts = new GeoTimeSerie(timestamps.length);
          Map<String,String> labels = new HashMap<String,String>();
          labels.put(LABEL_RRDFUN, fun);
          labels.put(LABEL_RRDTYPE, db.getDatasource(names[j]).getType().toString());
          labels.put(LABEL_RRDROWS, rows);
          labels.put(LABEL_RRDSTEPS, steps);
          
          Map<String,String> attributes = new HashMap<String,String>();
          attributes.put(ATTR_RRDXFF, xff);
          attributes.put(ATTR_RRDTSTEPS, tsteps);

          // DataSource name
          gts.setName(names[j]);
          gts.setLabels(labels);
          gts.getMetadata().setAttributes(attributes);
          series.add(gts);
          for (int k = 0; k < timestamps.length; k++) {
            GTSHelper.setValue(gts, timestamps[k] * Constants.TIME_UNITS_PER_S, values[j][k]);
          }
        }                  
      }
      
      stack.push(series);
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while serializing RRD DB.");
    }
    
    return stack;
  }
}
