import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
  
public class BigInteger {
    public static final String QUIT_COMMAND = "quit";
    public static final String MSG_INVALID_INPUT = "Wrong Input";
    public static final Pattern EXPRESSION_PATTERN = Pattern.compile(
        "([+-]*)\\s*(\\d+)\\s*([+\\-*])\\s*([+-]*)\\s*(\\d+)"
    );
    private static final int MAX_NUM_LIST = 200;
    private int[] num_list = new int[MAX_NUM_LIST];
    private boolean isNegative;
    

    //연산 메서드에서 연산결과 result를 담을 틀을 만들 생성자 입니다.
    public BigInteger() {    
    isNegative = false;
    num_list[0] = 0;
    }
    
    // 공백이나 부호처리는 전부다 evaluate() 함수에서 할수 있게 조정하여 s는 순수한 숫자의 문자열만 들어오도록 했습니다.
    // 문자열을 받아 각 숫자를 변환하여 num_list라는 int array에 역순으로 넣습니다. 
    public BigInteger(String s) {
        for (int i = 0; i < s.length(); i++) {
            num_list[s.length() - 1 - i] = s.charAt(i) - '0';
        }
    }
    
    public BigInteger add(BigInteger big) {
        BigInteger result = new BigInteger();
        int rest = 0;
        for (int i = 0; i < MAX_NUM_LIST; i++) {
            int sum = this.num_list[i] + big.num_list[i] + rest;
            result.num_list[i] = sum % 10;
            rest = sum / 10;
        }
        return result;
    }
    
    public BigInteger subtract(BigInteger big) {
        BigInteger result = new BigInteger();
        int borrow = 0;
        for (int i = 0; i < MAX_NUM_LIST; i++) {
            int sub = this.num_list[i] - big.num_list[i] - borrow;
            if (sub < 0) {
                sub += 10;
                borrow = 1;
            } else {
                borrow = 0;
            }
            result.num_list[i] = sub;
        }
        return result;
    }
    
    public BigInteger multiply(BigInteger big) {
        BigInteger result = new BigInteger();
        
        for (int i = 0; i < MAX_NUM_LIST; i++) {
            if (this.num_list[i] == 0) continue;
            int[] tempResult = new int[MAX_NUM_LIST];
            int rest = 0;
            
            for (int j = 0; j < MAX_NUM_LIST; j++) {
                if (i + j >= MAX_NUM_LIST) break;
                int mul = this.num_list[i] * big.num_list[j] + rest;
                tempResult[i + j] = mul % 10;
                rest = mul / 10;
            }
            rest = 0;
            for (int k = 0; k < MAX_NUM_LIST; k++) {
                int sum = result.num_list[k] + tempResult[k] + rest;
                result.num_list[k] = sum % 10;
                rest = sum / 10;
            }
        }
        return result;
    }
    
    // 빼기를 수행할 때 마지막에 부호를 정해주기 위해 절대값 비교를 해줍니다. 
    private int compareAbsolute(BigInteger big) { 
        for (int i = MAX_NUM_LIST - 1; i >= 0; i--) {
            if (this.num_list[i] != big.num_list[i]) {
                return this.num_list[i] - big.num_list[i];
            }
        }
        return 0;
    }
    
    
    @Override
    public String toString() {
        int start = MAX_NUM_LIST - 1;
        while (start > 0 && num_list[start] == 0) {
            start--;
        }
        if (start == 0 && num_list[0] == 0) {
            return "0";
        }
        // toString()에 와서 마지막으로 isNegative를 통해 부호를 판단하고 출력하게 했습니다.
        StringBuilder sb = new StringBuilder();
        if (isNegative) {
            sb.append('-');
        }
        for (int i = start; i >= 0; i--) {
            sb.append(num_list[i]);
        }
        return sb.toString();
    }
    
    static BigInteger evaluate(String input) throws IllegalArgumentException {
        input = input.replaceAll("\\s+", "");
        
        Matcher matcher = EXPRESSION_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid expression format");
        }
        
        String signs1 = matcher.group(1);
        String num1Str = matcher.group(2);
        String operator = matcher.group(3);
        String signs2 = matcher.group(4);
        String num2Str = matcher.group(5);
        
        
        boolean isNeg1 = false;
        boolean isNeg2 = false;
        
        //처음받을때부터 연산자들 중복을 각각의 부호를 boolean형태로 바로 처리해서 결국 최종적으로 갖고 연산해야 하는 숫자의 부호를 처음부터 파악하고 시작했습니다.
        for (char c : signs1.toCharArray()) {
            if (c == '-') isNeg1 = !isNeg1;
        }
        
        for (char c : signs2.toCharArray()) {
            if (c == '-') isNeg2 = !isNeg2;
        }
        
        
        BigInteger num1 = new BigInteger(num1Str);
        BigInteger num2 = new BigInteger(num2Str);
        num1.isNegative = isNeg1;
        num2.isNegative = isNeg2;
        
        BigInteger result;
        
        
        if (operator.equals("*")) {
            result = num1.multiply(num2);
            result.isNegative = (isNeg1 != isNeg2);
        } 
        // 결국 해야하는 연산이 덧셈인지, 뺄셈인지를 shouldAdd를 통해 먼저 구분했습니다. ( operator가 -여도 num1과 num2가 같은 부호면 덧셈을 진행합니다.) 
        // 이후 절댓값끼리 연산을 한후에 최종 부호는 compareAbsolute 메서드와 isNegative를 통해 마지막에 toString()에서 따로 처리했습니다.
        else {
            boolean shouldAdd = (operator.equals("+")) ? (isNeg1 == isNeg2) : (isNeg1 != isNeg2);
            
            if (shouldAdd) {
                result = num1.add(num2);
                result.isNegative = isNeg1; // 같은부호끼리(+,+)or(-,-) 더했을 때의 어차피 최종결과는 num1의 부호를 따라가게 되어있기 때문입니다.
            } else {
                int compare_Abs = num1.compareAbsolute(num2);
                if (compare_Abs == 0) {
                    result= new BigInteger();
                } else if (compare_Abs > 0) {
                    result = num1.subtract(num2);
                    result.isNegative = isNeg1;
                } else {
                    result = num2.subtract(num1); 
                    result.isNegative = (operator.equals("+")) ? isNeg2 : !isNeg2; 
                }
            }
        }
        
        return result;
    }
    
    public static void main(String[] args) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            boolean done = false;
            while (!done) {
                String input = br.readLine();
                try {
                    done = processInput(input);
                } catch (IllegalArgumentException e) {
                    System.err.println(MSG_INVALID_INPUT);
                }
            }
        }
    }
    
    static boolean processInput(String input) throws IllegalArgumentException {
        if (isQuitCmd(input)) {
            return true;
        }
        System.out.println(evaluate(input));
        return false;
    }
    
    static boolean isQuitCmd(String input) {
        return input.equalsIgnoreCase(QUIT_COMMAND);
    }
}
