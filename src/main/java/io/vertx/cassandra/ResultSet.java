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
package io.vertx.cassandra;

import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.Row;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.List;

/**
 * It is like {@link com.datastax.oss.driver.api.core.cql.ResultSet}, but adapted for Vert.x.
 *
 * @author Pavel Drankou
 * @author Thomas Segismont
 */
@VertxGen
public interface ResultSet {

  /**
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#isExhausted()
   */
  boolean isExhausted();

  /**
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#isFullyFetched()
   */
  boolean isFullyFetched();

  /**
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#getAvailableWithoutFetching()
   */
  int getAvailableWithoutFetching();

  /**
   * @param handler handler called when result is fetched
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#fetchMoreResults()
   */
  @Fluent
  ResultSet fetchMoreResults(Handler<AsyncResult<Void>> handler);

  /**
   * Like {@link #fetchMoreResults(Handler)} but returns a {@code Future} of the asynchronous result.
   */
  Future<Void> fetchMoreResults();

  /**
   * The method should <strong>not</strong> be used concurrently with others like {@link #several(int, Handler)} or {@link #all(Handler)}.
   * This may lead to unexpected result.
   *
   * @param handler handler called when one row is fetched
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#one
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  ResultSet one(Handler<AsyncResult<@Nullable Row>> handler);

  /**
   * Like {@link #one(Handler)} but returns a {@code Future} of the asynchronous result.
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  Future<@Nullable Row> one();

  /**
   * Fetch a specific amount of rows and notify via a handler.
   * <p>
   * If remaining amount of rows in a result set is less than desired amount of rows to fetch,
   * the {@code handler} will be called with a successful result encompassing just the remaining rows.
   *
   * <p>
   * The method should <strong>not</strong> be used concurrently with others like {@link #one(Handler)} or {@link #all(Handler)}.
   * This may lead to unexpected result.
   *
   * @param handler the handler
   * @param amount the amount
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  ResultSet several(int amount, Handler<AsyncResult<List<Row>>> handler);

  /**
   * Like {@link #several(int, Handler)} but returns a {@code Future} of the asynchronous result.
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  Future<List<Row>> several(int amount);

  /**
   * The method should <strong>not</strong> be used concurrently with others like {@link #several(int, Handler)} or {@link #one(Handler)}.
   * This may lead to unexpected result.
   *
   * @param handler handler called when all the rows is fetched
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#all
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  @Fluent
  ResultSet all(Handler<AsyncResult<List<Row>>> handler);

  /**
   * Like {@link #all(Handler)} but returns a {@code Future} of the asynchronous result.
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  Future<List<Row>> all();

  /**
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#getColumnDefinitions
   */
  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  ColumnDefinitions getColumnDefinitions();

  /**
   * @see com.datastax.oss.driver.api.core.cql.ResultSet#wasApplied
   */
  boolean wasApplied();
}
