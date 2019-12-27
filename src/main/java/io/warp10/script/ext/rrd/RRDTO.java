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

import org.rrd4j.core.RRD4JUtils;
import org.rrd4j.core.RrdDb;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;

public class RRDTO extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  public RRDTO(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    Object top = stack.pop();
    
    if (!(top instanceof RrdDb)) {
      throw new WarpScriptException(getName() + " operates on an RRD DB.");
    }

    try {
      stack.push(RRD4JUtils.getBytes((RrdDb) top));
    } catch (IOException ioe) {
      throw new WarpScriptException(getName() + " encountered an error while serializing RRD DB.");
    }
    
    return stack;
  }

}
