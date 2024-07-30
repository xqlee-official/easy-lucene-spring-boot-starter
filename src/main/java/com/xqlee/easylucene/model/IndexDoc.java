/*
 * Copyright 2017-2024 xqlee.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xqlee.easylucene.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndexDoc {

    public IndexDoc() {
    }
    /****
     * 多个字段创建
     * @param fields 字段
     */
    public IndexDoc(IndexField... fields) {
        if (fields != null) {
            this.fields.addAll(Arrays.asList(fields));
        }
    }

    private final List<IndexField> fields = new ArrayList<>();

    public Document toDoc() {
        Document doc = new Document();
        for (IndexField field : fields) {
            Field.Store store;
            if (field.isStore()) {
                store = Field.Store.YES;
            } else {
                store = Field.Store.NO;
            }
            if (field.isAnalysis()) {
                doc.add(new TextField(field.getName(), field.getValue(), store));
            } else {
                doc.add(new StringField(field.getName(), field.getValue(), store));
            }
        }
        return doc;
    }
}
