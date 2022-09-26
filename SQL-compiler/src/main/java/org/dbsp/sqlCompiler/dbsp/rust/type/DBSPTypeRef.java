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

import javax.annotation.Nullable;

/**
 * A type of the form &type.
 */
public class DBSPTypeRef extends DBSPType {
    public final DBSPType type;
    public final boolean mutable;

    public DBSPTypeRef(DBSPType type, boolean mutable) {
        super(null, false);
        this.type = type;
        this.mutable = mutable;
    }

    public DBSPTypeRef(DBSPType type) {
        this(type, false);
    }

    @Override
    public DBSPType setMayBeNull(boolean mayBeNull) {
        if (mayBeNull)
            throw new RuntimeException("Reference types cannot be null");
        return this;
    }

    public boolean same(@Nullable DBSPType other) {
        if (!super.same(other))
            return false;
        assert other != null;
        DBSPTypeRef oRef = other.as(DBSPTypeRef.class);
        if (oRef == null)
            return false;
        return this.type.same(oRef.type);
    }

    @Override
    public void accept(Visitor visitor) {
        if (!visitor.preorder(this)) return;
        this.type.accept(visitor);
        visitor.postorder(this);
    }
}
