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
import com.xqlee.easylucene.model.IndexField;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
public class EasyModelIndexWriteTaskThread implements Callable<Number> {

    /** 处理任务数据 **/
    private final List<IndexDoc> documents;
    /** 地址/目录 ***/
    private Directory directory;
    private final Analyzer analyzer;
    /** 统计数量 **/
    private int num = 0;
    /** 写入器 **/
    private IndexWriter writer;
    /** 批量写入缓冲区大小MB **/
    private int ramBufferSize = 32;

    /**
     * 批量写入 适用于不同目录的批量写入
     * @param documents 索引数据
     * @param analyzer 分词器
     * @param ramBufferSize 批量写入缓冲区大小MB
     * @param directory 目录
     */
    public EasyModelIndexWriteTaskThread(List<IndexDoc> documents, Analyzer analyzer,int ramBufferSize,Directory directory) {
        this.documents = documents;
        this.directory = directory;
        this.analyzer = analyzer;
        this.ramBufferSize = ramBufferSize;
    }

    /**
     *  批量写入（适用于相同Directory的情况）
     * @param documents  索引数据
     * @param analyzer 分词器
     * @param writer 写入器
     */
    public EasyModelIndexWriteTaskThread(List<IndexDoc> documents, Analyzer analyzer,IndexWriter writer) {
        this.documents = documents;
        this.analyzer = analyzer;
        this.writer = writer;
    }

    public void doTask(){
        try {
            // 模式一：外部注入了 Writer，直接并发写入（不 commit，交由外部管理）
            long startTime = System.currentTimeMillis();
            if (this.writer!=null){
                List<Document> docs = new ArrayList<>();
                for (IndexDoc document : documents) {
                    Document doc = document.toDoc();
                    docs.add( doc);
                }
                writer.addDocuments(docs);
                this.num += docs.size();
            }else{
                // 模式二：独立运行模式，自行管理 Writer 生命周期
                Map<String, Analyzer> customAnalyzerMap = new HashMap<>();
                for (IndexDoc document : documents) {
                    for (IndexField field : document.getFields()) {
                        if (field.isAnalysis() && field.getAnalyzer() != null) {
                            customAnalyzerMap.putIfAbsent(field.getName(), field.getAnalyzer());
                        }
                    }
                }

                Analyzer finalAnalyzer = analyzer;
                if (!customAnalyzerMap.isEmpty()) {
                    finalAnalyzer = new PerFieldAnalyzerWrapper(analyzer, customAnalyzerMap);
                }

                IndexWriterConfig config = new IndexWriterConfig(finalAnalyzer);
                config.setRAMBufferSizeMB(ramBufferSize); // 批量写入建议 64MB 起步

                try (IndexWriter localWriter = new IndexWriter(directory, config)) {
                    List<Document> docs = new ArrayList<>();
                    for (IndexDoc document : documents) {
                        Document doc = document.toDoc();
                        docs.add( doc);
                    }
                    localWriter.addDocuments(docs);
                    this.num += docs.size();
                    localWriter.commit(); // 独立模式下，写完必须 commit
                }
            }

            log.info("Easy Lucene Total Document[{}] Cost Time[{}] Thread Name [{}]",documents.size(),
                    (System.currentTimeMillis() - startTime),Thread.currentThread().getName());
        }catch (Exception e) {
            log.error(e.getMessage(), e);
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
