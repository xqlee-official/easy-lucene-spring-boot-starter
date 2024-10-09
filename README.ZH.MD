# Easy Lucene

## 说明
该组件库能简单使用apache lucene库来快速实现文章的全文检索功能。全文检索存储通过磁盘方式。

## 使用说明

### maven 引入依赖
````xml
        <dependency>
            <groupId>com.xqlee.boot</groupId>
            <artifactId>easy-lucene-spring-boot-starter</artifactId>
            <version>1.0.0</version>
        </dependency>
````
`版本号请用最新的`

### 配置项目
在spring boot application.yml针对下面参数可以进行配置
````yaml
spring:
    lucene:
      store_path: your_store_path
      max_threads: 2 # 最大任务处理的线程数量
````

### 接口说明

#### 核心全文检索存储服务
`com.xqlee.easylucene.service.EasyLuceneWriteService`

使用方法：

```java
import com.xqlee.easylucene.service.EasyLuceneWriteService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class YourClazz {
    @Resource
    EasyLuceneWriteService easyLuceneWriteService;

}
```
##### 写入多个文章内容
`int writeIndex(List<IndexDoc> documents, String path) throws IOException;`
- `documents` 文档内容（例如新闻文章，这里就是多篇新闻文章）
- `path` 存储相对 路径（例如新闻，news/ 报告 - report，每一个类型业务一个存放路径，也可以聚合根据业务来。）

##### 更新单个文章内容
`void update(List<IndexField> indexFields, String idFieldName, String pathName) throws IOException;`

- `indexFields` 文档字段（例如新闻的 标题、内容、简介等多个需要全文检索的字段）
- `path` 存储路径
##### 删除单个文章
`void delete(String idFieldName, String idFieldValue, String pathName) throws IOException;`

根据文档的id字段路径删除单个文档

注意：该删除方法非实时，不会立即融合
##### 删除单个文档并立即融合
`void deleteAndMerge(String idFieldName, String idFieldValue, String pathName) throws IOException;`

根据文档的id字段路径删除单个文档

注意：该删除方法实时执行，立即融合到文档里面，该操作相对比较耗io资源，建议闲时操作

##### 根据路径删除所有文档
`void deleteAll(String pathname)throws IOException;`

指定路径`pathname`下的全部文档删除。

#### 核心全文检索查询服务
`service.com.xqlee.boot.easylucene.LuceneSimpleQueryService`

##### 根据文档单个字段查询
`SearchResult queryOneField(int currentPage, int pageSize, SearchField searchField ,
String pathname);`

- `currentPage` 当前页码，从1开始；
- `pageSize` 分页大小
- `searchField` 查询的字段
- `pathname` 查询文档路径
- `SearchResult` 返回对象包含查询结果

##### 根据文档多个字段查询
`SearchResult queryMultiField(int currentPage, int pageSize, List<SearchField> searchFields, String pathname);`

`SearchResult queryMultiField(int currentPage,int pageSize,String pathname,SearchField ...searchFields);`

- `currentPage` 当前页码，从1开始；
- `pageSize` 分页大小
- `searchFields` 查询的字段集合
- `pathname` 查询文档路径
- `SearchResult` 返回对象包含查询结果

##### 根据lucene Query容器查询
`SearchResult query(int currentPage, int pageSize, Query query, List<String> highlightFieldNames, String pathname);`

- `currentPage` 当前页码，从1开始；
- `pageSize` 分页大小
- `query` 查询容器
- `highlightFieldNames` 高亮字段
- `pathname` 查询文档路径
- `SearchResult` 返回对象包含查询结果

注意：使用Query容器查询关键词需要注意特殊符号处理 

`String keywords = QueryParser.escape(keywords)`

## spring boot 版本对应关系
- spring boox 2.x.x 使用 com.xqlee.boot:easy-lucene-spring-boot-starter:1.x.x 版本
- spring boox 3.x.x 使用 com.xqlee.boot:easy-lucene-spring-boot-starter:3.x.x 版本


## LICENSE
Apache Software License, Version 2.0