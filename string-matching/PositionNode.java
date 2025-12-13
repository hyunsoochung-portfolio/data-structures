public class PositionNode<T> {
	T data;
	PositionNode<T> next;
	
	PositionNode(T data){
		this.data=data;
		this.next=null;
	}
}