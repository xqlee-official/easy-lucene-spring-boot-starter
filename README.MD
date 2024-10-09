# Easy lucene spring boot starter

## Description
The component library can easily use apache lucene library to quickly realize the full-text search function of articles. Full text search is stored on disk.

## Usage

### maven pom
````xml
        <dependency>
            <groupId>com.xqlee.boot</groupId>
            <artifactId>easy-lucene-spring-boot-starter</artifactId>
            <version>3.0.0</version>
        </dependency>
````
`The latest version number is recommended`

### Configuration
You can configure the following parameters in spring boot `application.yml`
````yaml
spring:
    lucene:
      store_path: your_store_path
      max_threads: 2 # Maximum number of threads for task processing
````

### Interface description

#### Full text search storage service
`com.xqlee.easylucene.service.EasyLuceneWriteService`

usage：

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
##### Write multiple document contents
`int writeIndex(List<IndexDoc> documents, String path) throws IOException;`
- documents Document content (for example, news articles, in this case, multiple news articles)
- path 存储相对 Store relative paths (such as news, news/ report-report, one path for each type of business, or aggregate according to business).

##### Update the individual document content
`void update(List<IndexField> indexFields, String idFieldName, String pathName) throws IOException;`

- `indexFields` Document fields (such as news title, content, introduction, and many other fields that require full-text search)
- `path` Document storage path
##### Delete a single document
`void delete(String idFieldName, String idFieldValue, String pathName) throws IOException;`

Delete a single document based on its id field path

Note: This deletion method is not real-time and does not merge immediately
##### Delete individual documents and merge immediately
`void deleteAndMerge(String idFieldName, String idFieldValue, String pathName) throws IOException;`

Delete a single document based on its id field path

Note: This method is executed in real time and merged into the document immediately. This operation consumes I/O resources. You are advised to perform this operation in idle time

##### Delete all documents based on the path
`void deleteAll(String pathname)throws IOException;`

Delete all documents under the specified path `pathname`.

#### Full text search query service
`com.xqlee.easylucene.service.EasyLuceneQueryService`

##### Query based on a single field of a document
`SearchResult queryOneField(int currentPage, int pageSize, SearchField searchField ,
String pathname);`

- `currentPage` Current page number, starting from 1;
- `pageSize` Page size
- `searchField` Queried field
- `pathname` Query document path
- `SearchResult` The returned object contains the query result

##### Query based on multiple fields of the document
`SearchResult queryMultiField(int currentPage, int pageSize, List<SearchField> searchFields, String pathname);`

`SearchResult queryMultiField(int currentPage,int pageSize,String pathname,SearchField ...searchFields);`

- `currentPage` Current page number, starting from 1;
- `pageSize` Page size
- `searchFields` The field collection to be queried
- `pathname` Query document path
- `SearchResult` The returned object contains the query result

##### Query according to the lucene Query container
`SearchResult query(int currentPage, int pageSize, Query query, List<String> highlightFieldNames, String pathname);`

- `currentPage` Current page number, starting from 1;
- `pageSize` Page size
- `query` Query container
- `highlightFieldNames` Highlighted field
- `pathname` Query document path
- `SearchResult` The returned object contains the query result

Note: Using the Query container to query keywords requires special symbolic handling

`String keywords = QueryParser.escape(keywords)`

## spring boot versions
- spring boox 2.x.x -> com.xqlee.boot:easy-lucene-spring-boot-starter:1.x.x
- spring boox 3.x.x -> com.xqlee.boot:easy-lucene-spring-boot-starter:3.x.x

## LICENSE
Apache Software License, Version 2.0