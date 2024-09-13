package BPlusTree.node;

/**
 * 节点抽象类
 */
public abstract class BTreeNode {
	//节点的高度
	protected Integer height;
	//节点的编号
	protected Integer number;
	//节点名称
	protected Integer[] keys;
	//个数统计
	protected int keyCount;
	//父节点
	protected BTreeNode parentNode;
	//左兄弟
	protected BTreeNode leftSibling;
	//右兄弟
	protected BTreeNode rightSibling;


	protected BTreeNode() {
		this.keyCount = 0;
		this.parentNode = null;
		this.leftSibling = null;
		this.rightSibling = null;
	}


	public int getKeyCount() {
		return this.keyCount;
	}

	public Integer getKey(int index) {
		return this.keys[index];
	}

	public void setKey(int index, Integer key) {
		this.keys[index] = key;
	}

	public BTreeNode getParent() {
		return this.parentNode;
	}

	public void setParent(BTreeNode parent) {
		this.parentNode = parent;
	}

	public Integer getNumber(){
		return this.number;
	}

	public void setNumber(Integer number){
		this.number = number;
	}

	public void setHeight(Integer height){
		this.height = height;
	}

	public Integer getHeight(){
		return this.height;
	}

	public void setKeyCount(Integer integer){
		this.keyCount = integer;
	}



	public abstract TreeNodeType getNodeType();


	/**
	 *如果当前节点是叶子节点，如果找到了则返回其在节点中的位置，否则返回 -1 表示未找到。
	 *如果当前节点是内部节点，如果找到了则返回其在节点中的位置，如果未找到，则返回一个 "应该" 包含该键的子节点的索引。
	 */
	public abstract int search(Integer key);


	/**
	 * 判断节点是否溢出
	 */
	public boolean isOverflow() {
		return this.getKeyCount() == this.keys.length;
	}

	/**
	 * 处理溢出，需要进行分裂
	 * @return
	 */
	public BTreeNode dealOverflow() {
		int midIndex = this.getKeyCount() / 2;
		Integer upKey = this.getKey(midIndex);
		//得到分裂后的新节点
		BTreeNode newRNode = this.split();
		//将新节点插入到B+树中去
		if (this.getParent() == null) {
			this.setParent(new BTreeInnerNode());
		}
		newRNode.setParent(this.getParent());

		// 将分裂后的两个节点连接起来
		newRNode.setLeftSibling(this);
		newRNode.setRightSibling(this.rightSibling);
		if (this.getRightSibling() != null)
			this.getRightSibling().setLeftSibling(newRNode);
		this.setRightSibling(newRNode);

		// 将中间的key添加到上层节点
		return this.getParent().pushUpKey(upKey, this, newRNode);
	}

	/**
	 * 分裂节点操作。将当前节点分裂为两个节点，将中间的键值对上提到父节点。
	 */
	protected abstract BTreeNode split();


	/**
	 * 向上推键操作。当插入新键后可能导致节点溢出时，调用该方法将溢出问题向上推送到父节点。
	 * @param key
	 * @param leftChild
	 * @param rightNode
	 * @return
	 */
	protected abstract BTreeNode pushUpKey(Integer key, BTreeNode leftChild, BTreeNode rightNode);






	/* ---------------------------------------删除操作相关--------------------------------------- */

	/**
	 * 判断是否使用率没达到50%
	 * @return
	 */
	public boolean isUnderflow() {
		return this.getKeyCount() < (this.keys.length / 2);
	}

	/**
	 * 判断是否可以借一个索引给兄弟
	 * @return
	 */
	public boolean canLendAKey() {
		return this.getKeyCount() > (this.keys.length / 2);
	}

	public BTreeNode getLeftSibling() {
		if (this.leftSibling != null && this.leftSibling.getParent() == this.getParent())
			return this.leftSibling;
		return null;
	}

	public void setLeftSibling(BTreeNode sibling) {
		this.leftSibling = sibling;
	}

	public BTreeNode getRightSibling() {
		if (this.rightSibling != null && this.rightSibling.getParent() == this.getParent())
			return this.rightSibling;
		return null;
	}

	public BTreeNode getRightNode() {
		if (this.rightSibling != null)
			return this.rightSibling;
		return null;
	}

	public void setRightSibling(BTreeNode silbling) {
		this.rightSibling = silbling;
	}

	/**
	 * 处理删除后使用率没有达到50%
	 * @return
	 */
	public BTreeNode dealUnderflow() {
		//如果整个树只有这一个节点，不用操作
		if (this.getParent() == null) return null;
		// 首先尝试从兄弟节点来借索引
		BTreeNode leftSibling = this.getLeftSibling();
		if (leftSibling != null && leftSibling.canLendAKey()) {
			this.getParent().processChildrenTransfer(this, leftSibling, leftSibling.getKeyCount() - 1);
			return null;
		}
		BTreeNode rightSibling = this.getRightSibling();
		if (rightSibling != null && rightSibling.canLendAKey()) {
			this.getParent().processChildrenTransfer(this, rightSibling, 0);
			return null;
		}
		// 左右兄弟都借不了，那么尝试和左右兄弟进行合并
		if (leftSibling != null) {
			return this.getParent().processChildrenFusion(leftSibling, this);
		}
		else {
			return this.getParent().processChildrenFusion(this, rightSibling);
		}
	}

	protected abstract void processChildrenTransfer(BTreeNode borrower, BTreeNode lender, int borrowIndex);

	protected abstract BTreeNode processChildrenFusion(BTreeNode leftChild, BTreeNode rightChild);

	protected abstract void fusionWithSibling(Integer sinkKey, BTreeNode rightSibling);

	protected abstract Integer transferFromSibling(Integer sinkKey, BTreeNode sibling, int borrowIndex);
}
