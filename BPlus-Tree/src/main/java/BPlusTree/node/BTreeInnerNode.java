package BPlusTree.node;


/**
 * 非叶子节点
 */
public class BTreeInnerNode extends BTreeNode {
	//非叶子节点的Key的个数
	public final static int INNER_NUM = 512;
	protected Object[] children;

	public BTreeInnerNode() {
		//设置为INNER_NUM + 1，多一个用于检测溢出
		this.keys = new Integer[INNER_NUM + 1];
		this.children = new Object[INNER_NUM + 2];
	}


	/**
	 * 获取孩子节点
	 * @param index
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public BTreeNode getChild(int index) {
		return (BTreeNode)this.children[index];
	}

	/**
	 * 插入孩子
	 * @param index
	 * @param child
	 */
	public void setChild(int index, BTreeNode child) {
		this.children[index] = child;
		if (child != null)
			child.setParent(this);
	}

	@Override
	public TreeNodeType getNodeType() {
		return TreeNodeType.InnerNode;
	}

	/**
	 * 根据Key获取下一层节点位置
	 * @param key
	 * @return
	 */
	@Override
	public int search(Integer key) {
		int index = 0;
		int keySum = this.getKeyCount();
		for (index = 0 ; index < keySum ; index++) {
			Integer temp = this.getKey(index);
			//compare > 0 --> temp > key
			//compare < 0 --> temp < key
			//如果两者key相同，那么就是右侧
			if(key == temp){
				return index + 1;
			}
			//如果key小于此index的key，那么就是左侧
			if(key < temp){
				return index;
			}
		}
		return index;
	}

	/*-----------------------------------------插入操作相关-----------------------------------------*/

	/**
	 * 插入操作。在指定索引位置插入一个新的键值对，并调整子节点数组。
	 * 如果插入后节点溢出，则调用 dealOverflow() 方法处理溢出情况。
	 * @param index
	 * @param key
	 * @param leftChild
	 * @param rightChild
	 */
	private void insertAt(int index, Integer key, BTreeNode leftChild, BTreeNode rightChild) {
		int keySum = this.getKeyCount();
		// 把index --> keySum + 1的value全都向后移动一个
		for (int i = keySum + 1 ; i > index ; i--) {
			this.setChild(i, this.getChild(i - 1));
		}
		// 把index --> keySum  的key全都向后移动一个
		for (int i = keySum ; i > index ; i--) {
			this.setKey(i, this.getKey(i - 1));
		}

		// 在index处插入key以及左右孩子
		this.setKey(index, key);
		this.setChild(index, leftChild);
		this.setChild(index + 1, rightChild);
		this.keyCount++;
	}

	/**
	 * 分裂节点操作。将当前节点分裂为两个节点，将中间的键值对上提到父节点。
	 */
	@Override
	protected BTreeNode split() {
		int midIndex = this.getKeyCount() / 2;
		BTreeInnerNode newRNode = new BTreeInnerNode();
		//当前节点前midIndex分裂到newRNode的0-midIndex
		for (int i = midIndex + 1; i < this.getKeyCount(); ++i) {
			newRNode.setKey(i - midIndex - 1, this.getKey(i));
			this.setKey(i, null);
		}
		for (int i = midIndex + 1; i <= this.getKeyCount(); ++i) {
			newRNode.setChild(i - midIndex - 1, this.getChild(i));
			newRNode.getChild(i - midIndex - 1).setParent(newRNode);
			this.setChild(i, null);
		}
		this.setKey(midIndex, null);
		newRNode.keyCount = this.getKeyCount() - midIndex - 1;
		this.keyCount = midIndex;
		return newRNode;
	}

	/**
	 * 向上推键操作。当插入新键后可能导致节点溢出时，调用该方法将溢出问题向上推送到父节点。
	 * @param key
	 * @param leftChild
	 * @param rightNode
	 * @return
	 */
	@Override
	protected BTreeNode pushUpKey(Integer key, BTreeNode leftChild, BTreeNode rightNode) {
		// 获得当前key应当插入的位置
		int index = this.search(key);
		this.insertAt(index, key, leftChild, rightNode);
		// 检查是否溢出
		if (this.isOverflow()) {
			return this.dealOverflow();
		}
		else {
			return this.getParent() == null ? this : null;
		}
	}




	/* ---------------------------------------删除操作相关--------------------------------------- */

