import java.io.*;
import java.util.*;

public class CalculatorTest
{
	public static void main(String args[])
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		while (true)
		{
			try
			{
				String input = br.readLine();
				if (input.compareTo("q") == 0)
					break;

				command(input);
			}
			catch (Exception e)
			{
				System.out.println("ERROR");
			}
		}
	}

	private static void command(String input)
	{
		try
		{
			String postfix;
			// 평균구하는건지 체크
			if (isAverage(input))
			{
				postfix = avgToPostfix(input);
			}
			else
			{
				postfix = infixToPostfix(input);
			}
			long result = calculate(postfix);
			System.out.println(postfix);
			System.out.println(result);
		}
		catch (Exception e)
		{
			System.out.println("ERROR");
		}
	}

	// 평균 연산자인지 확인
	private static boolean isAverage(String expr)
	{
		expr = expr.trim();
		if (!expr.startsWith("(") || !expr.endsWith(")")) return false;
		
		// 괄호 depth 체크하면서 평균연산자인지 확인
		int cnt = 0;
		for (int i = 0; i < expr.length(); i++)
		{
			char c = expr.charAt(i);
			if (c == '(') cnt++;
			else if (c == ')') cnt--;
			else if (c == ',' && cnt == 1) return true;
		}
		return false;
	}

	// 평균 연산자를 postfix로 변환
	private static String avgToPostfix(String expr) throws Exception
	{
		expr = expr.trim();
		if (!expr.startsWith("(") || !expr.endsWith(")"))
			throw new Exception();
		
		String content = expr.substring(1, expr.length() - 1).trim();
		String[] parts = content.split(",");
		
		// 각 숫자를 리스트에 추가
		List<String> result = new ArrayList<>();
		for (int i = 0; i < parts.length; i++)
		{
			String part = parts[i].trim();
			if (part.isEmpty() || !checkNum(part))
				throw new Exception();
			result.add(part);
		}
		result.add(parts.length + "avg");
		String res = String.join(" ", result);
		return res;
	}

	private static boolean checkNum(String str)
	{
		if (str == null || str.isEmpty())
			return false;
		try
		{
			Long.parseLong(str);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	// infix를 postfix로 변환
	private static String infixToPostfix(String infix) throws Exception
	{
		List<String> tokens = tokenize(infix);
		if (tokens.isEmpty())
			throw new Exception();
		
		List<String> output = new ArrayList<>();
		Stack<String> ops = new Stack<>();
		
		boolean needOperand = true;
		
		for (int i = 0; i < tokens.size(); i++)
		{
			String token = tokens.get(i);
			
			if(isNumber(token))
			{
				output.add(token);
				needOperand = false;
			}
			else if (token.equals("("))
			{
				ops.push(token);
				needOperand = true;
			}
			else if (token.equals(")"))
			{
				if (needOperand)
					throw new Exception();
				
				// 왼쪽 괄호 나올때까지 pop
				boolean found = false;
				while (!ops.isEmpty())
				{
					String op = ops.pop();
					if (op.equals("("))
					{
						found = true;
						break;
					}
					output.add(op);
				}
				if (!found)
					throw new Exception();
				
				needOperand = false;
			}
			else if (isOperator(token))
			{
				// unary minus 처리
				if (token.equals("-") && needOperand)
				{
					ops.push("~");
					needOperand = true;
				}
				else
				{
					if (needOperand)
						throw new Exception();
					
					// 우선순위 비교하면서 pop
					while (!ops.isEmpty() && !ops.peek().equals("("))
					{
						String top = ops.peek();
						int p1 = getPriority(top);
						int p2 = getPriority(token);
						
						if (isRight(token))
						{
							if (p1 > p2)
								output.add(ops.pop());
							else
								break;
						}
						else
						{
							if (p1 >= p2)
								output.add(ops.pop());
							else
								break;
						}
					}
					ops.push(token);
					needOperand = true;
				}
			}
			else
			{
				throw new Exception();
			}
		}
		
		if (needOperand)
			throw new Exception();
		
		// 남은 연산자 전부 pop
		while (!ops.isEmpty())
		{
			String op = ops.pop();
			if (op.equals("(") || op.equals(")"))
				throw new Exception();
			output.add(op);
		}
		
		if (output.isEmpty())
			throw new Exception();
		
		return String.join(" ", output);
	}
	
	// 연산자 우선순위 반환
	private static int getPriority(String op)
	{
		switch (op)
		{
			case "+":
			case "-":
				return 1;
			case "*":
			case "/":
			case "%":
				return 2;
			case "~":
				return 3;
			case "^":
				return 4;
			default:
				return 0;
		}
	}

	private static boolean isRight(String op)
	{
		return op.equals("^") || op.equals("~");
	}

	// 문자열을 토큰으로 분리
	private static List<String> tokenize(String expr) throws Exception
	{
		List<String> tokens = new ArrayList<>();
		int i = 0;
		
		while (i < expr.length())
		{
			char c = expr.charAt(i);
			

			if (Character.isWhitespace(c))
			{
				i++;
				continue;
			}
			
			// 숫자일경우
			if (Character.isDigit(c))
			{
				StringBuilder num = new StringBuilder();
				while (i < expr.length() && Character.isDigit(expr.charAt(i)))
				{
					num.append(expr.charAt(i));
					i++;
				}
				tokens.add(num.toString());
			}
			// 연산자, 괄호일 경우
			else if (c == '+' || c == '-' || c == '*' || c == '/' || 
			         c == '%' || c == '^' || c == '(' || c == ')')
			{
				tokens.add(String.valueOf(c));
				i++;
			}
			else
			{
				throw new Exception();
			}
		}
		
		return tokens;
	}

	private static boolean isNumber(String token)
	{
		if (token == null || token.isEmpty())
			return false;
		for (char c : token.toCharArray())
		{
			if (!Character.isDigit(c))
				return false;
		}
		return true;
	}

	private static boolean isOperator(String token)
	{
		return token.equals("+") || token.equals("-") || token.equals("*") ||
		       token.equals("/") || token.equals("%") || token.equals("^");
	}

	// postfix 표현식 계산
	private static long calculate(String postfix) throws Exception
	{
		String[] tokens = postfix.split("\\s+");
		Stack<Long> stack = new Stack<>();
		
		for (String token : tokens)
		{
			// 평균 연산자
			if (token.endsWith("avg"))
			{
				int k = Integer.parseInt(token.substring(0, token.length() - 3));
				if (stack.size() < k)
					throw new Exception();
				
				long sum = 0;
				for (int i = 0; i < k; i++)
				{
					sum += stack.pop();
				}
				stack.push(sum / k);
			}
			// 숫자면 push
			else if (isNumber(token))
			{
				stack.push(Long.parseLong(token));
			}
			// unary minus
			else if (token.equals("~"))
			{
				if (stack.isEmpty())
					throw new Exception();
				long a = stack.pop();
				stack.push(-a);
			}
			// 이항 연산자
			else
			{
				if (stack.size() < 2)
					throw new Exception();
				
				long b = stack.pop();
				long a = stack.pop();
				long result;
				
				switch (token)
				{
					case "+":
						result = a + b;
						break;
					case "-":
						result = a - b;
						break;
					case "*":
						result = a * b;
						break;
					case "/":
						if (b == 0)
							throw new Exception();
						result = a / b;
						break;
					case "%":
						if (b == 0)
							throw new Exception();
						result = a % b;
						break;
					case "^":
						if (a == 0 && b < 0)
							throw new Exception();
						result = (long) Math.pow(a, b);
						break;
					default:
						throw new Exception();
				}
				
				stack.push(result);
			}
		}
		
		if (stack.size() != 1)
			throw new Exception();
		
		return stack.pop();
	}
}
