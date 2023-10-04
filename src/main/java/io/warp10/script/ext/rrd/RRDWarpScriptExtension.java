//
//   Copyright 2019-2023  SenX S.A.S.
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

import java.util.HashMap;
import java.util.Map;

import io.warp10.WarpConfig;
import io.warp10.script.functions.SNAPSHOT;
import io.warp10.warp.sdk.WarpScriptExtension;

public class RRDWarpScriptExtension extends WarpScriptExtension {

  private static final Map<String,Object> functions;

  public static final String CONF_RRD_DIR = "rrd.dir";

  public static final String RRDTOOL_PREFIX = "rrdtool:/";

  public static final String RRD_DIR;

  public static final String TORRD = "->RRD";
  public static final String RRDTO = "RRD->";

  static {
    RRD_DIR = WarpConfig.getProperty(CONF_RRD_DIR);

    if (null == RRD_DIR) {
      throw new RuntimeException("Configuration key '" + CONF_RRD_DIR + "' not set.");
    }

    functions = new HashMap<String,Object>();

    functions.put("RRDGRAPH", new RRDGRAPH("RRDGRAPH"));
    functions.put("RRDUPDATE", new RRDUPDATE("RRDUPDATE"));
    functions.put("RRDCREATE", new RRDCREATE("RRDCREATE"));
    functions.put("RRDLOAD", new RRDLOAD("RRDLOAD"));
    functions.put("RRDTOGTS", new RRDTOGTS("RRDTOGTS"));
    functions.put(RRDTO, new RRDTO("RRD->"));
    functions.put(TORRD, new TORRD("->RRD"));

    SNAPSHOT.addEncoder(new RrdDbSnapshotEncoder());
  }

  @Override
  public Map<String, Object> getFunctions() {
    return functions;
  }
}
