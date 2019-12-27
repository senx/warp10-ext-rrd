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

package org.rrd4j.core;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class RRD4JUtils {
  public static FetchData archiveFetch(Archive archive, FetchRequest request) throws IOException {
    return archive.fetchData(request);
  }
  public static void setBuffer(ByteBufferBackend backend, byte[] data) {
    ByteBuffer buffer = ByteBuffer.wrap(data);  
    backend.setByteBuffer(buffer);
  }
  
  public static byte[] getBytes(RrdDb db) throws IOException {
    try {
      return db.getBytes();
    } catch (BufferUnderflowException bue) {
      bue.printStackTrace();
      if (db.getRrdBackend() instanceof ByteBufferBackend) {
        int len = (int) db.getRrdBackend().getLength();
        if (1 == len % 2) {
          byte[] bytes = new byte[len];
          CharBuffer cb = ((ByteBufferBackend) db.getRrdBackend()).getCharBuffer(0L, len / 2);
          for (int i = 0; i < bytes.length / 2; i++) {
            char c = cb.charAt(i);
            bytes[2 * i] = (byte) (((int) c >> 8) & 0xFF);
            bytes[2 * i + 1] = (byte) (c & 0xFF);
          }
          // Retrieve the last byte
          cb = ((ByteBufferBackend) db.getRrdBackend()).getCharBuffer(len - 2, 2);
          bytes[len - 1] = (byte) (cb.charAt(0) & 0xFF);
          return bytes;          
        } else {
          CharBuffer cb = ((ByteBufferBackend) db.getRrdBackend()).getCharBuffer(0L, len / 2);
          byte[] bytes = new byte[len];
          for (int i = 0; i < bytes.length / 2; i++) {
            char c = cb.charAt(i);
            bytes[2 * i] = (byte) (((int) c >> 8) & 0xFF);
            bytes[2 * i + 1] = (byte) (c & 0xFF);
          }
          return bytes;          
        }
      } else {
        throw new IOException(bue);
      }
    }
  }
  
  public static RrdMemoryBackend getMemoryBackend(String path) {
    return new RrdMemoryBackend(path);
  }
}
