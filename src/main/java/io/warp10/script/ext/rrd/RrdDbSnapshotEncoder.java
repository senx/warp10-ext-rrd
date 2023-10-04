//
//   Copyright 2023  SenX S.A.S.
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

import org.rrd4j.core.RRD4JUtils;
import org.rrd4j.core.RrdDb;

import io.warp10.script.WarpScriptException;
import io.warp10.script.functions.SNAPSHOT;
import io.warp10.script.functions.SNAPSHOT.SnapshotEncoder;

public class RrdDbSnapshotEncoder implements SnapshotEncoder {
  @Override
  public boolean addElement(SNAPSHOT snapshot, StringBuilder sb, Object o, boolean readable) throws WarpScriptException {
    if (!(o instanceof RrdDb)) {
      return false;
    }

    try {
      SNAPSHOT.addElement(snapshot, sb, RRD4JUtils.getBytes((RrdDb) o));
      sb.append(" ");
      sb.append(RRDWarpScriptExtension.TORRD);
      sb.append(" ");
      return true;
    } catch (IOException ioe) {
      throw new WarpScriptException("Error snapshotting RRD DB.", ioe);
    }
  }
}
