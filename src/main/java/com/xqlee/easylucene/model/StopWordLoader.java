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

package com.xqlee.easylucene.model;

import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 停用词加载器 (基于 Lucene WordlistLoader)
 */
public class StopWordLoader {

    /**
     * 私有构造函数，防止实例化
     */
    private StopWordLoader() {
        throw new UnsupportedOperationException("StopWordLoader Utility class");
    }

    /**
     * 根据路径加载停用词集合
     * @param location 支持 classpath:stopwords.txt 或 file:/opt/app/stopwords.txt
     *  comment 默认 //
     * @return CharArraySet
     * @throws IOException 抛出 IOException
     */
    public static CharArraySet loadStopWords(String location) throws IOException {
        return loadStopWords(location,"//");
    }

    /**
     * 根据路径加载停用词集合
     * @param location 支持 classpath:stopwords.txt 或 file:/opt/app/stopwords.txt
     * @param comment 停用词文件中的注释行
     * @return CharArraySet
     * @throws IOException 抛出 IOException
     */
    public static CharArraySet loadStopWords(String location,String comment) throws IOException {
        InputStream inputStream;

        // 1. 根据前缀获取对应的 InputStream
        if (location.startsWith("classpath:")) {
            String path = location.substring("classpath:".length());
            inputStream = StopWordLoader.class.getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                throw new IOException("Classpath resource not found: " + location);
            }
        } else if (location.startsWith("file:")) {
            String path = location.substring("file:".length());
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                throw new IOException("File not found: " + location);
            }
            inputStream = Files.newInputStream(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported location prefix. Use 'classpath:' or 'file:'");
        }

        // 2. 使用 JDK 17 try-with-resources 包装流
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // 3. 调用 Lucene 官方的 WordlistLoader 解析并返回 CharArraySet
            return WordlistLoader.getWordSet(reader, comment);
        }
    }
}