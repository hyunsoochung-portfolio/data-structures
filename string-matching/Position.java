public class Position {
	int line;
	int column;
	Position(int line, int column){
		this.line=line;
		this.column=column;
	}
	@Override
	public String toString(){
		return "(" + line + ", " +column+ ")";
	}
}