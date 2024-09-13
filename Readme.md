## 1.目标

1. 设计并模拟实现一个数据库存储引擎中的 B+树索引算法。
   - 1）设计数据页和索引页的结构，每个数据页和索引页都以文件的形式存储于外存。设计的结构要便于对数据记录的插入，删除和查找。
   - 索引页和数据页的文件名可以用顺序数字表示，方便 IO 算法实现。
   - 每次操作时需要将完整的文件页读入内存，因此也要设计相应的内存操作数据结构和IO 操作函数。
   - 文件中记录位置指针可以通过记录在文件中的偏移位置来标识，比如，每个记录长度为 16 字节，当前文件中第 4 条记录的位置就是当前文件的第 48-63 字节。
2. 索引页和数据页的文件大小均为 16KB，内存中最多可以读取 4 个页，也就是实现的数据库存储引擎的缓存空间为 64KB。
3. 自己设计记录格式，包括 2 个属性（A 和 B），其中 A 为主键，B 作为记录；数据类型不限。每条记录固定长度为 64B。随机生成足够数量的记录来支持你的功能演示。
4. 用自己熟悉的高级语言实现 B+树索引算法，能够进行索引的插入，删除和记录的查找操作。
5. 能够从理论对算法的 IO 效率进行分析，如查找操作最多需要多少次磁盘 IO 等。

## 2.实现方法

### 2.1 数据结构设计

1. **数据页和索引页外存文件设计**

   对于两者，我均采用`.xlsx`格式的文件进行存储。

   - 对于数据页，其有两列，第一列为主键(A)，第二列为记录(B)。其中主键采用从1开始递增的方式进行设置，记录为随机生成的字符串，两者总字符长度为64，也就是一条记录为64B，每个Excel文件存储256条记录，总的一个Excel文件大小为16KB。为了保证有足够数量的记录来支持功能演示，我随机生成了10万条数据。

   - 对于索引页，其含有散列，第一列为索引名称、第二列为下一个文件的指针（地址）、第三列为其索引在树中的高度（设置此列是为了便于加载到内存之后还原B+树）

   数据页和索引页具体形式如下：

   数据页：

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240507194255464.png" alt="image-20240507194255464" style="zoom: 50%;" />

   索引页：

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240507194343451.png" alt="image-20240507194343451" style="zoom: 50%;" />

2. **数据页和内存页类设计**

   我采用Java进行开发，对于数据页和内存页均设计相应的类进行映射，具体的各个类以及各类的字段如下：

   - `Entry`类：数据页对应的类。字段有`String primaryKey`以及`String record`。

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240507195037982.png" alt="image-20240507195037982" style="zoom: 49%;" />

   - `Index`类：索引页对应的类。字段有`String key`、`String value`以及`String height`对应上面索引页的三列。

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240507195057920.png" alt="image-20240507195057920" style="zoom: 57%;" />

3. **缓存数据结构设计**

   对于缓存，我采用了`LRU (least recently  used)`最近最久未使用缓存。根据使用时间来判定对象是否被持续缓存，当对象被访问时放入缓存，当缓存满了，最久未被使用的对象将被移除。此缓存基于`LinkedHashMap`，因此当被缓存的对象每被访问一次，这个对象的key就到链表头部。优点是简单并且非常快，缺点是当缓存满时，不能被很快的访问。

   对于`Cache`，存放的为一个个键值对，其中`Value`为一个`List`集合，`List`集合里面存储的是一条条数据或者是索引记录，可以看做是一个数据页或者是索引页。

   ```java
   Cache<String, List> cache = CacheUtil.newLRUCache(4);
   //CacheUtil中具体采用的数据结构为：
   LinkedHashMap<Mutable<K>, CacheObj<K, V>> linkedHashMap = new LinkedHashMap(capacity);
   ```

4. **B+数节点数据结构设计**

   对于B+数的节点，主要分为叶子节点和非叶子节点，两者有相同之处，也有不同之处。因此我创建了抽象类`BTreeNode`以及实现了抽象类的`BTreeLeafNode`以及`BTreeInnerNode`类来分别表示叶子结点以及非叶子结点。将两者都具有的属性以及方法存放在抽象类。具体字段如下：
   
   - 抽象结点类`BTreeNode`:
   
     结点编号`Number`、结点的`Keys`、`Key`的个数`KeyCount`、父节点`ParentNode`、左兄弟`LeftSibling`、右兄弟`RightSibling`。
   
   - 叶子结点类`BTreeLeafNode`：
   
     每个主键对应的具体的数据。
   
   - 非叶子结点类`BTreeInnerNode`：
   
     每个Key对应的孩子引用(指针)、每个结点的键值对的个数。
   
   另外，每个节点的`Keys`为一个存放`String`类型的数组，`Value`为一个存放`Object`类型的数组。假设节点最大存放的`Key`和`Value`键值对格式为`Num`，那么对于叶子结点：`Keys`的数组大小为`Num+1`，`Value`的数组大小为`Num+1`。对于非叶子结点，`Keys`的数组大小为`Num+1`，`Value`的数组大小为`Num+2`。
   
   我这么设计的原因是**为了便于检查插入索引后节点是否会溢出，如果插入了一个索引，Key和Value的个数正好达到了数组的最大大小，那么就说明超出了节点能存放的索引的个数。**

