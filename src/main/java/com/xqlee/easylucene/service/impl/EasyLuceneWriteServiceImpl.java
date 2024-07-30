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

package com.xqlee.easylucene.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import com.xqlee.easylucene.autoconfigure.EasyLuceneProperties;
import com.xqlee.easylucene.model.IndexDoc;
import com.xqlee.easylucene.model.IndexField;
import com.xqlee.easylucene.service.EasyLuceneWriteService;
import com.xqlee.easylucene.thread.EasyModelIndexWriteTaskThread;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class EasyLuceneWriteServiceImpl implements EasyLuceneWriteService {

    @Resource
    EasyLuceneProperties easyLuceneProperties;

    private IndexWriter getWriter(Directory directory) throws IOException {
        // 中文分词器
        SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
        // writer配置
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        return new IndexWriter(directory, indexWriterConfig);
    }

    private Directory getDirectory(String pathName) throws IOException {
        String dirPath = easyLuceneProperties.getStore_path() + File.separator + pathName;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return FSDirectory.open(Paths.get(dirPath));
    }


    @Override
    public int writeIndex(List<IndexDoc> documents, String path) throws IOException {
        int num = 0;
        synchronized (this) {
            long startTime = System.currentTimeMillis();
            // 拆分任务
            int totalSize = documents.size();
            // 任务数量
            int pageSize = totalSize / easyLuceneProperties.getMax_threads();
            if (pageSize<=0){
                pageSize = 1;
            }
            //
            List<List<IndexDoc>> subs = ListUtil.partition(documents,pageSize);

            // 多线程写
            ExecutorService pool = Executors.newCachedThreadPool();
            List<Future<Number>> tasks = new ArrayList<Future<Number>>();// 任务集合

            List<Directory> dirs = new ArrayList<>();
            // 循环创建线程
            FutureTask<Number> task;
            for (int i = 0; i < subs.size(); i++) {
                String pathDir = path + File.separator + "tmp" + File.separator + i + File.separator;
                Directory dir = getDirectory(pathDir);
                dirs.add(dir);
                task = new FutureTask<>(new EasyModelIndexWriteTaskThread(subs.get(i), dir));
                pool.execute(task);
                tasks.add(task);
            }
            // 获取线程返回值
            for (Future<Number> tk : tasks) {
                try {
                    num += (int) tk.get();
                } catch (InterruptedException | ExecutionException e) {
                   log.warn(e.getMessage(),e);
                }
            }
            // 关闭线程池
            pool.shutdown();
            long mstartTime = System.currentTimeMillis();
            // 合并索引
            Directory root = getDirectory(path);
            IndexWriter indexWriter = getWriter(root);
            indexWriter.addIndexes(dirs.toArray(new Directory[0]));
            indexWriter.commit();// 提交索引
            int total = indexWriter.numDocs();
            indexWriter.close();
            log.info("Easy Lucene All Index Write Tasks Complete!Total [{}] Document Number [{}] Merge Time [{}ms] Total Time [{}ms]"
                    ,num,total,(System.currentTimeMillis() - mstartTime),(System.currentTimeMillis() - startTime) );
            String tmpPath = easyLuceneProperties.getStore_path() + File.separator + path + File.separator + "tmp";
            File tempDir = new File(tmpPath);
            FileUtil.del(tempDir);
            return num;
        }
    }

    @Override
    public synchronized void update(List<IndexField> indexFields, String idFieldName, String pathName) throws IOException {
        long startTime = System.currentTimeMillis();
        // 1.路径
        Directory directory = this.getDirectory(pathName);
        // 2.写入器
        IndexWriter indexWriter = this.getWriter(directory);
        // 3.更新
        // 转换文档
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
        String idFieldValue = "";
        for (IndexField field : indexFields) {
            boolean isStore = field.isStore();
            boolean isAnalyzer = field.isAnalysis();
            Field.Store store;
            if (isStore) {
                store = Field.Store.YES;
            } else {
                store = Field.Store.NO;
            }
            if (isAnalyzer) {
                doc.add(new TextField(field.getName(), field.getValue(),
                        store));
            } else {
                doc.add(new StringField(field.getName(), field.getValue(),
                        store));
            }
            if (field.getName().equals(idFieldValue)) {
                idFieldValue = field.getValue();
            }
        }
        indexWriter.updateDocument(new Term(idFieldName, new BytesRef(idFieldValue.getBytes())), doc);
        log.info("Easy Lucene Index Update Time [{}ms]", (System.currentTimeMillis() - startTime));
    }

    @Override
    public synchronized void delete(String idFieldName, String idFieldValue, String pathName) throws IOException {
        IndexWriter indexWriter = null;
        try {
            long startTime = System.currentTimeMillis();
            // 1.路径
            Directory directory = this.getDirectory(pathName);
            // 2.写入器
            indexWriter = this.getWriter(directory);
            // 3.删除(软删除)
            indexWriter.deleteDocuments(new Term(idFieldName, new BytesRef(idFieldValue.getBytes())));
            // 提交操作
            indexWriter.commit();
            indexWriter.close();
            log.info("Easy Lucene Index Delete Time [{}ms]", (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (indexWriter != null) {
                indexWriter.close();
            }
        }
    }

    @Override
    public void deleteAndMerge(String idFieldName, String idFieldValue, String pathName) throws IOException {
        long startTime = System.currentTimeMillis();
        // 1.路径
        Directory directory = this.getDirectory(pathName);
        // 2.写入器
        IndexWriter indexWriter = this.getWriter(directory);
        // 3.删除(软删除)
        indexWriter.deleteDocuments(new Term(idFieldName, new BytesRef(idFieldValue.getBytes())));
        // 4.执行io操作,合并删除文件
        indexWriter.forceMergeDeletes();
        // 提交操作
        indexWriter.commit();
        indexWriter.close();
        log.info("Easy Lucene Delete Cost Time [{}ms]",(System.currentTimeMillis() - startTime));
    }

    @Override
    public void deleteAll(String pathname) throws IOException {
        try {
            long startTime = System.currentTimeMillis();
            // 1.路径
            Directory directory = this.getDirectory(pathname);
            // 2.写入器
            IndexWriter indexWriter = this.getWriter(directory);
            indexWriter.deleteAll();
            indexWriter.forceMergeDeletes();
            indexWriter.commit();
            indexWriter.close();
            log.info("Easy Lucene Deletes Cost Time [{}ms]",(System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IOException("Easy Lucene Delete Error :" + e.getMessage());
        }
    }

}
