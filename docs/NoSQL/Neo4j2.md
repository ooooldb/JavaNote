## 使用

### 安装

#### Windows
1.下载

地址：

`官网`

https://neo4j.com/download/

`第三方`

http://we-yun.com/index.php/blog/releases-56.html

2.解压

3.启动

cd bin

(打开PowerShell)

./neo4j.bat console 启动

4.测试

访问地址

http://localhost:7474/browser/

## CQL
全称（Cypher Query Language）

- 它是Neo4j图形数据库的查询语言。
- 它是一种声明性模式匹配语言

### CQL命令

| CQL命令          | 用法                         |
| ---------------- | ---------------------------- |
| CREATE 创建      | 创建节点，关系和属性         |
| MATCH 匹配       | 检索有关节点，关系和属性数据 |
| RETURN 返回      | 返回查询结果                 |
| WHERE 哪里       | 提供条件过滤检索数据         |
| DELETE 删除      | 删除节点和关系               |
| REMOVE 移除      | 删除节点和关系的属性         |
| ORDER BY 以…排序 | 排序检索数据                 |
| SET 组           | 添加或更新属性               |

### CQL函数

| CQL函数           | 用法                                             |
| ----------------- | ------------------------------------------------ |
| String 字符串     | 它们用于使用String字面量。                       |
| Aggregation 聚合  | 它们用于对CQL查询结果执行一些聚合操作。          |
| Relationship 关系 | 他们用于获取关系的细节，如startnode，endnode等。 |

### CQL数据类型

| CQL数据类型 | 用法                            |
| ----------- | ------------------------------- |
| boolean     | 用于表示布尔文字：true，false。 |
| byte        | 用于表示8位整数。               |
| short       | 用于表示16位整数。              |
| int         | 用于表示32位整数。              |
| long        | 用于表示64位整数。              |
| float       | 用于表示32位浮点数              |
| double      | 用于表示64位浮点数              |
| char        | 用于表示16位字符。              |
| String      | 用于表示字符串。                |

下面来了解一下neo4j的CRUD

#### CREATE

##### 用途

- 创建节点
- 创建关系

create总是在执行创建，同一条语句执行两次会创建两个属性一样的节点

##### 语法

```
CREATE (
   <node-name>:<label-name>
   { 	
      <Property1-name>:<Property1-Value>,
      ........
      <Propertyn-name>:<Propertyn-Value>
   }
)
```

`eg:`

创建带有属性的节点

```
create(n:学生{age:11,name:'小红'})
```

这里的n类似sql中的别名，仅在语句中指代新增或查询的节点。

单个标签到关系

```
CREATE (<node1-name>:<label1-name>)-
	[(<relationship-name>:<relationship-label-name>)]
	->(<node2-name>:<label2-name>)
```

`eg:`

创建带关系的新节点(关系必须带有方向)

```
create (a:Student{name:"肥妞",weight:200})-[r:class{className:"母猪管理"}]->(b:teacher{name:"oooblack"})
```

创建带双向关系的新节点

```
create (a:Student{name:"肥妞",weight:200})-[r:class{className:"母猪管理"}]->(b:teacher{name:"oooblack"})
create (b)->[r1:class{className:"母猪管理"}]->(a)
```

为已存在节点增加关系（使用match匹配节点，然后增加关系)

```
match (a),(b) where id(a) = 20 and id(b) = 43 create (a)-[r:testRelation]->(b) return a,b
```

增加双向关系

```
match (a),(b) where id(a) = 20 and id(b) = 43 create (a)-[r:testRelation]->(b)
create (b)-[r1:testRelation]->(a) return a,b
```

`ps`

```
neo4j会给新增的节点生成id,id(a)是获取这个id的,与a.id不同，a.id是我们新增时指定的一个属性
```

#### Read(Select)

在neo4j中，使用match关键字提供检索功能

eg:

检索所有节点

```
match (n) return n
```

检索所有节点及关系

```
match (a)-[r]-(b) return a,r,b
```

检索指定标签下指定属性的节点，并对这些节点进行排序和分页。

```
match (a:Student) where a.name = '胖妞' return a order by id(a) desc skip 0 limit 1
```

##### union子句

- UNION 对结果去重
- UNION ALL 对结果不去重
  语法

```
<MATCH Command1>
   UNION
<MATCH Command2>
```

eg

```
MATCH (cc:CreditCard) RETURN cc.id,cc.number
UNION (all)
MATCH (dc:DebitCard) RETURN dc.id,dc.number
```

union all格式一致

##### WITH 
对结果进行过滤
```
MATCH (n {name: 'John'})-[:FRIEND]-(friend)
WITH n, count(friend) AS friendsCount
WHERE friendsCount > 3
RETURN n, friendsCount
```
##### OPTION MATCH
作用相当于SQL里的 Left Join

# Delete条件

##### 作用

- 删除节点
- 删除节点及相关节点和关系

##### 语法

```
DELETE <node-name-list>
```

用逗号（，）运算符来分隔节点名

eg

`删除标签Employee下所有节点`

```
MATCH (e: Employee) DELETE e
```

`删除CreditCard和Customer具有连接关系的节点和关系`

```
MATCH (cc: CreditCard)-[rel]-(c:Customer) 
DELETE cc,c,rel
```

# remove子句

用于

- 删除节点或关系的标签
- 删除节点或关系的属性

和DELETE的区别：

- Delete用于删除节点和关联关系。
- Remove用于删除标签和属性

