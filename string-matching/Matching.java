import java.io.*;
import java.util.*;

public class Matching
{
	private static final int HASH_SIZE =100;
	private static final int SUBSTRING_LENGTH =6;
	private static AVLTree<String, Position>[] hashTable;
	private static List<String> lines = new ArrayList<>();
	static {
		@SuppressWarnings("unchecked")
		AVLTree<String, Position>[] temp =(AVLTree<String, Position>[]) new AVLTree[HASH_SIZE];
		hashTable =temp;
		for(int i=0; i<HASH_SIZE;i++){
			hashTable[i] = new AVLTree<String, Position>();
		}
	}
	public static void main(String args[])
	{
		BufferedReader br= new BufferedReader(new InputStreamReader(System.in));
		while (true)
		{
			try
			{
				String input = br.readLine();
				if (input==null || input.compareTo("QUIT") ==0)
					break;
				command(input);
			}
			catch (IOException e)
			{
				System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
			}
		}
	}
	private static void command(String input)
	{
		if (input.startsWith("< ")){
			String filename =input.substring(2);
			loadFile(filename);
		} else if (input.startsWith("@ ")){
			int idx = Integer.parseInt(input.substring(2));
			printSlot(idx);
		} else if (input.startsWith("? ")){
			String pattern = input.substring(2);
			search(pattern);
		}
	}
	private static int hash(String s){
		int sum = 0;
		for(int i=0; i<s.length(); i++){
			sum += (int) s.charAt(i);
		}
		return (sum % HASH_SIZE + HASH_SIZE) % HASH_SIZE;
	}
	private static void loadFile(String filename){
		for(int i=0;i<HASH_SIZE;i++) {              
			hashTable[i]=new AVLTree<String,Position>();  
		}
		lines.clear();
		try {
			FileReader fr =new FileReader(filename);    
			BufferedReader br =new BufferedReader(fr);
			String line;
			int lineNum=1;                               
			while((line=br.readLine())!=null){          
				lines.add(line);
				processLine(line,lineNum);               
				lineNum++;
			}
			br.close();
		} catch(IOException e) {
			System.out.println("파일 로드 오류");  
			e.printStackTrace();                  
		} }
	private static void processLine(String line, int lineNum){
		int len = line.length();
		for (int i=0;i<=len-SUBSTRING_LENGTH; i++) {
			String sub= line.substring(i,i+SUBSTRING_LENGTH);
			int h =hash(sub);
			Position pos=new Position(lineNum, i + 1);
			AVLTree<String,Position> tree=hashTable[h];
			tree.insert(sub,pos);
		}
	}
	private static void printSlot(int idx){
		if(idx < 0 || idx >= HASH_SIZE){
			System.out.println("EMPTY");
			return;
		}
		if(hashTable[idx].isEmpty()){
			System.out.println("EMPTY");
		} else {
			StringBuilder sb = new StringBuilder();
			hashTable[idx].preorderTraversal(sb);
			System.out.println(sb.toString());
		}
	}
	private static void search(String pattern){
		if(pattern.length() < SUBSTRING_LENGTH){
			System.out.println("(0, 0)");
			return;
		}
		String key = pattern.substring(0, SUBSTRING_LENGTH);
		int h = hash(key);
		List<Position> temp = hashTable[h].search(key);
		if(temp.isEmpty()){
			System.out.println("(0, 0)");
			return;
		}
		List<Position> result =new ArrayList<>();
		for(int i=0; i<temp.size(); i++){
			Position pos=temp.get(i);
			int lineIdx=pos.line-1;
			int colIdx=pos.column-1;
			if(lineIdx >=lines.size()) continue;
			String line=lines.get(lineIdx);
			if (colIdx + pattern.length() <= line.length()){
				String sub = line.substring(colIdx, colIdx + pattern.length());
				if(sub.equals(pattern)){
					result.add(pos);
				}
			}
		}
		if (result.isEmpty()){
			System.out.println("(0, 0)");
		} else {
			StringBuilder sb = new StringBuilder();
			for(int i = 0;i < result.size();i++){
				if(i > 0) sb.append(" ");
				sb.append(result.get(i));
			}
			System.out.println(sb.toString());
		}
	}
}