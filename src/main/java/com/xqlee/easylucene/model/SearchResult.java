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

package com.xqlee.easylucene.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 搜索结果
 */
@Data
public class SearchResult {

    /**
     * 总数
     */
    int total;
    /***
     * 当前页码
     */
    int currentPage = 1;
    /**
     * 分页大小
     */
    int pageSize = 10;
    /**
     * 总页数
     */
    int pages;
    /**
     * 记录
     */
    List<Map<String,String>> rows;



    public void setTotal(int total){
        this.total = total;
        int pages = 0;
        if (total>0 && pageSize >0){
            pages= total%pageSize == 0 ? total/pageSize:total/pageSize+1;
        }
        setPages(pages);
    }

    public void setPages(int pages){
       this.pages = pages;
    }

}
