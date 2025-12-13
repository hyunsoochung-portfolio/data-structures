public class AVLNode<K extends Comparable<K>, V> {
	K key;
	PositionNode<V> valueList; 
	AVLNode<K,V> left,right;
	int height;
	
	AVLNode(K key,V value){
		this.key=key;
		this.valueList = new PositionNode<V>(value);
		this.left =null;
		this.right =null;
		this.height=1;
	}
}