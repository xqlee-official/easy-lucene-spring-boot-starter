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

package com.xqlee.easylucene.thread;

import com.xqlee.easylucene.model.IndexDoc;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class EasyModelIndexWriteTaskThread implements Callable<Number> {

    /** 处理任务数据 **/
    private final List<IndexDoc> documents;
    /** 地址/目录 ***/
    private final Directory directory;
    private final Analyzer analyzer;
    private int num = 0;

    public EasyModelIndexWriteTaskThread(List<IndexDoc> documents, Analyzer analyzer,Directory directory) {
        this.documents = documents;
        this.directory = directory;
        this.analyzer = analyzer;
    }

    private IndexWriter getWriter(Directory directory) throws IOException {
        // 中文分词器
        // writer配置
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        return new IndexWriter(directory, indexWriterConfig);
    }

    public void doTask() throws IOException {
        // 获取写入器
        IndexWriter indexWriter = null;
        try {
            // 线程启用时间标记
            long startTime = System.currentTimeMillis();
            // 转换文档对象
            List<Document> docs = new ArrayList<>();
            Document doc;
            for (IndexDoc document : documents) {
                doc = document.toDoc();
                docs.add(doc);
            }
            // 获取写入器
            indexWriter = getWriter(directory);
            indexWriter.addDocuments(docs);
            num = indexWriter.numDocs();
            indexWriter.close();

            log.info("Easy Lucene Total Document[{}] Cost Time[{}] Thread Name [{}]",docs.size(),
                    (System.currentTimeMillis() - startTime),Thread.currentThread().getName());
            this.num = docs.size();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if(indexWriter!=null){
                indexWriter.close();
            }
        }
    }


    @Override
    public Number call() throws Exception {
        log.info("Easy Lucene Start Index Write Task Thread Name [{}]" , Thread.currentThread().getName());
        try {
            // 处理任务
            doTask();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return num;
    }
}
