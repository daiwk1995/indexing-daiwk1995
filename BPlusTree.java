import java.util.*;

// The BPlusTree class. You'll need to fill the methods in. DO NOT change the
// function signatures of these methods. Our checker relies on these methods and
// assume these function signatures.

public class BPlusTree {

    // A tree has a root node, and an order
    public Node root;
    public Integer order;
    public boolean isS;

    // Required methods to implement. DO NOT change the function signatures of
    // these methods.

    // Instantiate a BPlusTree with a specific order
    public BPlusTree(Integer order) {
        this.order = order;
        this.root = new LNode(order);
        this.isS = false;
    }

    public BPlusTree(Integer order, boolean isS) {
        this.order = order;
        this.isS = isS;
        if(this.isS) {
            this.root = new SNode(order);
        }
        else {
            this.root = new LNode(order);
        }
    }

    // Given a key, returns the value associated with that key or null if doesnt
    // exist
    public Integer get(Integer key) {
        return this.root.get(key);
    }

    public TupleIDSet getSecondary(Integer key) {
        return this.root.getSecondary(key);
    }

    // Insert a key-value pair into the tree. This tree does not need to support
    // duplicate keys
    public void insert(Integer key, Integer value) {
        Split root_split = this.root.insert(key, value);
        if  (root_split != null) {
            INode new_root = new INode(order, root_split);
            this.root = new_root;
        }
    }

    // Delete a key and its value from the tree
    public void delete(Integer key) {
        root.delete(key);
        if ((root.numChildren == 1) && (root.nt == NodeType.INTERNAL)) {
            this.root = ((INode)root).children[0];
        }
    }

    public TupleIDSet clusterIndexRangeQuery(Integer lowerBound, Integer upperBound, Integer maxID){
        TupleIDSet result = new TupleIDSet();
        Integer lower = this.root.rangeGetLowerBound(lowerBound);
        Integer upper = this.root.rangeGetUpperBound(upperBound);
        if(lower == null) {
            return result;
        }
        Integer loopUpperBound = upper == null ? maxID : upper;

        for(int i = lower ; i < loopUpperBound; i++) {
            result.add(i);
        }
        return result;

    }

    public TupleIDSet SecondIndexRangeQuery(Integer lowerBound, Integer upperBound){
        Node startNode = this.root.rangeGetSecondary(lowerBound);
        SNode cur = (SNode) startNode;
        TupleIDSet result = new TupleIDSet();
        while(cur != null){
            for(int i =0 ; i < cur.numChildren; i++){
                if(cur.keys[i] >= lowerBound && cur.keys[i] <= upperBound){
                    result.addAll(cur.values[i]);
                }
            }
            if(cur.keys[cur.numChildren - 1] < upperBound) {
                cur = (SNode)cur.rightSibling;
            }
            else break;
        }
        return result;
    }
    // Optional methods to write
    // This might be a helpful function for your debugging needs
    // public void print() { }
}

// DO NOT change this enum. There are two types of nodes; an Internal node, and
// a Leaf node
enum NodeType {
    LEAF,
    INTERNAL,
}

// This class encapsulates the pair of left and right nodes after a split
// occurs, along with the key that divides the two nodes. Both leaf and internal
// nodes split. For this reason, we use Java's generics (e.g. <T extends Node>).
// This is a helper class. Your implementation might not need to use this class
class Split<T extends Node> {
    public Integer key;
    public T left;
    public T right;

    public Split(Integer k, T l, T r) {
        key = k;
        right = r;
        left = l;
    }
}

// An abstract class for the node. Both leaf and internal nodes have the a few
// attributes in common.
abstract class Node {

    // DO NOT edit this attribute. You should use to store the keys in your
    // nodes. Our checks for correctness rely on this attribute. If you change
    // it, your tree will not be correct according to our checker. Values in
    // this array that are not valid should be null.
    public Integer[] keys;

    // Do NOT edit this attribute. You should use it to keep track of the number
    // of CHILDREN or VALUES this node has. Our checks for correctness rely on
    // this attribute. If you change it, your tree will not be correct according
    // to our checker.
    public Integer numChildren;

    // DO NOT edit this method.
    abstract NodeType nodeType();