```
MATCH (book { id:122 })
REMOVE book.price
RETURN book
```

##### 删除节点/关系的标签

REMOVE一个Label子句语法：

```
REMOVE <label-name-list>
```

# SET子句

##### 作用

- 向现有节点或关系添加新属性
- 添加或更新属性值

##### 语法

```
SET  <property-name-list>
```

eg:

```
MATCH (dc:DebitCard)
SET dc.atm_pin = 3456
RETURN dc
```

# Merge

##### 用途

当指定节点或关系不存在时，创建它；存在则不会创建

##### 语法

```
MERGE (<node-name>:<label-name>
{
   <Property1-name>:<Pro<rty1-Value>
   .....
   <Propertyn-name>:<Propertyn-Value>
})
```

eg

```
MERGE (gp2:GoogleProfile2{ Id: 201402,Name:"Nokia"})
```

`最短路径`
```
MATCH (p1),(p2),p=shortestpath((p1)-[*0..3]-(p2))
where p1.name='kunlin' and p2.xm = '杰伦'
RETURN p,p1
```

`指定层级内的节点`
```
match (a:People) where id(a) = 1992 match data=(a)-[*0..3]->(nb) return data
```

`索引`
```
创建索引
CREATE INDEX ON :Person(name)
删除索引
DROP INDEX ON :Person(name)
查询已创建的索引
:schema
```

`更改节点的Label（使用APOC）`
```
MATCH (n) where id(n) = 540089
WITH n, [k in labels(n)] as labels
CALL apoc.create.removeLabels(n,labels) YIELD node
set n:这是一首简单的小情歌
RETURN count(*)
```

`更改节点的Label（不使用APOC）`
```
MATCH (n:OLD_LABEL)
REMOVE n:OLD_LABEL
SET n:NEW_LABEL
```
## 函数
### Predicate functions
断定性函数，即返回true or false的函数。

常用于Where查询部分中

Function | Description
---|---
exists() | 如果指定的模式存在于图中，或者特定的属性存在于节点、关系或Map中，那么函数返回True
all() | 测试是否list中所有元素均满足条件
any() | 测试是否list中包含元素满足条件
none() | 仅当list中元素均不满足条件返回true
single() | 如果list中元素恰好只有一个满足条件，返回true

`all`
```
MATCH (n)
WHERE exists(n.name)
RETURN n.name AS name, exists((n)-[:MARRIED]->()) AS is_married
```
`any()`
```
MATCH (a)
WHERE a.name = 'Eskil' AND ANY (x IN a.array WHERE x = 'one')
RETURN a.name, a.array
```
`exists()`
```
MATCH (n)
WHERE EXISTS (n.name)
RETURN n.name AS name, EXISTS ((n)-[:MARRIED]->()) AS is_married
```
`none()`
```
MATCH p =(n)-[*1..3]->(b)
WHERE n.name = 'Alice' AND NONE (x IN nodes(p) WHERE x.age = 25)
RETURN p
```
`single()`
```
MATCH p =(n)-->(b)
WHERE n.name = 'Alice' AND SINGLE (var IN nodes(p) WHERE var.eyes = 'blue')
RETURN p
```
### Scalar functions
1.获取节点和关系的ID和属性
- id()
- properties()
```
CREATE (p:Person { name: 'Stefan', city: 'Berlin' })
RETURN id(p), properties(p)
```
2.关系
- endNode()：返回关系的结束节点
- startNode()：返回关系的开始节点
- type()：返回关系的类型
```
MATCH (n)-[r]->()
WHERE n.name = 'Alice'
RETURN type(r), startNode(r), endNode(r)
```
3.列表相关
列表是元素的有序序列，Cypher使用List来表示列表类型，应用于列表的函数有：
- coalesce()：返回列表中第一个非NULL的元素
- head()：返回列表中的第一个元素
- last()：返回列表中的最有一个元素
- size()：返回列表中元素的数量
```
MATCH (a)
WHERE a.name = 'Eskil'
RETURN a.array, head(a.array), last(a.array), size(a.array)
```
4.size()和length()函数
求长度的函数：
- size(string)：表示字符串中字符的数量，可以把字符串当作是字符的列表。
- size(list)：返回列表中元素的数量。
- size(pattern_expression)：也是统计列表中元素的数量，但是不是直接传入列表，而是提供模式表达式（pattern_expression），用于在匹配查询（Match query）中提供一组新的结果，这些结果是路径列表，size()函数用于统计路径列表中元素（即路径）的数量。
- length(path)：返回路径的长度，即路径中关系的数量
例如，统计路径列表中的元素数量：
```
MATCH (a)
WHERE a.name = 'Alice'
RETURN size((a)-->()-->()) AS fof
```
5.工具函数
- randomUUID()
- timestamp()
- toBoolean()
- toFloat()
- toInteger()
### Aggregating functions
聚合函数用于对查询的结果进行统计：
- avg()：计算均值
- count(exp)：用于计算非null值（value）的数量，使用 count(distinct exp)进行无重复非null值的计数，使用count(*)：计算值或记录的总数量，包括null值
- max(),min()：求最大值和最小值，在计算极值时，null被排除在外，min(null)或max(null)返回null
- sum()：求和，在求和时，null被排除在外，sum(null)的结果是0
- collect()：把返回的多个值或记录组装成一个列表，collect(null)返回一个空的列表
## APOC
https://neo4j.com/labs/apoc/4.1/