### 2.2 缓存、IO算法设计

对于文件的读写，我才用了由`Alibaba`提供的`EasyExcel`工具进行快速从磁盘读取、写入`Excel`文件到磁盘。其底层采用了`NIO`技术来实现，首先先将文件读入到缓冲区（用来存储IO操作的数据的一段连续区域）通过通道进行读写操作。

1. **从缓存中获取页**

   基本思路：向此函数传入页的编号，程序会首先检查缓存中是否存在此缓存，如果存在，直接返回。如果不存在，那么就会从文件中读取此页，并加载缓存中去。

   ```java
       /**
        * 从Cache中获取数据，如果不存在的话，那么就从磁盘读取
        * @param key 文件的数字编号
        * @param clazz 文件要映射的类
        * @return
        */
       public static List getFromCache(String key,Class clazz){
           String indexFilePath = System.getProperty("user.dir")+"/index/"+key+".xlsx";
           if(cache.containsKey(key)){
               return cache.get(key);
           }else{
               if(FileUtil.exist(indexFilePath)){
                   List objects = EasyExcel.read(indexFilePath, clazz, new PageReadListener<>(a -> {
                   })).sheet().doReadSync();
                   cache.put(key,objects);
               }else{
                   return null;
               }
           }
           return cache.get(key);
       }
   ```

2. **缓存淘汰策略**

   实现思路：由于我的缓存采用的是`LinkedHashMap`，其不仅具有`HashMap`的特性，而且还保持了访问顺序的特性，如果缓存满了，那么我们只需要获得最早访问的元素，并把它淘汰即可。通过Java提供的集合类，我们可以很简单的来实现LRU淘汰策略的缓存

   ```java
   public class LRUCache<K, V> extends LinkedHashMap<K, V> {
       //缓存的容量
       private final int capacity;
       public LRUCache(int capacity) {
           //第三个参数为accessOrder,设置为true，就会按照访问顺序维护顺序
           super(capacity, 1f, true);
           this.capacity = capacity;
       }
   
       //当缓存项数量超过容量时返回 true，表示需要移除最老的缓存项。
       @Override
       protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
           return size() > capacity;
       }
   }
   ```

3. **将缓存中的页写入到外存** 

   基本思路：读入内存的页数据会被映射为相应的类，如果要写回到磁盘，我们只需要再将类映射为对应的Excel文件即可：

   ```java
   	String fileName = "index/" + temp.getNumber() + ".xlsx";
   	EasyExcel.write(fileName, Index.class).sheet("1").doWrite(getInnerData(temp));
       /**
        * 将多条数据转换为大小为16KB的页
        * @param node
        * @return
        */
       private static List<Index> getInnerData(BTreeInnerNode node) {
           List<Index> list = ListUtils.newArrayList();
           //一条数据64B，大小为16KB，则一个数据页可以有256条记录
           int keyCount = node.getKeyCount();
           for (int i = 0; i < keyCount; i++) {
               Index data = new Index();
               Integer key = node.getKey(i);
               data.setHeight(node.getHeight());
               data.setKey(key);
               data.setValue(String.valueOf(node.getChild(i).getNumber()));
               list.add(data);
           }
           return list;
       }
   ```

### 2.3 B+树操作算法设计

1. **插入索引**

   实现思路：

   - 首先，先查找到该索引应该存放的索引页（叶子节点），然后向该索引页中插入该索引。
   - 检查该索引页的索引个数是否溢出。
   - 如果溢出了，那么需要将该索引页分裂为两个索引页，建立两个索引页以及和原来父索引页的关系，并将该索引页中中间的那个索引的名称（Key）上提到其父索引页。
   - 如果没有溢出，那么无需进行别的操作，插入索引后结束即可。

2. **删除索引**

   实现思路：

   - 删除索引需要传入一个索引的Key值，首先，会根据该Key值查找到该索引存放的索引页，然后从索引页中删除该索引记录。
   - 检查该索引页的使用率是否达标（索引记录数是否达到最大记录数的50%）
   - 如果删除后使用率仍大于等于50%，那么就无需别的操作。
   - 如果删除后使用率不达标，那么就需要从兄弟借索引记录或者合并操作。
     - 首先会检查左兄弟是否可以借出记录（左兄弟记录数减少一之后的使用率是否大于50%），如果可以借出的话，那么需要把**左兄弟的最右记录添加到该节点的开头**。
     - 如果左兄弟不可以借的话，那么再去检查右兄弟是否可以借出记录。如果可以的话，那么把**右兄弟第一个记录转移到该节点的最右侧。**
     - 如果左右兄弟都不可以借出记录的话，那么再尝试和左右节点进行合并。**合并之后，首先需要从父节点中删除两个子节点中间的Key，并且删除之后还需要检查父节点使用率是否达到了50%，如果没有达到还需重复上述操作。**