    // You may edit everything that occurs in this class below this line.
    // *********************************************************************
    public Integer order;
    public NodeType nt;
    public Node rightSibling;
    // Both leaves and nodes need to keep track of a few things:
    //      their parent
    //      a way to tell another class whether it is a leaf or a node

    // A node is instantiated by giving it an order, and a node type
    public Node(Integer order, NodeType nt) {
        this.order = order;
        this.numChildren = 0;
        this.keys = new Integer[order];
        this.nt = nt;
    }


    public Integer mid(){
        return this.order/2;
    }
    /*
    public NodeType nodeType(){
        return this.nt;
    }*/


    // A few things both leaves and internal nodes need to do. You are likely
    // going to need to implement these functions. Our correctness checks rely
    // on the structure of the keys array, and values and children arrays in the
    // leaf and child nodes so you may choose to forgo these functions.

    // You might find that printing your nodes' contents might be helpful in
    // debugging. The function signature here assumes spaces are used to
    // indicate the level in the tree.
    //abstract void print(Integer nspaces);

    // You might want to implement a search method to search for the
    // corresponding position of a given key in the node
    abstract Integer search(Integer key);

    // You might want to implement a split method for nodes that need to split.
    // We use the split class defined above to encapsulate the information
    // resulting from the split.
    abstract Split split();          // Note the use of split here

    // You might want to implement an insert method. We use the Split class to
    // indicate whether a node split as a result of an insert because splits in
    // lower levels of the tree may propagate upward.
    abstract Split insert(Integer key, Integer value); // And split here

    // You might want to implement a delete method that traverses down the tree
    // calling a child's delete method until you hit the leaf.
    abstract void delete(Integer key);

    // You might want to implement a get method that behaves similar to the
    // delete method. Here, the get method recursively calls the child's get
    // method and returns the integer up the recursion.
    abstract Integer get(Integer key);

    abstract TupleIDSet getSecondary(Integer key);
    // You might want to implement a helper function that cleans up a node. Note
    // that the keys, values, and children of a node should be null if it is
    // invalid. Java's memory manager won't garbage collect if there are
    // references hanging about.
    abstract void cleanEntries();

    abstract Integer rangeGetUpperBound(Integer key);

    abstract Integer rangeGetLowerBound(Integer key);

    abstract Node rangeGetSecondary(Integer key);
}

// A leaf node (LNode) is an instance of a Node
class LNode extends Node {

    // DO NOT edit this attribute. You should use to store the values in your
    // leaf node. Our checks for correctness rely on this attribute. If you
    // change it, your tree will not be correct according to our checker. Values
    // in this array that are not valid should be null.
    public Integer[] values;

    // DO NOT edit this method;
    public NodeType nodeType() { return NodeType.LEAF; };

    // You may edit everything that occurs in this class below this line.
    // *************************************************************************

    // A leaf has siblings on the left and on the right.
    // A leaf node is instantiated with an order
    public LNode(Integer order) {

        // Because this is also a Node, we instantiate the Node (abstract)
        // superclass, identifying itself as a leaf.
        super(order, NodeType.LEAF);
        this.rightSibling = null;
        this.values = new Integer[order];
        // A leaf needs to instantiate the values array.
    }
    /*
    public void print(Integer nspaces){
        String k = Arrays.toString(this.keys);
        String v = Arrays.toString(this.values);
        String space = new String(new char[nspaces]).replace("\0", " ");
        System.out.println(spaces
                + "Leaf: N: "
                + this.num
                + " Keys: "
                + k
                + " Values: "
                + v);
    }*/

    // search the idx for the new key
    public Integer search(Integer key){
        for (Integer i = 0; i < this.numChildren; i++) {
            if (keys[i] >= key) {
                return i;
            }
        }
        return this.numChildren;
    }

    public Integer get(Integer key){
        for (Integer i = 0; i < this.numChildren; i++) {
            if (this.keys[i].equals(key)) {
                return this.values[i];
            }
        }
        return null;
    }

    public TupleIDSet getSecondary(Integer key){
        return null;
    }

