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
 */

package org.dbsp.sqllogictest.executors;

import org.apache.calcite.sql.parser.SqlParseException;
import org.dbsp.sqlCompiler.compiler.visitors.DBSPCompiler;
import org.dbsp.sqlCompiler.ir.expression.literal.DBSPZSetLiteral;
import org.dbsp.sqllogictest.SqlStatement;
import org.dbsp.sqllogictest.SqlTestFile;
import org.dbsp.util.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a hybrid test executor which keeps all the state in a
 * database using JDBC and executes all queries using DBSP>
 */
public class DBSP_JDBC_Executor extends DBSPExecutor {
    private final JDBCExecutor statementExecutor;
    private final List<String> tablesCreated;

    /**
     * @param execute If true the tests are executed, otherwise they are only compiled to Rust.
     * @param incremental If true generate incremental streaming circuits.
     */
    public DBSP_JDBC_Executor(JDBCExecutor executor, boolean execute, boolean incremental) {
        super(execute, incremental);
        this.statementExecutor = executor;
        this.tablesCreated = new ArrayList<>();
    }

    @Override
    public TableValue[] getInputSets(DBSPCompiler compiler) throws SQLException {
        TableValue[] result = new TableValue[this.tablesCreated.size()];
        int i = 0;
        for (String table: this.tablesCreated) {
            DBSPZSetLiteral lit = this.statementExecutor.getTableContents(table);
            result[i++] = new TableValue(table, lit);
        }
        return result;
    }

    @Nullable
    String rewriteCreateTable(String command) throws SQLException {
        String regex = "create\\s+table\\s+(\\w+)";
        Pattern pat = Pattern.compile(regex);
        Matcher m = pat.matcher(command);
        if (!m.find())
            return null;
        String tableName = m.group(1);
        this.tablesCreated.add(tableName);
        return this.statementExecutor.generateCreateStatement(tableName);
    }

    public boolean statement(SqlStatement statement) throws SQLException {
        this.statementExecutor.statement(statement);
        String command = statement.statement.toLowerCase();
        if (this.getDebugLevel() > 0)
            Logger.instance.append("Executing ")
                    .append(command)
                    .newline();
        @Nullable
        String create = this.rewriteCreateTable(command);
        if (create != null) {
            SqlStatement rewritten = new SqlStatement(create, statement.shouldPass);
            super.statement(rewritten);
        } else if (command.contains("drop table") ||
                command.contains("create view") ||
                command.contains("drop view")) {
            // These should perhaps use a regex too.
            super.statement(statement);
        }
        return true;
    }

    @Override
    public TestStatistics execute(SqlTestFile file)
            throws SqlParseException, IOException, InterruptedException, SQLException {
        this.statementExecutor.establishConnection();
        this.statementExecutor.dropAllTables();
        this.statementExecutor.dropAllViews();
        return super.execute(file);
    }
}
