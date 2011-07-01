package util;

import org.junit.Test;


public class LevenshteinDistanceTest {

	String str1 = "LAWYEdQKPGTT";
	String str2 = "LAWYQQKPGKA";
	
	@Test
	public void test1(){
		
		System.out.println(LevenshteinDistance.computeLevenshteinDistance(str1, str2));
	}
	
	@Test
	public void test2(){
		
		System.out.println(Distance.LD(str1, str2));
	}
}