    //split the array into two different array
    Split<LNode> split() {
        LNode right = new LNode(this.order);
        Integer mid = this.mid();
        Integer num = this.numChildren - mid;
        System.arraycopy(this.keys, mid, right.keys, 0, num);
        System.arraycopy(this.values, mid, right.values, 0, num);

        right.numChildren = num;
        this.numChildren = mid;

        if (this.rightSibling != null) {
            right.rightSibling = this.rightSibling;
        }
        this.rightSibling = right;

        Split<LNode> split = new Split<LNode>(keys[mid], this, right);

        this.cleanEntries();
        right.cleanEntries();
        return split;
    }

    //insert the new key and value
    public Split insert(Integer key, Integer value) {
        Integer idx = search(key);

        // if idx not in keys, we just add a new key and value;
        if (idx == this.numChildren) {
            this.keys[idx] = key;
            this.values[idx] = value;
            this.numChildren++;
        }

        // if key already in the array, replace the value;
        else if (keys[idx].equals(key)) { values[idx] = value; }

        // if the key not in array and key < keys[idx],shift it
        else {

            Integer num = this.numChildren - idx;
            System.arraycopy(keys, idx, keys, idx+1, num);
            System.arraycopy(values, idx, values, idx+1, num);

            keys[idx] = key;
            values[idx] = value;
            this.numChildren++;
        }

        if (this.numChildren == this.order) { return this.split(); }

        return null;

    }
    //keep the array and clean it
    public void cleanEntries() {
        for (Integer i = 0; i < this.order; i++) {
            if (i < this.numChildren) { continue; }
            else {
                keys[i] = null;
                values[i] = null;
            }
        }
    }

    //delete the key and value in the array
    public void delete(Integer key) {
        Integer idx = this.search(key);

        if (keys[idx] != key) {
            return;
        }

        Integer num = this.numChildren - (idx + 1);
        System.arraycopy(keys, idx+1, keys, idx, num);
        System.arraycopy(values, idx+1, values, idx, num);
        this.numChildren--;

        this.cleanEntries();
    }

    public Integer rangeGetUpperBound(Integer key){
        Integer idx = search(key);
        if(idx == this.numChildren){
            if(this.rightSibling == null) {
                return null;
            }
            else {
                LNode tmp = (LNode)this.rightSibling;
                return tmp.values[0];
            }
        }
        if(keys[idx].equals(key)){
            if(idx == this.numChildren - 1){
                if(this.rightSibling != null) {
                    //System.out.println('5');
                    LNode tmp = (LNode)this.rightSibling;
                    return tmp.values[0];
                }
                else {
                    //System.out.println('2');
                    return null;
                }
            }
            else{
                //System.out.println('3');
                return this.values[idx + 1];
            }

        }
        //System.out.println('4');
        return this.values[idx];
    }

    public Integer rangeGetLowerBound(Integer key){
        Integer idx = search(key);

        if(idx == this.numChildren){
            if(this.rightSibling == null){
                return null;
            }
            else {
                LNode tmp = (LNode)this.rightSibling;
                return tmp.values[0];
            }
        }

        return this.values[idx];
    }
    public Node rangeGetSecondary(Integer key){
        return null;
    }
}

// An internal node (INode) is an instance of a Node
class INode extends Node {

    // DO NOT edit this attribute. You should use to store the children of this
    // internalnode. Our checks for correctness rely on this attribute. If you
    // change it, your tree will not be correct according to our checker. Values
    // in this array that are not valid should be null.
    // An INode (as opposed to a leaf) has children. These children could be
    // either leaves or internal nodes. We use the abstract Node class to tell
    // Java that this is the case. Using this abstract class allows us to call
    // abstract functions regardless of whether it is a leaf or an internal
    // node. For example, children[x].get() would work regardless of whether it
    // is a leaf or internal node if the get function is an abstract method in
    // the Node class.
    public Node[] children; 

    // DO NOT edit this method;
    public NodeType nodeType() { return NodeType.INTERNAL; };

    // You may edit everything that occurs in this class below this line.
    // *************************************************************************
    // A leaf node is instantiated with an order
    public INode(Integer order) {
        super(order, NodeType.INTERNAL);
        this.rightSibling = null;
        this.children = new Node[order+1];
    }

