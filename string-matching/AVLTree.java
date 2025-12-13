import java.util.*;

public class AVLTree<K extends Comparable<K>,V> {
	AVLNode<K,V> root;
	public AVLTree(){
		root =null;
	}
	int height(AVLNode<K,V> node){
		if (node == null) return 0;
		return node.height;
	}
	int getBalance(AVLNode<K,V> node){
		if (node == null) return 0;
		return height(node.left)-height(node.right);
	}
	AVLNode<K,V> rightRotate(AVLNode<K,V> y){
		AVLNode<K,V> x = y.left;
		AVLNode<K,V> T2 = x.right;
		x.right=y;
		y.left=T2;
		y.height=Math.max(height(y.left), height(y.right))+1;
		x.height=Math.max(height(x.left), height(x.right))+1;
		return x;
	}
	// rotation 수행	
	AVLNode<K,V> leftRotate(AVLNode<K,V> x){
		AVLNode<K,V> y = x.right;
		AVLNode<K,V> T2 =y.left;
		y.left=x;
		x.right=T2;
		x.height=Math.max(height(x.left), height(x.right)) +1;
		y.height=Math.max(height(y.left), height(y.right)) +1;
		return y;
	}
	public void insert(K key,V value) {
		root = insertNode(root, key,value);
	}
	AVLNode<K, V> insertNode(AVLNode<K, V> node, K key, V value) {
		if (node ==null){
			return new AVLNode<K, V>(key, value);}
		int i=key.compareTo(node.key);
		if (i <0){
			node.left=insertNode(node.left, key, value);
		}else if (i >0){
			node.right = insertNode(node.right, key, value);
		}else {
			// 같은 키라면 linkedlist에 삽입
			addValueToList(node, value);
			return node;
		}
		node.height = 1 + Math.max(height(node.left), height(node.right));
		int balance = getBalance(node);
		// unbalanced한 경우 나누기
		if(balance > 1 && key.compareTo(node.left.key)<0){
			return rightRotate(node);
		}
		if(balance < -1 && key.compareTo(node.right.key)>0){
			return leftRotate(node);
		}
		if(balance > 1 && key.compareTo(node.left.key)>0){
			node.left= leftRotate(node.left);
			return rightRotate(node);
		}
		if(balance < -1 && key.compareTo(node.right.key)< 0){
			node.right = rightRotate(node.right);
			return leftRotate(node);
		}
		return node;
	}
	void addValueToList(AVLNode<K,V> node, V value) {
		PositionNode<V> newNode =new PositionNode<V>(value);
		if (node.valueList == null){
			node.valueList=newNode;
		} else {
			PositionNode<V> current=node.valueList;
			while (current.next != null){
				current= current.next;
			}
			current.next= newNode;
		}
	}
	public List<V> search(K key) {
		List<V> result = new ArrayList<>();
		AVLNode<K,V> node = searchNode(root, key);
		if (node != null) {
			PositionNode<V> current = node.valueList;
			while (current !=null) {
				result.add(current.data);
				current = current.next;
			}
		}
		return result;
	}
	AVLNode<K,V> searchNode(AVLNode<K, V> node, K key) {
		if (node ==null) return null;
		int i =key.compareTo(node.key);
		if (i<0){
			return searchNode(node.left, key);
		} else if(i>0){
			return searchNode(node.right, key);
		} else{
			return node;
		}
	}
	public void preorderTraversal(StringBuilder sb) {
		preorderHelper(root, sb);
		//마지막 공백 제거
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' '){
			sb.setLength(sb.length()-1);
		}
	}
	public boolean isEmpty(){
		return root==null;
	}
	void preorderHelper(AVLNode<K,V> node,StringBuilder sb) {
		if (node !=null){
			sb.append(node.key).append(" ");
			preorderHelper(node.left,sb);
			preorderHelper(node.right,sb);
		}
	}

}