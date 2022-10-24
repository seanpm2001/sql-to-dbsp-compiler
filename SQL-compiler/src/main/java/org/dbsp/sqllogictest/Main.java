/*
 * Copyright 2022 VMware, Inc.
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */

package org.dbsp.sqllogictest;

import org.apache.calcite.sql.parser.SqlParseException;
import org.dbsp.sqlCompiler.circuit.SqlRuntimeLibrary;
import org.dbsp.sqllogictest.executors.*;
import org.dbsp.util.Linq;
import org.dbsp.util.Utilities;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Execute all SqlLogicTest tests.
 */
public class Main {
    // Following are queries that calcite fails to parse.
    static final String[] skipFiles = {};

    static class General implements QueryAcceptancePolicy {
        @Override
        public boolean accept(List<String> skip, List<String> only) {
            if (only.contains("postgresql"))
                return true;
            if (!only.isEmpty())
                return false;
            return !skip.contains("postgresql");
        }
    }

    static class MySql implements QueryAcceptancePolicy {
        @Override
        public boolean accept(List<String> skip, List<String> only) {
            if (only.contains("mysql"))
                return true;
            if (!only.isEmpty())
                return false;
            return !skip.contains("mysql");
        }
    }

    static class TestLoader extends SimpleFileVisitor<Path> {
        int errors = 0;
        private final SqlTestExecutor executor;
        final SqlTestExecutor.TestStatistics statistics;
        private final QueryAcceptancePolicy policy;

        /**
         * Creates a new class that reads tests from a directory tree and executes them.
         * @param executor Program that knows how to generate and run the tests.
         * @param policy   Policy that dictates which tests can be executed.
         */
        TestLoader(SqlTestExecutor executor, QueryAcceptancePolicy policy) {
            this.executor = executor;
            this.statistics = new SqlTestExecutor.TestStatistics();
            this.policy = policy;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String extension = Utilities.getFileExtension(file.toString());
            String str = file.toString();
            //noinspection RedundantOperationOnEmptyContainer
            for (String d: skipFiles)
                if (str.contains("expr/slt_good_" + d + "."))
                    return FileVisitResult.CONTINUE;
            if (attrs.isRegularFile() && extension != null && extension.equals("test")) {
                // validates the test
                SqlTestFile test = null;
                try {
                    test = new SqlTestFile(file.toString());
                    test.parse(this.policy);
                } catch (Exception ex) {
                    // We can't yet parse all kinds of tests
                    //noinspection UnnecessaryToStringCall
                    System.out.println(ex.toString());
                    this.errors++;
                }
                if (test != null) {
                    try {
                        System.out.println(file);
                        SqlTestExecutor.TestStatistics stats = this.executor.execute(test);
                        this.statistics.add(stats);
                    } catch (SqlParseException | IOException | InterruptedException |
                            SQLException | NoSuchAlgorithmException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static void main(String[] argv) throws IOException {
        SqlRuntimeLibrary.instance.writeSqlLibrary( "../lib/genlib/src/lib.rs");
        String benchDir = "../../sqllogictest/test";
        int batchSize = 500;
        int skipPerFile = 0;
        List<String> files = Linq.list(
                /*
                "random/select",  //done
                "random/expr",    // done
                "random/groupby", // done
                "random/aggregates", // done
                "select1.test",  // done
                "select2.test",  // done
                "select3.test",  // done
                "select4.test",  // done
                "select5.test",  // done
                "index/orderby", // done
                "index/between", // done
                "index/view",    // done
                "index/in",      // done
                "index/delete",  // done
                "index/commute", // done
                "index/orderby_nosort", // done
                 */
                "random/aggregates/slt_good_12.test",
                "index/random",
                "evidence"
        );

        String[] args = { "-e", "hybrid", "-i" };
        if (argv.length > 0) {
            args = argv;
        } else {
            List<String> a = new ArrayList<>();
            a.addAll(Linq.list(args));
            a.addAll(files);
            args = a.toArray(new String[0]);
        }
        ExecutionOptions options = new ExecutionOptions(args);
        SqlTestExecutor executor = options.getExecutor();

        System.out.println(options);
        QueryAcceptancePolicy policy =
                executor.is(DBSPExecutor.class) ? new General() : new MySql();
        TestLoader loader = new TestLoader(executor, policy);
        for (String file : options.getDirectories()) {
            if (file.startsWith("select"))
                batchSize = Math.min(batchSize, 20);
            if (file.startsWith("select5"))
                batchSize = Math.min(batchSize, 5);
            Path path = Paths.get(benchDir + "/" + file);
            //Logger.instance.setDebugLevel("DBSPExecutor", 1);
            //Logger.instance.setDebugLevel("JDBCExecutor", 1);
            //Logger.instance.setDebugLevel("DBSP_JDBC_Executor", 1);
            if (executor.is(DBSPExecutor.class))
                executor.to(DBSPExecutor.class).setBatchSize(batchSize, skipPerFile);
            Files.walkFileTree(path, loader);
        }
        System.out.println("Files that could not be not parsed: " + loader.errors);
        System.out.println(loader.statistics);
    }
}
