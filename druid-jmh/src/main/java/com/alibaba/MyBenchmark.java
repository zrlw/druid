/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.alibaba;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.stat.DruidDataSourceStatManager;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 10)
// Threads.MAX means using Runtime.getRuntime().availableProcessors().
@Threads(Threads.MAX)
@Fork(1)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MyBenchmark {
    private DruidDataSource dataSource;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        DruidDataSourceStatManager.clear();

        dataSource = new DruidDataSource();
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeoutMillis(100);
        dataSource.setLogAbandoned(true);
        dataSource.setTimeBetweenEvictionRunsMillis(10);
        dataSource.setMinEvictableIdleTimeMillis(300 * 1000);
        dataSource.setUrl("jdbc:mock:xxx");
        // does not wait response at putLast for pursuing performance.
        dataSource.setPutLastWaitResponseJustForUnitTestsCompatible(false);
        int procNum = Runtime.getRuntime().availableProcessors();
        dataSource.setMaxActive(procNum);
        dataSource.setInitialSize(procNum);
        dataSource.init();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        dataSource.close();
        Assert.assertEquals(0, DruidDataSourceStatManager.getInstance().getDataSourceList().size());
    }

    @Benchmark
    public void test_activeTrace() throws Exception {
        int count = 1000_00;
        int i = 0;
        try {
            for (; i < count; ++i) {
                Connection conn = dataSource.getConnection();
                Assert.assertNotNull(conn);
                // sleep 0 to trigger thread scheduling.
                Thread.sleep(0);
                conn.close();
                Assert.assertTrue(conn.isClosed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Assert.assertEquals(count, i);
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(MyBenchmark.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}