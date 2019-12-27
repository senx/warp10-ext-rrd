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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDLOAD extends NamedWarpScriptFunction implements WarpScriptStackFunction {

  public RRDLOAD(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    Object top = stack.pop();
    
    List<String> files = new ArrayList<String>();
    
    if (top instanceof String) {
      files.add(top.toString());
    } else if (top instanceof Collection) {
      for (Object o: (Collection) top) {
        if (!(o instanceof String)) {
          throw new WarpScriptException(getName() + " expects file names as strings.");
        }
        files.add(o.toString());
      }
    }
    
    List<RrdDb> dbs = new ArrayList<RrdDb>();
    
    RrdBackendFactory factory = new RrdMemoryBackendFactory();
    
    for (String file: files) {
      if (file.contains("/../") || !file.startsWith("/")) {
        throw new WarpScriptException(getName() + " Illegal relative path.");
      }
      
      File f = new File(RRDWarpScriptExtension.RRD_DIR + file);
      
      if (!f.exists()) {
        throw new WarpScriptException(getName() + " RRD file '" + file + "' not found.");
      }
      
      try {
        RrdDb db = RrdDb.getBuilder().setUsePool(false).setPath("").setExternalPath(RRDWarpScriptExtension.RRDTOOL_PREFIX + f.getPath()).setBackendFactory(factory).build();
        dbs.add(db);
      } catch (IOException ioe) {
        throw new WarpScriptException(getName() + " error loading '" + file + "'.", ioe);
      }
    }
    
    if (top instanceof Collection) {
      stack.push(dbs);
    } else {
      stack.push(dbs.get(0));
    }
    
    return stack;
  }
}
