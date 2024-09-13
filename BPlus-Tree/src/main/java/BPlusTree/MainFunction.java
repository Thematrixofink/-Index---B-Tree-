package BPlusTree;

import BPlusTree.node.BTreeInnerNode;
import BPlusTree.node.BTreeLeafNode;
import BPlusTree.node.BTreeNode;
import BPlusTree.node.TreeNodeType;
import Cache.EntryCacheManager;
import ExcelUtils.model.Entry;
import ExcelUtils.ExcelUtil;
import ExcelUtils.model.Index;
import ExcelUtils.model.LastLevelIndex;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class MainFunction {

    //B+树
    private static BTree bTree = new BTree();

    //缓存最大的页数
    private static final Integer CACHE_PAGE_SIZE = 4;

    //记录的页数
    private static final Integer ENTRY_PAGE_NUM = 400;

    public static void main(String[] args) throws IOException {
        //MainFunction.genEntry(ENTRY_PAGE_NUM);
        while (true) {
            System.out.println("");
            System.out.println("1.建立索引");
            System.out.println("2.添加索引");
            System.out.println("3.删除索引");
            System.out.println("4.搜索记录");
            System.out.println("5.导入索引");
            System.out.println("6.清除并生成新数据");
            System.out.println("");
            Scanner scanner = new Scanner(System.in);
            String s = scanner.nextLine();
            switch (s){
                case "1" :
                    genBPlusTree();
                    writeBPlusTree();
                    System.out.println("建立索引并持久化成功!");
                    break;
                case "2" :
                    System.out.println("请输入key和value");
                    String key = scanner.nextLine();
                    String value = scanner.nextLine();
                    bTree.insert(Integer.parseInt(key),value);
                    writeBPlusTree();
                    EntryCacheManager.clearCache();
                    break;
                case "3" :
                    System.out.println("请输入要删除的key");
                    String key1 = scanner.nextLine();
                    bTree.delete(Integer.parseInt(key1));
                    writeBPlusTree();
                    EntryCacheManager.clearCache();
                    break;
                case "4" :
                    System.out.println("请输入要查找的记录的key");
                    String key2 = scanner.nextLine();
                    bTree.searchEntry(Integer.parseInt(key2));
                    break;
                case "5" :
                    bTree.genTreeFromFile();
                    break;
                case "6" :
                    genEntry(ENTRY_PAGE_NUM);
                    break;
            }
        }
    }

    private static void genBPlusTree(){
        //下一个要读的数据页
        int curPageNum = 1;
        while(curPageNum <= ENTRY_PAGE_NUM) {
            //一次读取(最多)四块到缓冲区
            for (int i = 1 ; i <= CACHE_PAGE_SIZE && curPageNum <= ENTRY_PAGE_NUM ; i++) {
                int pageNum = curPageNum*256-255;
                //Entry只会读取数据的主键,value为其页号
                List<Entry> entries = readEntry(pageNum);
                EntryCacheManager.cache.put(String.valueOf(pageNum),entries);
                for (Entry entry : entries) {
                    //最底层索引存储 主键 + 0(pageNum)
                    bTree.insert(entry.getPrimaryKey(),"0"+String.valueOf(pageNum));
                }
                curPageNum++;
            }
        }
    }


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


    /**
     * 向缓冲区中读取一个数据页
     * @param pageNum 数据页的编号
     */
    private static List<Entry> readEntry(int pageNum){
        ExcelUtil excelUtil = new ExcelUtil();
        List<Entry> entries = excelUtil.readEntry("entry/0" + pageNum + ".xlsx");
        return entries;
    }


    /**
     * 将索引持久化到磁盘
     */
    private static void writeBPlusTree(){
        bTree.setNodeNumber();
        BTreeNode node = bTree.getRoot();
        if(node == null || node.getKeyCount() == 0){
            throw new RuntimeException("B+树为空!");
        }
        //如果不是叶子节点就一直往下找
        while (node.getNodeType() == TreeNodeType.InnerNode) {
            BTreeInnerNode temp = (BTreeInnerNode) node;
            while(temp != null){
                String fileName = "index/" + temp.getNumber() + ".xlsx";
                EasyExcel.write(fileName, Index.class).sheet("1").doWrite(getInnerData(temp));
                temp = (BTreeInnerNode) temp.getRightNode();
            }
            node = ((BTreeInnerNode) node).getChild(0);
        }

        //如果是叶子节点
        if(node.getNodeType() == TreeNodeType.LeafNode){
            BTreeLeafNode temp = (BTreeLeafNode) node;
            while(temp != null){
                String fileName = "index/" + temp.getNumber() + ".xlsx";
                EasyExcel.write(fileName, LastLevelIndex.class).sheet("1").doWrite(getLeafData(temp));
                temp = (BTreeLeafNode) temp.getRightNode();
            }
        }
    }


    /**
     * 获取非叶子节点的数据
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
        Index index = new Index();
        index.setHeight(node.getHeight());
        index.setValue(String.valueOf(node.getChild(keyCount).getNumber()));
        list.add(index);
        return list;
    }


    /**
     * 获取叶子节点的数据
     * @param node
     * @return
     */
    private static List<LastLevelIndex> getLeafData(BTreeLeafNode node) {
        List<LastLevelIndex> list = ListUtils.newArrayList();
        //一条数据64B，大小为16KB，则一个数据页可以有256条记录
        int keyCount = node.getKeyCount();
        for (int i = 0; i < keyCount; i++) {
            LastLevelIndex data = new LastLevelIndex();
            Integer key = node.getKey(i);
            data.setKey(key);
            data.setValue(node.getValue(i));
            data.setHeight(0);
            list.add(data);
        }
        return list;
    }



}
