package BPlusTree;

import BPlusTree.node.BTreeInnerNode;
import BPlusTree.node.BTreeLeafNode;
import BPlusTree.node.BTreeNode;
import BPlusTree.node.TreeNodeType;
import Cache.ByteCacheManager;
import Cache.EntryCacheManager;
import ExcelUtils.model.Entry;
import ExcelUtils.model.IndexRead;
import cn.hutool.core.io.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * B+ 树
 */
@Slf4j
public class BTree {

	/**
	 * 根节点
	 */
	private BTreeNode root;


	public BTree() {
		//根节点初始化,初始化为叶子节点！！！
		this.root = new BTreeLeafNode();
	}

	public BTreeNode getRoot(){
		return root;
	}

	/**
	 * 根据key查找数据
	 * @param key 数据
	 */
	public String searchEntry(Integer key){
		boolean flag = false;
		if(key <= 0){
			throw new RuntimeException("文件名称(key)名错误!");
		}
		String indexFilePath = System.getProperty("user.dir")+"/index";
		String entryFilePath = System.getProperty("user.dir")+"/entry";

		//1.首先从1号索引文件查找
		String curPageNum = "1";
		String firstIndex = indexFilePath + "/"+curPageNum+".xlsx";
		//如果索引中一直有索引文件，一直向下查询
		while(FileUtil.exist(firstIndex)) {
			//先从缓存中读取页
			List<IndexRead> firstPage = null;
			firstPage = EntryCacheManager.getFromCache(curPageNum, IndexRead.class);
			log.info("读取一个索引页到内存");
			if(firstPage.get(0).getHeight() == 0) {
				//说明是叶子节点了
				for (int i = 0; i < firstPage.size(); i++) {
					IndexRead indexRead = firstPage.get(i);
					Integer numKey = indexRead.getKey();
					if (numKey.compareTo(key) == 0 ) {
						curPageNum = indexRead.getValue();
						//如果找到了
						flag = true;
						break;
					}
				}
				if(!flag) {
					log.error("未通过索引找到该记录!");
					return "";
				}
			}else{
				for (int i = 0; i < firstPage.size(); i++) {
					IndexRead indexRead = firstPage.get(i);
					Integer numKey = indexRead.getKey();
					if (numKey == null) {
						curPageNum = indexRead.getValue();
						break;
					}
					if (key < numKey) {
						curPageNum = indexRead.getValue();
						break;
					}
				}
				firstIndex = indexFilePath + "/" + curPageNum + ".xlsx";
			}
			firstIndex = indexFilePath + "/" + curPageNum + ".xlsx";
			if(!FileUtil.exist(firstIndex)) break;
		}
		String temp = entryFilePath+"/"+ curPageNum +".xlsx";
		if(FileUtil.exist(temp)){
			//先从缓存读取页
			List<Entry> entryPage = null;
			entryPage = EntryCacheManager.getFromCache(curPageNum, Entry.class);
			log.info("读取一个数据页页到内存");
			for (Entry entry : entryPage) {
				if(entry.getPrimaryKey().compareTo(key) == 0){
					System.out.println("key:"+key+" 对应的value:"+entry.getRecord());
					return entry.getRecord();
				}
			}
		}else{
			 throw new RuntimeException("未找到该数据页!");
		}
		return null;
	}

	/**
	 * 	根据索引名称Key查找索引的值Value
	 */
	public String search(Integer key) {
		BTreeLeafNode leaf = this.findLeafNodeShouldContainKey(key);
		int index = leaf.search(key);
		return (index == -1) ? null : leaf.getValue(index);
	}

	/**
	 * 向B+树中插入一个索引
	 */
	public void insert(Integer key, String value) {
		//找到key应该所在的叶子节点
		BTreeLeafNode leaf = this.findLeafNodeShouldContainKey(key);
		//向叶子接节点进行插入
		leaf.insertIndex(key, value);
		//检查插入之后是否溢出了
		if (leaf.isOverflow()) {
			BTreeNode n = leaf.dealOverflow();
			if (n != null)
				this.root = n;
		}
	}