3. **查找记录**

   实现思路：

   - 我的**索引文件名称是按照从数字1开始按顺序存储的**，并且每个索引文件的各个记录的Key均为递增。首先从一号文件进行查询，如果当前要查找的记录的Key小于索引记录的Key，那么获取到其对应的Value（下一个索引文件的名称）。
   - 根据Value去下一个索引文件进行查找，重复第一条步骤，**直到查询到的Value是以数字0开头的（表明指向的是数据页）**，那么就根据此Value去查找对应的数据页。
   - 遍历数据页中的数据，直到查找到相同的Key所对应的记录值。

## 3.算法验证方法

1. **外存文件的构造**

2. **记录的构造**

   我通过编写Java程序的方式来随机生成数据，使用到的具体的函数如下：

   ```java
       /**
        * 生成数据,一个数据页大小为16KB，一条数据64B，也就是一个数据页有256条数据
        * @param pageNum 数据页的个数
        */
       private static void genEntry(int pageNum){
           //1.清除原来的文件
           FileUtil.clean(System.getProperty("user.dir")+"/index/");
           FileUtil.clean(System.getProperty("user.dir")+"/entry/");
           //2.生成新的文件
           ExcelUtil excelUtil = new ExcelUtil();
           for(int i = 1 ; i <= pageNum ; i++){
               excelUtil.genRandomEntry();
           }
       }
       public void genRandomEntry() {
           EasyExcel.write("entry/0"+(currentPrimaryKey+1)+".xlsx", Entry.class)
                   .sheet("1")
                   .doWrite(() -> {
                       List<Entry> list = ListUtils.newArrayList();
          				 //一条数据64B，大小为16KB，则一个数据页可以有256条记录
           			for (int i = 0; i < 256; i++) {
              			 	Entry data = new Entry();
               			data.setRecord(generateRecord());
               			data.setPrimaryKey(generatePrimaryKey());
               			list.add(data);
           			}
           		return list;
                   });
       }
   ```

   **验证思路**：通过编写对应的`Juint`测试类，来对构造记录函数进行测试

   **验证结果**：可以看到，每个Excel文件的记录数为256，而且每一条记录的总字符数为64B。主键也是递增的。

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508110339509.png" alt="image-20240508110339509" style="zoom: 67%;" />

3. **删除算法的正确性、IO效率验证**

   **验证思路**：我通过借助查找算法来对删除算法进行正确性验证。

   - 删除某个索引之前，通过该索引查找该索引对应的记录，**以及该索引周围的记录**。
   - 删除该索引，再次通过该索引查找对应的记录以及周围的记录，**正确情况下，该索引查找不到，但是周围的记录可以查找到**。

   **验证结果：**

   - 查找索引100对应的记录

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508111444248.png" alt="image-20240508111444248" style="zoom: 67%;" />

     可以看到，索引页中也存在该索引记录：

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508111540832.png" alt="image-20240508111540832" style="zoom:80%;" />

   - 删除索引值为100的索引之后，再次查找该索引对应的记录，未查询到100索引对应的记录

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508111717830.png" alt="image-20240508111717830" style="zoom: 67%;" />

     可以看到，索引100不存在了，

     <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508111739964.png" alt="image-20240508111739964" style="zoom:80%;" />

   **IO效率验证：**由于我建立的B+树高度为3，对于删除的索引操作，IO的次数不是很固定，但通过我的测试，大致可以确定其IO次数在1~5次（忽略查询该页所导致的IO次数）。针对各种情况，我进行了以下分析：

   - 如果叶子节点删除了索引之后，利用率扔大于50%，那么仅仅只会涉及一次IO操作
   - 如果该叶子节点需要从左右节点借索引，并且将父亲节点的记录删除之后父节点的利用率大于50%，那么会需要3次IO操作
   - 在上述步骤的基础上，如果父亲节点的记录删除之后父节点的利用率小于50%，那么父亲节点让需要从兄弟节点借索引记录或者合并，此情况一共需要5次IO操作。

4. **查找算法的正确性、IO效率验证**

   **验证思路：**查询边界索引对应的记录以及随机的多个索引对应的记录。

   **验证结果：**可以看到，对于抽取查询的各个索引值，都查询到了对应的记录值。

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508112417788.png" alt="image-20240508112417788" style="zoom: 67%;" />

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508112434846.png" alt="image-20240508112434846" style="zoom: 67%;" />

   <img src="https://inkblogbucket.oss-cn-beijing.aliyuncs.com/image-20240508112449524.png" alt="image-20240508112449524" style="zoom: 67%;" />

   **IO效率：**对于这些记录，我建立了一棵高度为3的B+树，可以看到，对于任何记录，IO次数均为4：前三次为读取3索引页，最后一次为读取相应的数据页到内存，读取数据。
   
   **由此不难得出，对于一棵高度为H的B+索引树，查找对应的数据需要进行H+1次IO（B+树叶子节点存储的是数据页的指针）或者H次IO（B+树叶子节点存储的是数据）**