	/**
	 *  删除指定索引位置的键值对，调整子节点数组。如果删除后节点下溢，则调用 dealUnderflow() 方法处理下溢情况。
	 * @param index
	 */
	private void deleteAt(int index) {
		int i = 0;
		for (i = index; i < this.getKeyCount() - 1; ++i) {
			this.setKey(i, this.getKey(i + 1));
			this.setChild(i + 1, this.getChild(i + 2));
		}
		this.setKey(i, null);
		this.setChild(i + 1, null);
		--this.keyCount;
	}


	/**
	 * 处理子节点转移。当删除键值对后可能导致节点下溢时，尝试从兄弟节点中借取键值对，以解决下溢问题。
	 * @param borrower		借索引的节点
	 * @param lender		借出索引的节点
	 * @param borrowIndex	借出的索引的下标
	 */
	@Override
	protected void processChildrenTransfer(BTreeNode borrower, BTreeNode lender, int borrowIndex) {
		int borrowerChildIndex = 0;
		while (borrowerChildIndex < this.getKeyCount() + 1 && this.getChild(borrowerChildIndex) != borrower)
			borrowerChildIndex++;

		if (borrowIndex == 0) {
			// 从右兄弟借
			Integer upKey = borrower.transferFromSibling(this.getKey(borrowerChildIndex), lender, borrowIndex);
			this.setKey(borrowerChildIndex, upKey);
		}
		else {
			// 从左兄弟借
			Integer upKey = borrower.transferFromSibling(this.getKey(borrowerChildIndex - 1), lender, borrowIndex);
			this.setKey(borrowerChildIndex - 1, upKey);
		}
	}


	/**
	 * 处理子节点合并。当删除键值对后可能导致节点下溢时，尝试将当前节点与其兄弟节点合并，以解决下溢问题。
	 * @param leftChild
	 * @param rightChild
	 * @return
	 */
	@Override
	protected BTreeNode processChildrenFusion(BTreeNode leftChild, BTreeNode rightChild) {
		int index = 0;
		while (index < this.getKeyCount() && this.getChild(index) != leftChild)
			index++;
		Integer sinkKey = this.getKey(index);
		leftChild.fusionWithSibling(sinkKey, rightChild);
		this.deleteAt(index);

		if (this.isUnderflow()) {
			if (this.getParent() == null) {
				if (this.getKeyCount() == 0) {
					leftChild.setParent(null);
					return leftChild;
				}
				else {
					return null;
				}
			}

			return this.dealUnderflow();
		}

		return null;
	}


	/**
	 * 将当前节点与右兄弟节点合并。将右兄弟节点的键值对和子节点合并到当前节点中，然后调整指针以保持正确的连接关系。
	 * @param sinkKey
	 * @param rightSibling
	 */
	@Override
	protected void fusionWithSibling(Integer sinkKey, BTreeNode rightSibling) {
		BTreeInnerNode rightSiblingNode = (BTreeInnerNode)rightSibling;

		int j = this.getKeyCount();
		this.setKey(j++, sinkKey);

		for (int i = 0; i < rightSiblingNode.getKeyCount(); ++i) {
			this.setKey(j + i, rightSiblingNode.getKey(i));
		}
		for (int i = 0; i < rightSiblingNode.getKeyCount() + 1; ++i) {
			this.setChild(j + i, rightSiblingNode.getChild(i));
		}
		this.keyCount += 1 + rightSiblingNode.getKeyCount();

		this.setRightSibling(rightSiblingNode.rightSibling);
		if (rightSiblingNode.rightSibling != null)
			rightSiblingNode.rightSibling.setLeftSibling(this);
	}

	/**
	 * 从兄弟节点中转移键值对。根据借取位置，从左兄弟节点或右兄弟节点中借取键值对，并将其插入到当前节点中。
	 * @param sinkKey
	 * @param sibling
	 * @param borrowIndex
	 * @return
	 */
	@Override
	protected Integer transferFromSibling(Integer sinkKey, BTreeNode sibling, int borrowIndex) {
		//借出的兄弟节点
		BTreeInnerNode siblingNode = (BTreeInnerNode)sibling;
		Integer upKey = null;
		if (borrowIndex == 0) {
			// 将右Sibling的第一个索引拼接到其末尾
			int index = this.getKeyCount();
			this.setKey(index, sinkKey);
			this.setChild(index + 1, siblingNode.getChild(borrowIndex));
			this.keyCount++;

			upKey = siblingNode.getKey(0);
			siblingNode.deleteAt(borrowIndex);
		}
		else {
			// 从左Sibling的最右边索引拼接到其开头
			this.insertAt(0, sinkKey, siblingNode.getChild(borrowIndex + 1), this.getChild(0));
			upKey = siblingNode.getKey(borrowIndex);
			siblingNode.deleteAt(borrowIndex);
		}
		return upKey;
	}
}
