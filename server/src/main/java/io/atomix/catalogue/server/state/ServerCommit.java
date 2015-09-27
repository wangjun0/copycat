/*
 * Copyright 2015 the original author or authors.
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
 * limitations under the License.
 */
package io.atomix.catalogue.server.state;

import io.atomix.catalogue.client.Command;
import io.atomix.catalogue.client.Operation;
import io.atomix.catalogue.client.session.Session;
import io.atomix.catalogue.server.Commit;
import io.atomix.catalogue.server.storage.entry.OperationEntry;
import io.atomix.catalyst.util.Assert;

import java.time.Instant;

/**
 * Server commit.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
class ServerCommit implements Commit<Operation> {
  private final ServerCommitPool pool;
  private final ServerCommitCleaner cleaner;
  private final ServerSessionManager sessions;
  private OperationEntry entry;
  private Session session;
  private Instant instant;
  private volatile boolean open;

  public ServerCommit(ServerCommitPool pool, ServerCommitCleaner cleaner, ServerSessionManager sessions) {
    this.pool = pool;
    this.cleaner = cleaner;
    this.sessions = sessions;
  }

  /**
   * Resets the commit.
   *
   * @param entry The entry.
   */
  void reset(OperationEntry entry) {
    entry.acquire();
    this.entry = entry;
    this.session = sessions.getSession(entry.getSession());
    this.instant = Instant.ofEpochMilli(entry.getTimestamp());
    open = true;
  }

  @Override
  public long index() {
    return entry.getIndex();
  }

  @Override
  public Session session() {
    return session;
  }

  @Override
  public Instant time() {
    return instant;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class type() {
    return entry.getOperation().getClass();
  }

  @Override
  public Operation operation() {
    return entry.getOperation();
  }

  @Override
  public void clean() {
    Assert.state(open, "commit closed");
    if (entry.getOperation() instanceof Command)
      cleaner.clean(entry);
    close();
  }

  @Override
  public void clean(boolean tombstone) {
    Assert.state(open, "commit closed");
    if (entry.getOperation() instanceof Command)
      cleaner.clean(entry, tombstone);
    close();
  }

  @Override
  public void close() {
    if (open) {
      entry.release();
      entry = null;
      pool.release(this);
      open = false;
    }
  }

  @Override
  protected void finalize() throws Throwable {
    pool.warn(this);
  }

  @Override
  public String toString() {
    return String.format("%s[index=%d, session=%s, time=%s, operation=%s]", getClass().getSimpleName(), index(), session(), time(), operation());
  }

}