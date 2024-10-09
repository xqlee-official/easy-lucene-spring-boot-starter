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

import com.xqlee.easylucene.autoconfigure.EasyLuceneProperties;
import com.xqlee.easylucene.model.SearchField;
import com.xqlee.easylucene.model.SearchResult;
import com.xqlee.easylucene.service.EasyLuceneQueryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class EasyLuceneQueryServiceImpl implements EasyLuceneQueryService {

    /** 分词器 **/
    private SmartChineseAnalyzer analyzer;

    @Resource
    EasyLuceneProperties easyLuceneProperties;

    public EasyLuceneQueryServiceImpl() {
        analyzer = new SmartChineseAnalyzer();
    }

    /**
     * 获取lucene读写目录
     * @param pathname 路径
     * @return 目录
     * @throws IOException 获取目录可能的io错误
     */
    private Directory getDirectory(String pathname) throws IOException {
        String dirPath = easyLuceneProperties.getStore_path() + File.separator + pathname;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return FSDirectory.open(Paths.get(dirPath));
    }

    private IndexReader getReader(Directory directory) throws IOException {
        return DirectoryReader.open(directory);
    }

    private ScoreDoc getLastScoreDoc(int currentPage, int pageSize, Query query, IndexSearcher indexSearcher)
            throws IOException {
        if (currentPage == 1) {
            return null;
        } // 第一页返回空
        int num = pageSize * (currentPage - 1);// 获取前面数量
        TopDocs topDocs = indexSearcher.search(query, num);
        return topDocs.scoreDocs[num - 1];// 获取最后一个
    }

    @Override
    public SearchResult queryOneField(int currentPage, int pageSize, SearchField searchField, String pathname) {
        SearchResult result = new SearchResult();
        result.setCurrentPage(currentPage);
        result.setPageSize(pageSize);
        try {
            List<Map<String, String>> list = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            Directory directory = this.getDirectory(pathname);
            IndexReader indexReader = this.getReader(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);// 查询器
            QueryParser parser = new QueryParser(searchField.getName(), analyzer);// 查询容器
            Query query = parser.parse(QueryParser.escape(searchField.getValue()));
            // 上次最后一个文档位置
            ScoreDoc after = getLastScoreDoc(currentPage, pageSize, query, indexSearcher);
            TopDocs topDocs = indexSearcher.searchAfter(after, query, pageSize);
            // 高亮处理
            QueryScorer queryScorer = new QueryScorer(query);
            Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
            SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
            highlighter.setTextFragmenter(fragmenter);

            HashMap<String, String> bean;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = indexSearcher.doc(scoreDoc.doc);
                bean = new HashMap<>();
                Iterator<IndexableField> iterator = doc.iterator();
                while (iterator.hasNext()) {
                    IndexableField field = iterator.next();
                    String fvalue = field.stringValue();
                    // deal highlight
                    if (field.name().equals(searchField.getName()) && searchField.isHighlight()) {
                        TokenStream tokenStream = analyzer.tokenStream(searchField.getName(), new StringReader(fvalue));
                        String highlight = highlighter.getBestFragment(tokenStream, field.stringValue());
                        highlight = highlight == null ? "" : highlight;
                        bean.put(field.name(), highlight);
                    } else {
                        if (fvalue == null) {
                            fvalue = "";
                        }
                        bean.put(field.name(), fvalue);
                    }

                }
                list.add(bean);
            }
            int total = topDocs.totalHits;

            result.setRows(list);
            result.setTotal(total);
            log.info("Easy Lucene keywords [{}] hit [{}] Cost Time [{}ms]", searchField.getValue(), total ,(System.currentTimeMillis() - startTime));
            indexReader.close();// 关闭
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setRows(new ArrayList<>());
            result.setTotal(0);
            return result;

        }

    }

    @Override
    public SearchResult queryMultiField(int currentPage, int pageSize, List<SearchField> searchFields, String pathname) {
        SearchResult result = new SearchResult();
        result.setCurrentPage(currentPage);
        result.setPages(pageSize);
        try {
            List<Map<String, String>> list = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            Directory directory = this.getDirectory(pathname);
            IndexReader indexReader = this.getReader(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);// 查询器

            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for (SearchField searchField : searchFields) {
                Query query;
                if (searchField.isBaseQuery()){
                    query=new TermQuery(new Term(searchField.getName(),QueryParser.escape(searchField.getValue())));
                }else{
                    // 解析查询条件进行查询
                    QueryParser parser = new QueryParser(searchField.getName(), analyzer);
                    query = parser.parse(QueryParser.escape(searchField.getValue()));
                }
                // 1．MUST和MUST：取得连个查询子句的交集。
                // 2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
                // 3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
                // 4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
                // 5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。
                // 6．MUST_NOT和MUST_NOT：无意义，检索无结果。
                booleanQueryBuilder.add(query, searchField.getOccur());
            }
            TopDocs tmp = indexSearcher.search(booleanQueryBuilder.build(), pageSize);
            int total = 0;
            if (tmp.totalHits > 0) {
                // 分页处理
                ScoreDoc after = getLastScoreDoc(currentPage, pageSize, booleanQueryBuilder.build(), indexSearcher);
                // 查询处理
                TopDocs topDocs = indexSearcher.searchAfter(after, booleanQueryBuilder.build(), pageSize);

                // 高亮处理
                QueryScorer queryScorer = new QueryScorer(booleanQueryBuilder.build());
                Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
                SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter();
                Highlighter highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
                highlighter.setTextFragmenter(fragmenter);

                Map<String, String> bean;
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = indexSearcher.doc(scoreDoc.doc);
                    bean = new HashMap<>();
                    for (IndexableField field : doc) {
                        // deal highlight
                        boolean tmpFlag = false;
                        for (SearchField searchField : searchFields) {
                            if (field.name().equals(searchField.getName()) && searchField.isHighlight()) {
                                TokenStream tokenStream = analyzer.tokenStream(searchField.getName(),
                                        new StringReader(field.stringValue()));
                                String highlight = highlighter.getBestFragment(tokenStream, field.stringValue());
                                highlight = StringUtils.isEmpty(highlight) ? field.stringValue() : highlight;
                                bean.put(field.name(), highlight);
                                tmpFlag = true;
                                break;
                            }
                        }
                        if (!tmpFlag) {// 非高亮字段,直接赋值
                            bean.put(field.name(), field.stringValue() == null ? "" : field.stringValue());
                        }
                    }
                    list.add(bean);
                }
                total = topDocs.totalHits;
            }

            result.setRows(list);
            result.setTotal(total);
            log.info("Easy Lucene Index Query Total[{}] Cost Time [{}ms]",total , (System.currentTimeMillis() - startTime));
            indexReader.close();// 关闭
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setRows(new ArrayList<>());
            result.setTotal(0);
            return result;
        }
    }
    
    @Override
    public SearchResult queryMultiField(int currentPage, int pageSize, String pathname, SearchField... searchFields) {

        SearchResult result = new SearchResult();
        try {
            List<Map<String, String>> list = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            Directory directory = this.getDirectory(pathname);
            IndexReader indexReader = this.getReader(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);// 查询器

            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for (SearchField searchField : searchFields) {
                QueryParser parser = new QueryParser(searchField.getName(), analyzer);// 查询容器
                Query query = parser.parse(QueryParser.escape(searchField.getValue()));
                // 1．MUST和MUST：取得连个查询子句的交集。
                // 2．MUST和MUST_NOT：表示查询结果中不能包含MUST_NOT所对应得查询子句的检索结果。
                // 3．SHOULD与MUST_NOT：连用时，功能同MUST和MUST_NOT。
                // 4．SHOULD与MUST连用时，结果为MUST子句的检索结果,但是SHOULD可影响排序。
                // 5．SHOULD与SHOULD：表示“或”关系，最终检索结果为所有检索子句的并集。
                // 6．MUST_NOT和MUST_NOT：无意义，检索无结果。
                booleanQueryBuilder.add(query, searchField.getOccur());
            }
            TopDocs tmp = indexSearcher.search(booleanQueryBuilder.build(), pageSize);
            int total = 0;
            if (tmp.totalHits > 0) {
                // 分页处理
                ScoreDoc after = getLastScoreDoc(currentPage, pageSize, booleanQueryBuilder.build(), indexSearcher);
                // 查询处理
                TopDocs topDocs = indexSearcher.searchAfter(after, booleanQueryBuilder.build(), pageSize);

                // 高亮处理
                QueryScorer queryScorer = new QueryScorer(booleanQueryBuilder.build());
                Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
                SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter();
                Highlighter highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
                highlighter.setTextFragmenter(fragmenter);

                Map<String, String> bean;
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = indexSearcher.doc(scoreDoc.doc);
                    bean = new HashMap<>();
                    Iterator<IndexableField> iterator = doc.iterator();
                    while (iterator.hasNext()) {
                        IndexableField field = iterator.next();

                        // deal highlight
                        boolean tmpFlag = false;
                        for (SearchField searchField : searchFields) {
                            if (field.name().equals(searchField.getName()) && searchField.isHighlight()) {
                                TokenStream tokenStream = analyzer.tokenStream(searchField.getName(),
                                        new StringReader(field.stringValue()));
                                String highlight = highlighter.getBestFragment(tokenStream, field.stringValue());
                                highlight = highlight == null ? "" : highlight;
                                bean.put(field.name(), highlight);
                                tmpFlag = true;
                                break;
                            }
                        }
                        if (!tmpFlag) {// 非高亮字段,直接赋值
                            bean.put(field.name(), field.stringValue() == null ? "" : field.stringValue());
                        }
                    }
                    list.add(bean);
                }
                total = topDocs.totalHits;
            }

            result.setRows(list);
            result.setTotal(total);
            log.debug("Easy Lucene Index Query Total [{}] Cost Time [{}ms]", total , (System.currentTimeMillis() - startTime));
            indexReader.close();// 关闭
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setRows(new ArrayList<>());
            result.setTotal(0);
            return result;
        }

    }

    @Override
    public SearchResult query(int currentPage, int pageSize, Query query,List<String> highlightFieldNames,String pathname) {
        SearchResult result = new SearchResult();
        result.setCurrentPage(currentPage);
        result.setPages(pageSize);
        try {
            List<Map<String, String>> list = new ArrayList<>();
            long startTime = System.currentTimeMillis();
            Directory directory = this.getDirectory(pathname);
            IndexReader indexReader = this.getReader(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);// 查询器

            TopDocs tmp = indexSearcher.search(query, pageSize);
            int total = 0;
            if (tmp.totalHits > 0) {
                // 分页处理
                ScoreDoc after = getLastScoreDoc(currentPage, pageSize, query, indexSearcher);
                // 查询处理
                TopDocs topDocs = indexSearcher.searchAfter(after, query, pageSize);

                // 高亮处理
                QueryScorer queryScorer = new QueryScorer(query);
                Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
                SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter();
                Highlighter highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
                highlighter.setTextFragmenter(fragmenter);

                Map<String, String> bean;
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = indexSearcher.doc(scoreDoc.doc);
                    bean = new HashMap<>();
                    Iterator<IndexableField> iterator = doc.iterator();
                    while (iterator.hasNext()) {
                        IndexableField field = iterator.next();

                        // deal highlight
                        boolean tmpFlag = false;
                        if (highlightFieldNames!=null){
                            for (String hField:highlightFieldNames){
                                if (field.name().equals(hField)) {
                                    TokenStream tokenStream = analyzer.tokenStream(hField,
                                            new StringReader(field.stringValue()));
                                    String highlight = highlighter.getBestFragment(tokenStream, field.stringValue());
                                    highlight = StringUtils.isEmpty(highlight) ? field.stringValue() : highlight;
                                    bean.put(field.name(), highlight);
                                    tmpFlag = true;
                                    break;
                                }
                            }
                        }
                        if (!tmpFlag) {// 非高亮字段,直接赋值
                            bean.put(field.name(), field.stringValue() == null ? "" : field.stringValue());
                        }
                    }
                    list.add(bean);
                }
                total = topDocs.totalHits;
            }

            result.setRows(list);
            result.setTotal(total);
            log.debug("Easy Lucene Index Query Total [{}] Cost Time [{}ms]", total , (System.currentTimeMillis() - startTime));
            indexReader.close();// 关闭
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.setRows(new ArrayList<>());
            result.setTotal(0);
            return result;

        }
    }




}
