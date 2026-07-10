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

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public void doTask(){
        try {
            // 线程启用时间标记
            long startTime = System.currentTimeMillis();
            Document doc;
            for (IndexDoc document : documents) {
                doc = document.toDoc();
                List<IndexField> list = document.getFields().stream().filter(IndexField::isAnalysis).filter(e -> Objects.nonNull(e.getAnalyzer())).toList();
                if (!list.isEmpty()){
                    Map<String,Analyzer> map = list.stream().collect(java.util.stream.Collectors.toMap(IndexField::getName, IndexField::getAnalyzer));
                    Analyzer perFieldAnalyzerWrapper = new PerFieldAnalyzerWrapper(analyzer,map);
                    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(perFieldAnalyzerWrapper);
                    try (IndexWriter writer = new IndexWriter(directory, indexWriterConfig)){
                        writer.addDocument(doc);
                        this.num++;
                    }
                }else{
                    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
                    try (IndexWriter writer = new IndexWriter(directory, indexWriterConfig)){
                        writer.addDocument(doc);
                        this.num++;
                    }
                }

            } // end for
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