	/**
	 * 从B+树删除一个索引
	 * @param key 索引的名称
	 */
	public void delete(Integer key) {
		BTreeLeafNode leaf = this.findLeafNodeShouldContainKey(key);
		if(leaf.delete(key)){
			//如果删除后使用率没有达标
			if(leaf.isUnderflow()){
				BTreeNode n = leaf.dealUnderflow();
				if (n != null)
					this.root = n;
			}
		}
	}

	/**
	 * 根据索引名称查找key “应该” 所在的叶子节点
	 * @param key 索引名称
	 * @return	  叶子节点
	 */
	private BTreeLeafNode findLeafNodeShouldContainKey(Integer key) {
		BTreeNode node = this.root;
		//如果不是叶子节点就一直往下找
		while (node.getNodeType() == TreeNodeType.InnerNode) {
			//获取孩子节点的下标
			int childIndex = node.search(key);
			node = ((BTreeInnerNode)node).getChild(childIndex);
		}
		return (BTreeLeafNode)node;
	}

	/**
	 * 给树的每个节点进行编号
	 */
	public void setNodeNumber(){
		int height = 0;
		int count = 0;
		this.root.setNumber(count);
		this.root.setHeight(height);
		count++;
		height++;
		BTreeNode node = this.root;
		while(node != null){
			BTreeNode temp = node;
			while(temp != null){
				temp.setNumber(count);
				temp.setHeight(height);
				count++;
				temp = temp.getRightNode();
			}
			if(node.getNodeType() == TreeNodeType.InnerNode){
				node = ((BTreeInnerNode) node).getChild(0);
				height++;
			}else{
				break;
			}
		}
	}

	/**
	 * 从文件中构建B+树,四个四个文件的操作
	 * @return
	 */
	public void genTreeFromFile() throws IOException {
		int leafNodeNum = 0;
		int tempNodeNum = 0;
		int nodeCount = 1;
		int maxHeight = 0;
		System.out.println();
		String indexFilePath = System.getProperty("user.dir")+"/index";
		File[] ls1 = FileUtil.ls(indexFilePath);
		List<File> sortFiles1 = sortFileByName(ls1);
		List<List<IndexRead>> indexPages = new ArrayList<>();
		for (File l : sortFiles1) {
			List<IndexRead> temp = EasyExcel.read(l.getCanonicalPath(), IndexRead.class, new PageReadListener<IndexRead>(dataList -> {
			})).sheet().doReadSync();
			indexPages.add(temp);
			if (temp.get(0).getHeight() > maxHeight) {
				maxHeight = temp.get(0).getHeight();
			}
		}
		//先全部转换为BTreeInnerNode节点
		List<BTreeInnerNode> innerNodes = new ArrayList<>();
		List<BTreeLeafNode> leafNodes = new ArrayList<>();
		for (int i = 0; i < indexPages.size(); i++) {
			BTreeInnerNode temp = new BTreeInnerNode();
			temp.setNumber(nodeCount);
			nodeCount++;
			innerNodes.add(temp);
		}
		//对索引页进行处理
		for (int i = 0; i < indexPages.size(); i++) {
			if(i != indexPages.size() - 1) {
				List<IndexRead> tempNode = indexPages.get(i);
				List<IndexRead> nextNode = indexPages.get(i+1);
				BTreeInnerNode node = innerNodes.get(i);
				int tempHeight = tempNode.get(0).getHeight();
				int nextHeight = nextNode.get(0).getHeight();
				//如果当前节点不是倒数第二层
				if(tempHeight != 0 ) {
					for (int j = 0; j < tempNode.size(); j++) {
						IndexRead indexRead = tempNode.get(j);
						Integer key = indexRead.getKey();
						String value = indexRead.getValue();
						node.setKey(j, key);
						BTreeInnerNode node1 = innerNodes.get(Integer.parseInt(value) - 1);
						node.setChild(j, node1);
						node1.setParent(node);
					}
					//设置当前节点的number以及height
					node.setHeight(tempHeight);
					node.setKeyCount(tempNode.size()-1);
					//设置左以及右兄弟
					if(tempHeight == nextHeight){
						node.setRightSibling(innerNodes.get(i+1));
						innerNodes.get(i+1).setLeftSibling(node);
					}
					tempNodeNum++;
				}else{
					BTreeLeafNode node1 = new BTreeLeafNode();
					node1.setNumber(node.getNumber());
					node1.setParent(node.getParent());
					for (int j = 0; j < tempNode.size(); j++) {
						IndexRead indexRead = tempNode.get(j);
						Integer key = indexRead.getKey();
						String value = indexRead.getValue();
						BTreeInnerNode temp = innerNodes.get(tempNodeNum);
						node1.setKey(j,key);
						node1.setValue(j,value);

					}
					//设置当前节点的number以及height
					node1.setHeight(tempHeight);
					node1.setKeyCount(tempNode.size()-1);
					//设置左以及右兄弟
					if(tempHeight == nextHeight){
						node1.setRightSibling(innerNodes.get(i+1));
						if(leafNodeNum!=0) {
							node1.setLeftSibling(leafNodes.get(leafNodeNum-1));
						}
					}
					leafNodes.add(node1);
					leafNodeNum++;
					tempNodeNum++;
				}

			}else{
				//处理最后一个节点
				List<IndexRead> tempNode = indexPages.get(indexPages.size() - 1);
				BTreeInnerNode node = innerNodes.get(indexPages.size() - 1);
				node.setHeight(maxHeight);
				node.setKeyCount(tempNode.size()-1);
				for (int j = 0; j < tempNode.size(); j++) {
					IndexRead indexRead = tempNode.get(j);
					Integer key = indexRead.getKey();
					String value = indexRead.getValue();
					node.setKey(j, key);
					BTreeInnerNode node1 = innerNodes.get(tempNodeNum);
					node.setChild(j, node1);
					node1.setParent(node);
				}
				tempNodeNum++;
				leafNodeNum++;
			}
		}
		int count = 0;
		for (int i = 0; i < indexPages.size(); i++) {
			if(i != indexPages.size() - 1) {
				List<IndexRead> tempNode = indexPages.get(i);
				List<IndexRead> nextNode = indexPages.get(i + 1);
				BTreeInnerNode node = innerNodes.get(i);
				int tempHeight = tempNode.get(0).getHeight();
				int nextHeight = nextNode.get(0).getHeight();
				//倒数第二层
				if(tempHeight == maxHeight){
					for(int j = 0; j < tempNode.size();j++){
						node.setChild(j,leafNodes.get(count));
						count++;
					}
					}
				}
			}
		this.root = innerNodes.get(0);
	}

