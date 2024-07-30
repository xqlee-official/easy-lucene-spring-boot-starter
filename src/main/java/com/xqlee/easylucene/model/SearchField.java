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

import lombok.Data;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;

import java.util.Objects;

@Data
public class SearchField {

    public SearchField() {

    }

    public SearchField(String name, String value, boolean isHighlight) {
        this.name = name;
        this.value = value;
        this.isHighlight = isHighlight;

    }

    /**
     * 查询字段构建
     *
     * @param name        查询字段名
     * @param value       查询字段值
     * @param isHighlight 是否高亮
     * @param occur       多条件查询时候关联关系
     *                    1．MUST和MUST：取得连个查询子句的交集。
     *                    2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
     *                    3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
     *                    4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
     *                    5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。
     *                    6．MUST_NOT和MUST_NOT：无意义，检索无结果。
     */
    public SearchField(String name, String value, boolean isHighlight, BooleanClause.Occur occur) {
        this.name = name;
        this.value = value;
        this.isHighlight = isHighlight;
        this.occur = occur;

    }
    public SearchField(String name, String value, boolean isHighlight,boolean isBaseQuery, BooleanClause.Occur occur) {
        this.name = name;
        this.value = value;
        this.isHighlight = isHighlight;
        this.occur = occur;
        this.isBaseQuery=isBaseQuery;

    }


    /**
     * 字段名称
     **/
    private String name;
    /**
     * 字段值
     ***/
    private String value;
    /**
     * 是否处理高亮
     ***/
    private boolean isHighlight;
    private boolean isBaseQuery;
    /**
     * 多条件查询时候
     **/
    private BooleanClause.Occur occur;

    public void setOccur(BooleanClause.Occur occur) {
        if (occur == null) {
            occur = BooleanClause.Occur.MUST;
        }
        this.occur = occur;
    }

    public String getValue(){
        if (Objects.nonNull(this.value)){
            return QueryParser.escape(this.value);
        }
        return null;
    }
}
