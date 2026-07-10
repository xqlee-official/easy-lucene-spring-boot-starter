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

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.*;

import java.io.IOException;

/**
 * 阈值过滤器
 */
@Slf4j
public class ThresholdFilterCollector extends FilterCollector {

    private final float threshold;

    /**
     * 构造函数
     *
     * @param in      内部 Collector
     * @param threshold 阈值
     */
    public ThresholdFilterCollector(Collector in, float threshold) {
        // 调用父类构造器，将真正的 Collector（如 TopScoreDocCollector）传入
        super(in);
        this.threshold = threshold;
    }

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
        // 获取内部 Collector 的 LeafCollector
        final LeafCollector inLeaf = super.getLeafCollector(context);

        // 返回一个自定义的 LeafCollector，在这里拦截 collect 方法
        return new FilterLeafCollector(inLeaf) {

            // 1. 声明 Lucene 6.x 的 Scorer 对象
            private Scorer scorer;

            @Override
            public void setScorer(Scorer scorer) throws IOException {
                this.scorer = scorer;
                // 必须调用父类方法，确保内部 Collector 也能拿到 Scorer
                super.setScorer(scorer);
            }

            @Override
            public void collect(int doc) throws IOException {
                // 2. 通过保存的 Scorer 获取当前文档得分
                float score = scorer.score();
                log.debug("score: {} threshold:{} " ,score, threshold);
                // 只有得分大于等于阈值的文档，才会被真正收集
                if (score >= threshold) {
                    super.collect(doc);
                }
                // 低于阈值的文档在这里被直接丢弃
            }
        };
    }
}
