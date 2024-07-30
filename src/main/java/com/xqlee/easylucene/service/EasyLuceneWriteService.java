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

import com.xqlee.easylucene.model.IndexDoc;
import com.xqlee.easylucene.model.IndexField;

import java.io.IOException;
import java.util.List;

public interface EasyLuceneWriteService {



    /**
     * 写索引
     *
     * @param documents 文档、多个
     * @param path 路径
     * @return 数量
     * @throws IOException 异常
     */
    int writeIndex(List<IndexDoc> documents, String path) throws IOException;

    /**
     *
     * 根据文档中的唯一标识制度按进行文档更新操作
     *
     * @param indexFields
     *            待更新文档
     * @param idFieldName
     *            唯一标识字段
     * @param pathName
     *            路径名称
     * @throws IOException
     *             异常
     */
    void update(List<IndexField> indexFields, String idFieldName, String pathName) throws IOException;

    /**
     *
     * 根据唯一标识字段进行删除文档操作(非实时)
     *
     * @param idFieldName
     *            唯一标识字段名称
     * @param idFieldValue
     *            唯一标识字段值
     * @param pathName
     *            存放路径
     * @throws IOException
     *             异常
     */
    void delete(String idFieldName, String idFieldValue, String pathName) throws IOException;

    /**
     * 根据文档唯一标识字段进行删除(实时,IO开销大,建议少用或系统不忙时处理)
     *
     * @param idFieldName
     *            唯一标识字段名称
     * @param idFieldValue
     *            唯一标识字段值
     * @param pathName
     *            存放路径
     * @throws IOException
     *             异常
     */
    void deleteAndMerge(String idFieldName, String idFieldValue, String pathName) throws IOException;

    /**
     * 删除指定文档所有索引
     * @param pathname IndexDoc 路径
     * @throws IOException 异常
     */
    void deleteAll(String pathname)throws IOException;

}
