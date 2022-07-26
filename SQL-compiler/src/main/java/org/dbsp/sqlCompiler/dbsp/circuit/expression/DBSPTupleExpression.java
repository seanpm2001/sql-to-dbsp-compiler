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

package org.dbsp.sqlCompiler.dbsp.circuit.expression;

import org.dbsp.sqlCompiler.dbsp.circuit.type.DBSPType;
import org.dbsp.sqlCompiler.dbsp.circuit.type.DBSPTypeTuple;
import org.dbsp.util.IndentStringBuilder;
import org.dbsp.util.Linq;

import javax.annotation.Nullable;
import java.util.List;

public class DBSPTupleExpression extends DBSPExpression {
    private final List<DBSPExpression> fields;

    public static final DBSPTupleExpression emptyTuple = new DBSPTupleExpression();

    public DBSPTupleExpression(@Nullable Object object, DBSPType tupleType, List<DBSPExpression> fields) {
        super(object, tupleType);
        this.fields = fields;
        if (!tupleType.is(DBSPTypeTuple.class))
            throw new RuntimeException("Expected a tuple type " + tupleType);
        DBSPTypeTuple tuple = tupleType.to(DBSPTypeTuple.class);
        if (tuple.size() != fields.size())
            throw new RuntimeException("Tuple size does not match field size " + tuple + " vs " + fields);
        int i = 0;
        for (DBSPType fieldType: tuple.tupArgs) {
            DBSPExpression field = fields.get(i);
            if (!fieldType.same(field.getType()))
                throw new RuntimeException("Tuple field " + i + " type " + fieldType +
                        " does not match expression "+ fields.get(i) + " with type " + field.getType());
            i++;
        }
    }

    public DBSPTupleExpression(DBSPExpression... expressions) {
        this(null, new DBSPTypeTuple(null,
                Linq.map(Linq.list(expressions), DBSPExpression::getType)),
                Linq.list(expressions)
        );
    }

    @Override
    public IndentStringBuilder toRustString(IndentStringBuilder builder) {
        return builder.append("(")
                .join(", ", this.fields)
                .append(")");
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        boolean first = true;
        for (DBSPExpression e: this.fields) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.append(e);
        }
        builder.append(")");
        return builder.toString();
    }
}
