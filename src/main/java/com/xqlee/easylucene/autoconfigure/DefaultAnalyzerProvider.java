/*
 * Copyright 2017-2026 xqlee.com.
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

package com.xqlee.easylucene.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;

/**
 * 默认中文分词器提供者
 */
@Slf4j
public class DefaultAnalyzerProvider extends AnalyzerProvider {

    /**
     * 默认无停止分词的中文分词器提供者
     */
    public DefaultAnalyzerProvider() {
       log.info("DefaultAnalyzerProvider initialized successfully !");
    }

    @Override
    public Analyzer getAnalyzer() {
        // 默认无停止分词的中文分词器
        return new SmartChineseAnalyzer();
    }
}