	/**
	 * 对文件按照名称进行排序
	 * @param files
	 * @return
	 */
	private List<File> sortFileByName(File[] files){
		List<File> collectFiles= Arrays.stream(files).sorted((o1, o2) -> {
			String name1 = o1.getName();
			String temp1 = name1.substring(0, name1.length() - 5);
			String name2 = o2.getName();
			String temp2 = name2.substring(0, name2.length() - 5);
			return Integer.parseInt(temp1) - Integer.parseInt(temp2);
		}).collect(Collectors.toList());
		return collectFiles;
	}

	private String searchEntryFileName(Integer key){
		if(key <= 0){
			throw new RuntimeException("文件名称(key)名错误!");
		}
		String indexFilePath = System.getProperty("user.dir")+"/index";
		String entryFilePath = System.getProperty("user.dir")+"/entry";

		//1.首先从1号索引文件查找
		String curPageNum = "1";
		String firstIndex = indexFilePath + "/"+curPageNum+".xlsx";
		//如果索引中一直有索引文件，一直向下查询
		while(FileUtil.exist(firstIndex)) {
			//先从缓存中读取页
			List<IndexRead> firstPage = null;
			firstPage = EntryCacheManager.getFromCache(curPageNum, IndexRead.class);
			for (int i = 0; i < firstPage.size(); i++) {
				IndexRead indexRead = firstPage.get(i);
				Integer numKey = indexRead.getKey();
				if(numKey == null){
					curPageNum = indexRead.getValue();
					break;
				}
				if (key < numKey) {
					curPageNum = indexRead.getValue();
					break;
				}
			}
			firstIndex = indexFilePath + "/"+curPageNum+".xlsx";
		}
		String temp = entryFilePath+"/"+curPageNum+".xlsx";
		if(FileUtil.exist(temp)){
			return curPageNum;
		}
		return null;
	}
}
