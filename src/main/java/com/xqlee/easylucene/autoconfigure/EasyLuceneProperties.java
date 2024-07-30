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

package com.xqlee.easylucene.autoconfigure;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@Data
@ConfigurationProperties(prefix = "spring.lucene")
public class EasyLuceneProperties {

    /**
     * 设置索引存储目录，默认运行目录的 xqlee/lucene下面
     */
    String store_path = "xqlee/lucene";

    /**
     * 处理任务开启最大线程数量，默认2
     * 数量因根据cpu数量、硬盘io性能自行调整
     */
    int max_threads = 2;
}
