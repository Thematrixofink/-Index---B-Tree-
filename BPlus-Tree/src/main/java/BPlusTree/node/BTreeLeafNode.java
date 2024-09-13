package BPlusTree.node;

/**
 * 叶子节点

 */
public class BTreeLeafNode extends BTreeNode {
	public final static int LEAF_NUM = 256;
	private String[] values;

	public BTreeLeafNode() {
		this.keys = new Integer[LEAF_NUM + 1];
		this.values = new String[LEAF_NUM + 1];
	}


	@SuppressWarnings("unchecked")
	public String getValue(int index) {
		return (String)this.values[index];
	}

	public void setValue(int index, String value) {
		this.values[index] = value;
	}


	@Override
	public TreeNodeType getNodeType() {
		return TreeNodeType.LeafNode;
	}


	@Override
	public int search(Integer key) {
		int keySum = this.getKeyCount();
		for (int i = 0 ; i <  keySum ; ++i) {
			Integer temp = this.getKey(i);

			 if (temp.compareTo(key) == 0) {
				 return i;
			 }
			 else if (temp > key) {
				 return -1;
			 }
		}
		return -1;
	}


	/*-----------------------------------------插入操作相关-----------------------------------------*/

	/**
	 * 向叶子节点插入索引项
	 * @param key
	 * @param value
	 */
	public void insertIndex(Integer key, String value) {
		int index = 0;
		int keySum = this.getKeyCount();
		Integer indexKey = this.getKey(index);
		//如果key比index的key大，那么就继续向下
		while (index < keySum && indexKey < key) {
			index++;
		}
		this.insertAt(index, key, value);
	}


	private void insertAt(int index, Integer key, String value) {
		int keySum = this.getKeyCount();
		// 把原本index-- keySum-1下标处的key以及value要向后移动一格
		for (int i = keySum - 1 ; i >= index ; i--) {
			//i位置的挪到i+1处
			this.setKey(i + 1, this.getKey(i));
			this.setValue(i + 1, this.getValue(i));
		}
		// 插入新的key和value
		this.setKey(index, key);
		this.setValue(index, value);
		this.keyCount++;
	}


	/**
	 * 分裂节点操作。将当前节点分裂为两个节点，
	 * 将中间的键值对上提到父节点。
	 */
	@Override
	protected BTreeNode split() {
		int midIndex = this.getKeyCount() / 2;
		BTreeLeafNode newRNode = new BTreeLeafNode();
		//原来节点midIndex到结尾的分裂到新的节点
		for (int i = midIndex; i < this.getKeyCount() ; i++) {
			newRNode.setKey(i - midIndex, this.getKey(i));
			newRNode.setValue(i - midIndex, this.getValue(i));
			this.setKey(i, null);
			this.setValue(i, null);
		}
		newRNode.keyCount = this.getKeyCount() - midIndex;
		this.keyCount = midIndex;
		return newRNode;
	}

	@Override
	protected BTreeNode pushUpKey(Integer key, BTreeNode leftChild, BTreeNode rightNode) {
		throw new UnsupportedOperationException();
	}


	/* ---------------------------------------删除操作相关--------------------------------------- */

	public boolean delete(Integer key) {
		int index = this.search(key);
		if (index == -1)
			return false;

		this.deleteAt(index);
		return true;
	}

	private void deleteAt(int index) {
		int i = index;
		int keySum = this.getKeyCount();
		//把index--> keySum-1 的key和value全部向前移动一个位置
		for (i = index; i < keySum - 1; i++) {
			this.setKey(i, this.getKey(i + 1));
			this.setValue(i, this.getValue(i + 1));
		}
		//把最后一个多余的删除
		this.setKey(i, null);
		this.setValue(i, null);
		this.keyCount--;
	}


	@Override
	@SuppressWarnings("unchecked")
	protected void fusionWithSibling(Integer sinkKey, BTreeNode rightSibling) {
		BTreeLeafNode siblingLeaf = (BTreeLeafNode)rightSibling;

		int j = this.getKeyCount();
		for (int i = 0; i < siblingLeaf.getKeyCount(); ++i) {
			this.setKey(j + i, siblingLeaf.getKey(i));
			this.setValue(j + i, siblingLeaf.getValue(i));
		}
		this.keyCount += siblingLeaf.getKeyCount();

		this.setRightSibling(siblingLeaf.rightSibling);
		if (siblingLeaf.rightSibling != null)
			siblingLeaf.rightSibling.setLeftSibling(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Integer transferFromSibling(Integer sinkKey, BTreeNode sibling, int borrowIndex) {
		BTreeLeafNode siblingNode = (BTreeLeafNode)sibling;

		this.insertIndex(siblingNode.getKey(borrowIndex), siblingNode.getValue(borrowIndex));
		siblingNode.deleteAt(borrowIndex);

		return borrowIndex == 0 ? sibling.getKey(0) : this.getKey(0);
	}

	@Override
	protected void processChildrenTransfer(BTreeNode borrower, BTreeNode lender, int borrowIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected BTreeNode processChildrenFusion(BTreeNode leftChild, BTreeNode rightChild) {
		throw new UnsupportedOperationException();
	}
}