    public INode(Integer order, Split split) {

        // Because this is also a Node, we instantiate the Node (abstract)
        // superclass, identifying itself as a leaf.
        super(order, NodeType.INTERNAL);
        this.children = new Node[this.order+1];
        // An INode needs to instantiate the children array

        // iitial setup is always from a split node...
        // what is a parent but for the children?
        this.children[0] = split.left;
        this.children[1] = split.right;
        this.keys[0] = split.key;
        this.numChildren = 2;
    }


    Integer search(Integer key) {
        for (Integer i = 0; i < this.numChildren - 1; i++) {
            if (keys[i] >= key) {
                return i;
            }
        }
        return this.numChildren - 1;
    }


    public Integer get(Integer key){
        Integer idx =search(key);

        if ((idx == this.numChildren-1) || (key < keys[idx])) {
            return children[idx].get(key);
        }
        else if (keys[idx].equals(key)) {
            return children[idx + 1].get(key);
        }
        return null;
    }

    public TupleIDSet getSecondary(Integer key){
        return null;
    }

    Split<INode> split() {
        Integer mid = this.mid();
        INode right = new INode(this.order);

        Integer num = this.numChildren - mid;
        System.arraycopy(keys, mid, right.keys, 0, num - 1);
        System.arraycopy(children, mid, right.children, 0, num);

        right.numChildren = num;
        this.numChildren = mid;

        this.rightSibling = right;

        Split<INode> split = new Split<INode>(keys[mid-1], this, right);

        this.cleanEntries();
        right.cleanEntries();

        return split;
    }

    Split<INode> insertSplit(Split to_insert) {

        Integer key = to_insert.key;
        Integer idx = this.search(key);

        Node new_child = to_insert.right;

        // corner case
        if (idx == this.numChildren-1) {
            keys[this.numChildren-1] = key;
            //System.out.println(Arrays.toString(children));
            children[this.numChildren] = new_child;
            children[this.numChildren-1].rightSibling = new_child;
        }
        else {
            Integer num = this.numChildren-idx-1;
            System.arraycopy(keys, idx, keys, idx+1, num);
            System.arraycopy(children, idx+1, children, idx+2, num);
            keys[idx] = key;
            children[idx+1] = new_child;
            children[idx].rightSibling = new_child;
            new_child.rightSibling = children[idx+2];
        }

        this.numChildren++;

        if (this.numChildren > this.order) {
            return this.split();
        }

        return null;

    }

    public Split insert(Integer key, Integer value) {

        Integer idx = this.search(key);

        Split split = null;

        //corner case
        if ((idx == this.numChildren-1) || (key < keys[idx])) {
            split = children[idx].insert(key, value);
        }

        //in the case key between two different key
        else if (keys[idx].equals(key)) {
            split = children[idx + 1].insert(key, value);
        }

        if (split == null) {
            return null;
        }
        else {
            return insertSplit(split);
        }

    }

    void cleanEntries() {
        for (Integer i = 0; i < this.order+1; ++i) {
            if (i < this.numChildren) {
                continue;
            }
            else {
                keys[i-1] = null;
                children[i] = null;
            }
        }
    }

    public void delete(Integer key) {
        Integer idx = this.search(key);

        // Order here matters - idx could be at the end of the array where the
        // value COULD be null...
        if ((idx == this.numChildren-1) || (key < keys[idx])) {
            children[idx].delete(key);
        }

        // keys point to references corresponsing to >= to...
        // alternatively, idx could be at the end of the key list (the key >
        // max key in node)
        else if (keys[idx].equals(key)) {
            children[idx + 1].delete(key);
        }
    }

    public Integer rangeGetLowerBound(Integer key) {

        Integer idx = search(key);
        if(idx == this.numChildren - 1 || keys[idx] > key){
            return children[idx].rangeGetLowerBound(key);
        }else if(keys[idx].equals(key)){
            return children[idx + 1].rangeGetLowerBound(key);
        }
        return null;
    }

    public Integer rangeGetUpperBound(Integer key) {

        Integer idx = search(key);
        if(idx == this.numChildren - 1 || keys[idx] > key){
            return children[idx].rangeGetUpperBound(key);
        }else if(keys[idx].equals(key)){
            return children[idx + 1].rangeGetUpperBound(key);
        }
        return null;
    }

