import java.io.*;
import java.util.*;

public class SortingTest
{
	public static void main(String args[])
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try
		{
			boolean isRandom = false;	// 입력받은 배열이 난수인가 아닌가?
			int[] value;	// 입력 받을 숫자들의 배열
			String nums = br.readLine();	// 첫 줄을 입력 받음
			if (nums.charAt(0) == 'r')
			{
				// 난수일 경우
				isRandom = true;	// 난수임을 표시

				String[] nums_arg = nums.split(" ");

				int numsize = Integer.parseInt(nums_arg[1]);	// 총 갯수
				int rminimum = Integer.parseInt(nums_arg[2]);	// 최소값
				int rmaximum = Integer.parseInt(nums_arg[3]);	// 최대값

				Random rand = new Random();	// 난수 인스턴스를 생성한다.

				value = new int[numsize];	// 배열을 생성한다.
				for (int i = 0; i < value.length; i++)	// 각각의 배열에 난수를 생성하여 대입
					value[i] = rand.nextInt(rmaximum - rminimum + 1) + rminimum;
			}
			else
			{
				// 난수가 아닐 경우
				int numsize = Integer.parseInt(nums);

				value = new int[numsize];	// 배열을 생성한다.
				for (int i = 0; i < value.length; i++)	// 한줄씩 입력받아 배열원소로 대입
					value[i] = Integer.parseInt(br.readLine());
			}

			// 숫자 입력을 다 받았으므로 정렬 방법을 받아 그에 맞는 정렬을 수행한다.
			while (true)
			{
				int[] newvalue = (int[])value.clone();	// 원래 값의 보호를 위해 복사본을 생성한다.
                char algo = ' ';

				if (args.length == 4) {
                    return;
                }

				String command = args.length > 0 ? args[0] : br.readLine();

				if (args.length > 0) {
                    args = new String[4];
                }
				
				long t = System.currentTimeMillis();
				switch (command.charAt(0))
				{
					case 'B':	// Bubble Sort
						newvalue = DoBubbleSort(newvalue);
						break;
					case 'I':	// Insertion Sort
						newvalue = DoInsertionSort(newvalue);
						break;
					case 'H':	// Heap Sort
						newvalue = DoHeapSort(newvalue);
						break;
					case 'M':	// Merge Sort
						newvalue = DoMergeSort(newvalue);
						break;
					case 'Q':	// Quick Sort
						newvalue = DoQuickSort(newvalue);
						break;
					case 'R':	// Radix Sort
						newvalue = DoRadixSort(newvalue);
						break;
					case 'S':	// Search
						algo = DoSearch(newvalue);
						break;
					case 'X':
						return;	// 프로그램을 종료한다.
					default:
						throw new IOException("잘못된 정렬 방법을 입력했습니다.");
				}
				if (isRandom)
				{
					// 난수일 경우 수행시간을 출력한다.
					System.out.println((System.currentTimeMillis() - t) + " ms");
				}
				else
				{
					// 난수가 아닐 경우 정렬된 결과값을 출력한다.
                    if (command.charAt(0) != 'S') {
                        for (int i = 0; i < newvalue.length; i++) {
                            System.out.println(newvalue[i]);
                        }
                    } else {
                        System.out.println(algo);
                    }
				}

			}
		}
		catch (IOException e)
		{
			System.out.println("입력이 잘못되었습니다. 오류 : " + e.toString());
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoBubbleSort(int[] value)
	{
		int n = value.length;
		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < n - 1 - i; j++) {
				if (value[j] > value[j + 1]) {
					int temp = value[j];
					value[j] = value[j + 1];
					value[j + 1] = temp;
				}
			}
		}
		return value;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoInsertionSort(int[] value)
	{
		int n = value.length;
		for (int i = 1; i < n; i++) {
			int key = value[i];
			int j = i - 1;
			while (j >= 0 && value[j] > key) {
				value[j + 1] = value[j];
				j--;
			}
			value[j + 1] = key;
		}
		return value;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoHeapSort(int[] value)
	{
		int n = value.length;
		
		// Build max heap
		for (int i = n / 2 - 1; i >= 0; i--) {
			build_heap(value, n, i);
		}
		
		// Extract elements from heap one by one
		for (int i = n - 1; i > 0; i--) {
			int temp = value[0];
			value[0] = value[i];
			value[i] = temp;
			
			build_heap(value, i, 0);
		}
		
		return value;
	}
	
	private static void build_heap(int[] arr, int n, int i) {
		int largest = i;
		int left = 2 * i + 1;
		int right = 2 * i + 2;
		
		if (left < n && arr[left] > arr[largest]) {
			largest = left;
		}
		
		if (right < n && arr[right] > arr[largest]) {
			largest = right;
		}
		
		if (largest != i) {
			int swap = arr[i];
			arr[i] = arr[largest];
			arr[largest] = swap;
			
			build_heap(arr, n, largest);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoMergeSort(int[] value)
	{
		if (value.length <= 1) {
			return value;
		}
		
		mergeSortHelper(value, 0, value.length - 1);
		return value;
	}
	
	private static void mergeSortHelper(int[] arr, int left, int right) {
		if (left < right) {
			int mid = left + (right - left) / 2;
			
			mergeSortHelper(arr, left, mid);
			mergeSortHelper(arr, mid + 1, right);
			
			merge(arr, left, mid, right);
		}
	}
	
	private static void merge(int[] arr, int left, int mid, int right) {
		int n1 = mid - left + 1;
		int n2 = right - mid;
		
		int[] Left = new int[n1];
		int[] Right = new int[n2];
		
		for (int i = 0; i < n1; i++) {
			Left[i] = arr[left + i];
		}
		for (int j = 0; j < n2; j++) {
			Right[j] = arr[mid + 1 + j];
		}
		
		int i = 0, j = 0, k = left;
		
		while (i < n1 && j < n2) {
			if (Left[i] <= Right[j]) {
				arr[k] = Left[i];
				i++;
			} else {
				arr[k] = Right[j];
				j++;
			}
			k++;
		}
		
		while (i < n1) {
			arr[k] = Left[i];
			i++;
			k++;
		}
		
		while (j < n2) {
			arr[k] = Right[j];
			j++;
			k++;
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoQuickSort(int[] value)
	{
		if (value.length <= 1) {
			return value;
		}
		
		quickSortHelper(value, 0, value.length - 1);
		return value;
	}
	
	private static void quickSortHelper(int[] arr, int low, int high) {
		if (low < high) {
			int k = partition(arr, low, high);
			
			quickSortHelper(arr, low, k - 1);
			quickSortHelper(arr, k + 1, high);
		}
	}
	
	private static int partition(int[] arr, int low, int high) {
		int pivot = arr[high];
		int i = low - 1;
		
		for (int j = low; j < high; j++) {
			if (arr[j] < pivot) {
				i++;
				int temp = arr[i];
				arr[i] = arr[j];
				arr[j] = temp;
			}
		}
		
		int temp = arr[i + 1];
		arr[i + 1] = arr[high];
		arr[high] = temp;
		
		return i + 1;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static int[] DoRadixSort(int[] value)
	{
		if (value.length == 0) return value;
		
		// 음수 처리를 위해 배열을 양수/음수로 분리
		ArrayList<Integer> negatives = new ArrayList<>();
		ArrayList<Integer> positives = new ArrayList<>();
		
		for (int num : value) {
			if (num < 0) {
				negatives.add(-num); // 절댓값으로 변환
			} else {
				positives.add(num);
			}
		}
		
		// 음수 부분 radix sort (내림차순으로 정렬되어야 함)
		if (negatives.size() > 0) {
			int[] negative_Array = new int[negatives.size()];
			for (int i = 0; i < negatives.size(); i++) {
				negative_Array[i] = negatives.get(i);
			}
			radixSortHelper(negative_Array);
			
			// 결과를 다시 음수로 변환하고 역순으로
			for (int i = negative_Array.length - 1; i >= 0; i--) {
				negatives.set(negative_Array.length - 1 - i, -negative_Array[i]);
			}
		}
		
		// 양수 부분 radix sort
		if (positives.size() > 0) {
			int[] positive_Array = new int[positives.size()];
			for (int i = 0; i < positives.size(); i++) {
				positive_Array[i] = positives.get(i);
			}
			radixSortHelper(positive_Array);
			
			for (int i = 0; i < positive_Array.length; i++) {
				positives.set(i, positive_Array[i]);
			}
		}
		
		// 결합
		int i = 0;
		for (int num : negatives) {
			value[i++] = num;
		}
		for (int num : positives) {
			value[i++] = num;
		}
		
		return value;
	}
	
	private static void radixSortHelper(int[] arr) {
		int max = arr[0];
		for (int num : arr) {
			if (num > max) max = num;
		}
		
		for (int exp = 1; max / exp > 0; exp *= 10) {
			countingSortByDigit(arr, exp);
		}
	}
	
	private static void countingSortByDigit(int[] arr, int exp) {
		int n = arr.length;
		int[] output = new int[n];
		int[] count = new int[10];
		
		for (int i = 0; i < n; i++) {
			count[(arr[i] / exp) % 10]++;
		}
		
		for (int i = 1; i < 10; i++) {
			count[i] += count[i - 1];
		}
		
		for (int i = n - 1; i >= 0; i--) {
			int digit = (arr[i] / exp) % 10;
			output[count[digit] - 1] = arr[i];
			count[digit]--;
		}
		
		for (int i = 0; i < n; i++) {
			arr[i] = output[i];
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	private static char DoSearch(int[] value)
	{
		int n = value.length;
		
		// 1. 최대 자릿수 확인
		int maxDigits = getMaxDigits(value);
		if (maxDigits <= 4) {  // 자릿수가 4 이하면 Radix Sort
			return 'R';
		}
		
		// 2. 중복도 확인
		double collisionRate = getCollisionRate(value);
		if (collisionRate > 0.75) {  // 충돌률이 75% 이상이면 중복이 많다고 판단
			return 'M';  // Merge Sort호출
		}
		
		// 3. 정렬도 확인 - 이미 정렬된 정도 측정
		double sortedRatio = getSortedRatio(value);
		if (sortedRatio > 0.9) {  // 90% 이상 정렬되어 있으면
			return 'I';  // Insertion Sort 호출
		}
		
		// 4. 배열 크기에 따른 선택
		if (n < 80) {
			return 'I';  // 작은 배열은 Insertion Sort
		}
		
		// 5. 기본적으로 Quick Sort (평균적으로 가장 빠름)
		return 'Q';
	}
	
	// 최대 자릿수 계산
	private static int getMaxDigits(int[] arr) {
		int max = 0;
		for (int num : arr) {
			int absNum = Math.abs(num);
			if (absNum > max) max = absNum;
		}
		
		if (max == 0) return 1;
		
		int digits = 0;
		while (max > 0) {
			max /= 10;
			digits++;
		}
		return digits;
	}
	
	// 충돌률 계산
	private static double getCollisionRate(int[] arr) {
		int tableSize = arr.length;
		boolean[] hashTable = new boolean[tableSize];
		int collisions = 0;
		
		for (int num : arr) {
			int hash = Math.abs(num % tableSize);
			if (hashTable[hash]) {
				collisions++;
			} else {
				hashTable[hash] = true;
			}
		}
		
		return (double) collisions / arr.length;
	}
	
	// 정렬도 계산
	private static double getSortedRatio(int[] arr) {
		if (arr.length <= 1) return 1.0;
		
		int Pairs = 0;
		for (int i = 0; i < arr.length - 1; i++) {
			if (arr[i] <= arr[i + 1]) {
				Pairs++;
			}
		}
		
		return (double) Pairs / (arr.length - 1);
	}
}