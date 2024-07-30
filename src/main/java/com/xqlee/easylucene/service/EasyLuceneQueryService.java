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

package com.xqlee.easylucene.service;

import com.xqlee.easylucene.model.SearchField;
import com.xqlee.easylucene.model.SearchResult;
import org.apache.lucene.search.Query;

import java.util.List;

public interface EasyLuceneQueryService {

    /**
     * 最基本查询,查询分词字段-查询值
     * @param currentPage 当前页码，默认1，从1开始
     * @param pageSize 分页大小
     * @param searchField 查询字段
     * @param pathname 存入时候的路径
     * @return 查询结果
     */
    SearchResult queryOneField(int currentPage, int pageSize, SearchField searchField ,
                               String pathname);

    /**
     * 多字段查询
     * @param currentPage 当前页码，默认1，从1开始
     * @param pageSize 分页大小
     * @param searchFields 查询字段
     * @param pathname 存入时候的路径
     * @return 查询结果
     */
    SearchResult queryMultiField(int currentPage, int pageSize, List<SearchField> searchFields, String pathname);

    /**
     * 多字段查询
     * @param currentPage 当前页码，默认1，从1开始
     * @param pageSize 分页大小
     * @param pathname 存入时候的路径
     * @param searchFields 查询字段
     * @return 查询结果
     */
    SearchResult queryMultiField(int currentPage,int pageSize,String pathname,SearchField ...searchFields);

    /**
     * Query 条件查询
     *
     * @param currentPage 当前页码，默认1，从1开始
     * @param pageSize 分页大小
     * @param query 条件 ，（条件值注意 转义字符处理 QueryParser.escape(this.value);）
     * @param highlightFieldNames 高亮字段
     * @param pathname 存入时候的路径
     * @return 查询结果
     */
    SearchResult query(int currentPage, int pageSize, Query query, List<String> highlightFieldNames, String pathname);

}
