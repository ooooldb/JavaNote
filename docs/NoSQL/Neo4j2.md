## 使用

### 安装

#### Windows

地址：

官网

https://neo4j.com/download/

第三方

http://we-yun.com/index.php/blog/releases-56.html

解压

cd bin

(打开PowerShell)

./neo4j.bat console 启动

访问地址

http://localhost:7474/browser/

## CQL

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
| SET 组           | 添加或更新标签               |

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

下面来了解以下neo4j的CRUD

#### CREATE

##### 用途

- 创建没有属性的节点
- 使用属性创建节点
- 在没有属性的节点之间创建关系
- 使用属性创建节点之间的关系
- 为节点或关系创建单个或多个标签

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

| header 1                            | header 2                                                     |
| ----------------------------------- | ------------------------------------------------------------ |
| 语法元素                            | 描述                                                         |
| <node-name>                         | 它是我们将要创建的节点名称。只是存储在库中，不能用作查询（类似临时变量）。使用相同的node-name也不影响数据库构建，不写也行。 |
| <label-name>                        | 它是一个节点标签名称，用来查询。                             |
| <Property1-name>...<Propertyn-name> | 属性是键值对。 定义将分配给创建节点的属性的名称              |

`eg:`

创建带有属性的节点

```
create(n:学生{age:11,name:'小红'})
```

这里的n类似sql中的别名，仅在语句中指代新增(查询)的节点。

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

##### where条件

基本与SQL一致

##### 排序 order by子句

```
ORDER BY  <property-name-list>  [DESC]	 
```

eg

```
MATCH (emp:Employee)
RETURN emp.empid,emp.name,emp.salary,emp.deptno
ORDER BY emp.name
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

##### LIMIT和SKIP子句

LIMIT子句用来过滤或限制查询返回的行数
SKIP子句用来修整CQL查询结果集顶部的结果

语法

```
LIMIT <number>

SKIP <number>
```

eg

```
MATCH (emp:Employee) 
RETURN emp
LIMIT 2

MATCH (emp:Employee) 
RETURN emp
SKIP 2
```

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

相似性

- 这两个命令不应单独使用。两个命令都应该与MATCH命令一起使用。

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

当指定节点或关系不存在时，创建它.存在不会创建

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

## 连接Java使用

### 原生

1. 导入依赖



### SpringBoot Starter

## 原理

## 高级技巧