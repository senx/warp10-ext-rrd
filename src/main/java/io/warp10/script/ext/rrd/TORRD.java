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

import java.io.IOException;

import org.rrd4j.core.ByteBufferBackend;
import org.rrd4j.core.RRD4JUtils;
import org.rrd4j.core.RrdBackend;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdMemoryBackend;
import org.rrd4j.core.RrdMemoryBackendFactory;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class TORRD extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  public TORRD(String name) {
    super(name);
  }

  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {

    Object top = stack.pop();

    if (!(top instanceof byte[])) {
      throw new WarpScriptException(getName() + " operates on a byte array.");
    }

    byte[] data = (byte[]) top;

    try {
      final ByteBufferBackend backend = RRD4JUtils.getMemoryBackend("");
      RRD4JUtils.setBuffer(backend, data);

      //
      // We need to trick RRD4J by using a custom backend factory
      // which will return a single custom backend
      //
      RrdBackendFactory factory = new RrdMemoryBackendFactory() {

        @Override
        protected RrdBackend open(String path, boolean readOnly) {
          return backend;
        }

        @Override
        protected boolean exists(String path) {
          return true;
        }

        @Override
        public String getName() {
          return "MEMORY";
        }
      };

      RrdDb db = RrdDb.getBuilder().setUsePool(false).setPath("").setBackendFactory(factory).build();
      RRD4JUtils.setBuffer((RrdMemoryBackend) db.getRrdBackend(), data);
      stack.push(db);
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while creating RRD DB.");
    }

    return stack;
  }

}
