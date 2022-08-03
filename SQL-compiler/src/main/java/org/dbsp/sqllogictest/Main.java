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
import org.dbsp.util.Utilities;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * Execute all SqlLogicTest tests.
 */
public class Main {
    static String[] done = { "10", "62", "12", "53", "126" };

    static class NoMySql implements TestAcceptancePolicy {
        @Override
        public boolean accept(List<String> skip, List<String> only) {
            return !only.contains("mysql");
        }
    }

    static class TestLoader extends SimpleFileVisitor<Path> {
        int errors = 0;
        int tests = 0;
        final ISqlTestExecutor executor = new DBSPExecutor();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String extension = Utilities.getFileExtension(file.toString());
            String str = file.toString();
            for (String d: done)
                if (str.contains("_" + d + "."))
                    return FileVisitResult.CONTINUE;
            if (attrs.isRegularFile() && extension != null && extension.equals("test")) {
                // validates the test
                SqlTestFile test = null;
                try {
                    test = new SqlTestFile(file.toString(), new NoMySql());
                    this.tests += test.getTestCount();
                } catch (Exception ex) {
                    // We can't yet parse all kinds of tests
                    //noinspection UnnecessaryToStringCall
                    System.out.println(ex.toString());
                    this.errors++;
                }
                if (test != null) {
                    try {
                        System.out.println(file);
                        test.execute(this.executor);
                    } catch (SqlParseException | IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            //return FileVisitResult.TERMINATE;
            return FileVisitResult.CONTINUE;
        }
    }

    public static void main(String[] argv) throws IOException {
        String directory =
            // "../../sqllogictest/test/s.test"; //
            "../../sqllogictest/test/random/select";
        if (argv.length > 1)
            directory = argv[1];
        Path path = Paths.get(directory);
        TestLoader loader = new TestLoader();
        Files.walkFileTree(path, loader);
        System.out.println("Could not parse: " + loader.errors);
        System.out.println("Parsed tests: " + String.format("%,3d", loader.tests));
    }
}
