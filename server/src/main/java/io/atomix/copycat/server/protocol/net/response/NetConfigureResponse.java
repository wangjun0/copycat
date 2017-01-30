/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package io.atomix.copycat.server.protocol.net.response;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.atomix.copycat.protocol.net.response.NetResponse;
import io.atomix.copycat.protocol.response.AbstractResponse;
import io.atomix.copycat.protocol.response.ProtocolResponse;
import io.atomix.copycat.server.protocol.response.ConfigureResponse;

/**
 * TCP configure response.
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public class NetConfigureResponse extends ConfigureResponse implements RaftNetResponse<NetConfigureResponse> {
  private final long id;

  public NetConfigureResponse(long id, ProtocolResponse.Status status, ProtocolResponse.Error error) {
    super(status, error);
    this.id = id;
  }

  @Override
  public long id() {
    return id;
  }

  @Override
  public Type type() {
    return Type.CONFIGURE;
  }

  /**
   * TCP configure response builder.
   */
  public static class Builder extends ConfigureResponse.Builder {
    private final long id;

    public Builder(long id) {
      this.id = id;
    }

    @Override
    public ConfigureResponse copy(ConfigureResponse response) {
      return new NetConfigureResponse(id, response.status(), response.error());
    }

    @Override
    public ConfigureResponse build() {
      return new NetConfigureResponse(id, status, error);
    }
  }

  /**
   * Configure response serializer.
   */
  public static class Serializer extends RaftNetResponse.Serializer<NetConfigureResponse> {
    @Override
    public void write(Kryo kryo, Output output, NetConfigureResponse response) {
      output.writeLong(response.id);
      output.writeByte(response.status.id());
      if (response.status == Status.ERROR) {
        output.writeByte(response.error.type().id());
        output.writeString(response.error.message());
      }
    }

    @Override
    public NetConfigureResponse read(Kryo kryo, Input input, Class<NetConfigureResponse> type) {
      final long id = input.readLong();
      final Status status = Status.forId(input.readByte());
      if (status == Status.OK) {
        return new NetConfigureResponse(id, status, null);
      } else {
        NetResponse.Error error = new AbstractResponse.Error(ProtocolResponse.Error.Type.forId(input.readByte()), input.readString());
        return new NetConfigureResponse(id, status, error);
      }
    }
  }
}