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

package org.dbsp.sqlCompiler.dbsp.rust.type;

import org.dbsp.sqlCompiler.dbsp.Visitor;
import org.dbsp.sqlCompiler.dbsp.rust.expression.*;
import org.dbsp.sqlCompiler.dbsp.rust.expression.literal.DBSPDoubleLiteral;
import org.dbsp.sqlCompiler.dbsp.rust.expression.literal.DBSPLiteral;

import javax.annotation.Nullable;

public class DBSPTypeDouble extends DBSPTypeFP implements IsNumericType, IDBSPBaseType {
    protected DBSPTypeDouble(@Nullable Object node, boolean mayBeNull) { super(node, mayBeNull); }

    @Override
    public DBSPType setMayBeNull(boolean mayBeNull) {
        if (this.mayBeNull == mayBeNull)
            return this;
        return new DBSPTypeDouble(this.getNode(), mayBeNull);
    }

    @Override
    public String shortName() {
        return "d";
    }

    public static final DBSPTypeDouble instance = new DBSPTypeDouble(null,false);

    @Override
    public boolean same(@Nullable DBSPType type) {
        if (!super.same(type))
            return false;
        assert type != null;
        return type.is(DBSPTypeDouble.class);
    }

    @Override
    public int getWidth() {
        return 64;
    }

    @Override
    public DBSPExpression castFrom(DBSPExpression source) {
        return castFrom(source, "F64");
    }

    @Override
    public DBSPLiteral getZero() {
        return new DBSPDoubleLiteral(0D, this.mayBeNull);
    }

    @Override
    public DBSPLiteral getOne() {
        return new DBSPDoubleLiteral(1D, this.mayBeNull);
    }

    @Override
    public void accept(Visitor visitor) {
        if (!visitor.preorder(this)) return;
        visitor.postorder(this);
    }
}
