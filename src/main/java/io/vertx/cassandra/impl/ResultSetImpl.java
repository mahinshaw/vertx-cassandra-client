/*
 * Copyright 2018 The Vert.x Community.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.cassandra.impl;

import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.Row;
import io.vertx.cassandra.ResultSet;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.vertx.cassandra.impl.Util.handleOnContext;

/**
 * @author Pavel Drankou
 * @author Thomas Segismont
 */
public class ResultSetImpl implements ResultSet {

  private com.datastax.oss.driver.api.core.cql.AsyncResultSet resultSet;
  private Vertx vertx;

  public ResultSetImpl(com.datastax.oss.driver.api.core.cql.AsyncResultSet resultSet, Vertx vertx) {
    this.resultSet = resultSet;
    this.vertx = vertx;
  }

  @Override
  public boolean isExhausted() {
    // There are no more pages to fetch and the current result set is returning null.
    // resultSet.one() == null might change to resultSet.remaining() == 0;
    return !resultSet.hasMorePages() && resultSet.one() == null;
  }

  @Override
  public boolean isFullyFetched() {
    return !resultSet.hasMorePages();
  }

  @Override
  public int getAvailableWithoutFetching() {
    return resultSet.remaining();
  }

  @Override
  public ResultSet fetchMoreResults(Handler<AsyncResult<Void>> handler) {
    Context context = vertx.getOrCreateContext();
    handleOnContext(resultSet.fetchNextPage(), context, ignore -> null, handler);
    return this;
  }

  @Override
  public Future<Void> fetchMoreResults() {
    Promise<Void> promise = Promise.promise();
    fetchMoreResults(promise);
    return promise.future();
  }

  @Override
  public ResultSet one(Handler<AsyncResult<Row>> handler) {
    if (getAvailableWithoutFetching() == 0 && !isFullyFetched()) {
      Context context = vertx.getOrCreateContext();
      handleOnContext(resultSet.fetchNextPage(), context, ignored -> resultSet.one(), handler);
    } else {
      handler.handle(Future.succeededFuture(resultSet.one()));
    }
    return this;
  }

  @Override
  public Future<@Nullable Row> one() {
    Promise<Row> promise = Promise.promise();
    one(promise);
    return promise.future();
  }

  @Override
  public ResultSet several(int amount, Handler<AsyncResult<List<Row>>> handler) {
    loadSeveral(amount, new ArrayList<>(amount), handler);
    return this;
  }

  @Override
  public Future<List<Row>> several(int amount) {
    Promise<List<Row>> promise = Promise.promise();
    several(amount, promise);
    return promise.future();
  }

  private void loadSeveral(int remainedToAdd, List<Row> resultedList, Handler<AsyncResult<List<Row>>> handler) {
    int availableWithoutFetching = getAvailableWithoutFetching();
    if (remainedToAdd > 0) {
      if (availableWithoutFetching > 0 && availableWithoutFetching < remainedToAdd) {
        List<Row> rows = getRows(availableWithoutFetching);
        resultedList.addAll(rows);
        loadSeveral(remainedToAdd - rows.size(), resultedList, handler);
      } else if (availableWithoutFetching >= remainedToAdd) {
        List<Row> rows = getRows(remainedToAdd);
        resultedList.addAll(rows);
        handler.handle(Future.succeededFuture(resultedList));
      } else if (availableWithoutFetching == 0) {
        if (isFullyFetched()) {
          handler.handle(Future.succeededFuture(resultedList));
        } else {
          fetchMoreResults(voidAsyncResult -> {
            if (voidAsyncResult.succeeded()) {
              loadSeveral(remainedToAdd, resultedList, handler);
            } else {
              handler.handle(Future.failedFuture(voidAsyncResult.cause()));
            }
          });
        }
      }
    } else {
      handler.handle(Future.succeededFuture(resultedList));
    }
  }

  private List<Row> getRows(int amountToFetch) {
    List<Row> rows = new ArrayList<>(amountToFetch);
    for (int i = 0; i < amountToFetch; i++) {
      Row row = resultSet.one();
      if (row != null) {
        rows.add(row);
      } else {
        break;
      }
    }
    return rows;
  }

  @Override
  public ResultSet all(Handler<AsyncResult<List<Row>>> handler) {
    loadMore(vertx.getOrCreateContext(), Collections.emptyList(), handler);
    return this;
  }

  @Override
  public Future<List<Row>> all() {
    Promise<List<Row>> promise = Promise.promise();
    all(promise);
    return promise.future();
  }

  private void loadMore(Context context, List<Row> loaded, Handler<AsyncResult<List<Row>>> handler) {
    int availableWithoutFetching = getAvailableWithoutFetching();
    List<Row> rows = new ArrayList<>(loaded.size() + availableWithoutFetching);
    for (int i = 0; i < availableWithoutFetching; i++) {
      rows.add(resultSet.one());
    }

    if (!isFullyFetched()) {
      handleOnContext(fetchMoreResults(), context, ar -> {
        if (ar.succeeded()) {
          loadMore(context, rows, handler);
        } else {
          handler.handle(Future.failedFuture(ar.cause()));
        }
      });
    } else {
      handler.handle(Future.succeededFuture(rows));
    }
  }

  @Override
  public ColumnDefinitions getColumnDefinitions() {
    return resultSet.getColumnDefinitions();
  }

  @Override
  public boolean wasApplied() {
    return resultSet.wasApplied();
  }
}