    Node rangeGetSecondary(Integer key) {
        Integer idx = search(key);

        if(idx == this.numChildren - 1 || keys[idx] > key){
            return children[idx].rangeGetSecondary(key);
        }else if(keys[idx].equals(key)){
            return children[idx + 1].rangeGetSecondary(key);
        }
        return null;
    }

}
//a type of leaf node only will be used for second index
class SNode extends Node {

    // DO NOT edit this attribute. You should use to store the values in your
    // leaf node. Our checks for correctness rely on this attribute. If you
    // change it, your tree will not be correct according to our checker. Values
    // in this array that are not valid should be null.
    public TupleIDSet[]values;
    //    Integer level;
    // DO NOT edit this method;
    public NodeType nodeType() { return NodeType.LEAF; }

    // You may edit everything that occurs in this class below this line.
    public SNode(Integer order) {

        // Because this is also a Node, we instantiate the Node (abstract)
        // superclass, identifying itself as a leaf.
        super(order, NodeType.LEAF);
        this.values = new TupleIDSet[order];
        this.rightSibling = null;
        // A leaf needs to instantiate the values array.
    }

    public Integer search(Integer key){
        for (Integer i = 0; i < this.numChildren; i++) {
            if (keys[i] >= key) {
                return i;
            }
        }
        return this.numChildren;
    }

    Split<SNode> split() {
        SNode right = new SNode(this.order);
        Integer mid = this.mid();
        Integer num = this.numChildren - mid;
        System.arraycopy(this.keys, mid, right.keys, 0, num);
        System.arraycopy(this.values, mid, right.values, 0, num);

        right.numChildren = num;
        this.numChildren = mid;

        if (this.rightSibling != null) {
            right.rightSibling = this.rightSibling;
        }
        this.rightSibling = right;

        Split<SNode> split = new Split<SNode>(keys[mid], this, right);

        this.cleanEntries();
        right.cleanEntries();
        return split;
    }


    Split insert(Integer key, Integer value) {
        Integer idx = search(key);

        // if idx not in keys, we just add a new key and value;
        if (idx == this.numChildren) {
            this.keys[idx] = key;
            this.values[idx] = new TupleIDSet();
            this.values[idx].add(value);
            this.numChildren++;
        }

        // if key already in the array, replace the value;
        else if (keys[idx].equals(key)) { values[idx].add(value); }

        // if the key not in array and key < keys[idx],shift it
        else {

            Integer num = this.numChildren - idx;
            System.arraycopy(keys, idx, keys, idx + 1, num);
            System.arraycopy(values, idx, values, idx + 1, num);

            this.keys[idx] = key;
            this.values[idx] = new TupleIDSet();
            this.values[idx].add(value);
            this.numChildren++;
        }

        if (this.numChildren == this.order) {
            return this.split();
        }

        return null;
    }

    //keep the array and clean it
    public void cleanEntries() {
        for (Integer i = 0; i < this.order; i++) {
            if (i < this.numChildren) { continue; }
            else {
                keys[i] = null;
                values[i] = null;
            }
        }
    }

    //delete the key and value in the array
    public void delete(Integer key) {
        Integer idx = this.search(key);

        if (keys[idx] != key) {
            return;
        }

        Integer num = this.numChildren - (idx + 1);
        System.arraycopy(keys, idx+1, keys, idx, num);
        System.arraycopy(values, idx+1, values, idx, num);
        this.numChildren--;

        this.cleanEntries();
    }

    public Integer get(Integer key) {
        return null;

    }

    public TupleIDSet getSecondary(Integer key) {
        for (Integer i = 0; i < this.numChildren; i++) {
            if (this.keys[i].equals(key)) {
                return this.values[i];
            }
        }
        return null;
    }

    public Integer rangeGetLowerBound(Integer key) {
        return null;
    }

    public Integer rangeGetUpperBound(Integer key) {
        return null;
    }

    public Node rangeGetSecondary(Integer key) {

        Integer idx = search(key);
        if(idx == this.numChildren){
            return this.rightSibling;
        }
        return this;
    }


}


// This is potentially encapsulates the resulting information after a node
// splits. This is might help when passing split information from the split
// child to the parent. Sea README for more details.
/*
class Split<T extends Node> {
    public Integer key;
    public T left;
    public T right; // always splits rightward

    public Split(Integer k, T l, T r) {
        key = k;
        right = r;
        left = l;
    }
}
*/

