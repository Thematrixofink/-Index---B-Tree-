package BPlusTree.node;

import lombok.Data;

/**
 * 树节点类型枚举
 */
public enum TreeNodeType {
    /**
     * 非叶子节点
     */
    InnerNode,
    /**
     * 叶子节点
     */
    LeafNode;
}
