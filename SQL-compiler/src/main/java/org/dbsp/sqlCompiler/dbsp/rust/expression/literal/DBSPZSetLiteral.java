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

package org.dbsp.sqlCompiler.dbsp.rust.expression.literal;

import org.dbsp.sqlCompiler.dbsp.Visitor;
import org.dbsp.sqlCompiler.dbsp.rust.expression.DBSPExpression;
import org.dbsp.sqlCompiler.dbsp.rust.expression.IDBSPContainer;
import org.dbsp.sqlCompiler.dbsp.rust.type.DBSPType;
import org.dbsp.sqlCompiler.dbsp.rust.type.DBSPTypeZSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a (constant) ZSet described by its elements.
 * A ZSet is a map from tuples to integer weights.
 * In general weights should not be zero.
 */
public class DBSPZSetLiteral extends DBSPLiteral implements IDBSPContainer {
    public final Map<DBSPExpression, Integer> data;
    public final DBSPTypeZSet zsetType;

    /**
     * Create a ZSet literal from a set of data values.
     * @param weightType  Type of weight used.
     * @param data Data to insert in zset - cannot be empty, since
     *             it is used to extract the zset type.
     *             To create empty zsets use the constructor
     *             with just a type argument.
     */
    public DBSPZSetLiteral(DBSPType weightType, DBSPExpression... data) {
        super(null, new DBSPTypeZSet(data[0].getNonVoidType(), weightType), 0);
        // value 0 is not used
        this.zsetType = this.getNonVoidType().to(DBSPTypeZSet.class);
        this.data = new HashMap<>();
        for (DBSPExpression e: data) {
            if (!e.getNonVoidType().same(data[0].getNonVoidType()))
                throw new RuntimeException("Not all values of set have the same type:" +
                    e.getType() + " vs " + data[0].getType());
            this.add(e);
        }
    }

    public DBSPZSetLiteral(DBSPType type) {
        super(null, type, 0); // Value is unused, but needs to be non-null
        this.zsetType = this.getNonVoidType().to(DBSPTypeZSet.class);
        this.data = new HashMap<>();
    }

    public DBSPType getElementType() {
        return this.zsetType.elementType;
    }

    public void add(DBSPExpression expression) {
        this.add(expression, 1);
    }

    public void add(DBSPExpression expression, int weight) {
        // We expect the expression to be a constant value (a literal)
        if (!expression.getNonVoidType().same(this.getElementType()))
            throw new RuntimeException("Added element does not match zset type " +
                    expression.getType() + " vs " + this.getElementType());
        if (this.data.containsKey(expression)) {
            int oldWeight = this.data.get(expression);
            int newWeight = weight + oldWeight;
            if (newWeight == 0)
                this.data.remove(expression);
            else
                this.data.put(expression, weight + oldWeight);
            return;
        }
        this.data.put(expression, weight);
    }

    public void add(DBSPZSetLiteral other) {
        if (!this.getNonVoidType().same(other.getNonVoidType()))
            throw new RuntimeException("Added zsets do not have the same type " +
                    this.getElementType() + " vs " + other.getElementType());
        other.data.forEach(this::add);
    }

    public int size() {
        return this.data.size();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        boolean first = true;
        for (Map.Entry<DBSPExpression, Integer> e: data.entrySet()) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.append(e.getKey())
                    .append(" => ")
                    .append(e.getValue());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public void accept(Visitor visitor) {
        if (!visitor.preorder(this)) return;
        for (DBSPExpression expr: this.data.keySet())
            expr.accept(visitor);
        visitor.postorder(this);
    }
}
