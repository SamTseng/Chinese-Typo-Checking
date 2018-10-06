package l2.spark.tokenizer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import l2.spark.tokenizer.utils.HPath;
import l2.spark.tokenizer.utils.T2;
import scala.Tuple2;
 

public class demo {
	public static HPath hp = new HPath(new File("/home/datas/tokens"), 2);
	public static JavaRDD<String> testRDD = null;
	public static JavaRDD<String> wordRDD = null;	
	public static Map<String,String> RPMap = new HashMap<String,String>();
	public static Set<String> PPSet = new TreeSet<String>();
	
	static {
		RPMap.put("{NUM}", "[.0-9]+");
		RPMap.put("{TEL}", "[0-9]+-[0-9]+");
		RPMap.put("{SUBJECT}", ".{1,5}"); PPSet.add("{SUBJECT}");
		RPMap.put("{OBJECT}", ".{1,5}"); PPSet.add("{OBJECT}");
		RPMap.put("{PEOPLE}", ".{2,5}"); PPSet.add("{PEOPLE}");
		RPMap.put("{NAME}", ".{2,5}"); PPSet.add("{NAME}");
		RPMap.put("{BRAND}", ".{2,5}"); PPSet.add("{BRAND}");
		RPMap.put("{NOUN}", ".{2,6}"); PPSet.add("{NOUN}");
		RPMap.put("{LOC}", ".{1,6}"); PPSet.add("{LOC}");
		RPMap.put("{ADJ}", ".{1,6}"); PPSet.add("{ADJ}");
		RPMap.put("{LOCATION}", ".{2,6}"); PPSet.add("{LOCATION}");
		RPMap.put("{PERT}", "[一二三四五六七八九十0-9]成[一二三四五六七八九十0-9]?");
		RPMap.put("{ANUM}", "[.0-9一二三四五六七八九十百廿千萬億兆]+");
		RPMap.put("{CNUM}", "[零壹貳參肆伍陸柒捌玖拾佰仟一二三四五六七八九十廿百千萬億兆]+");
		RPMap.put("{MONTH}", "[元0-9一二三四五六七八九十]+月份?");
		RPMap.put("{DAY}", "[0-9一二三四五六七八九十廿]+日");
		RPMap.put("{WDAY}", "[週周][0-9一二三四五六七]|星期[0-9一二三四五六七]");
		RPMap.put("{RANK}", "第[0-9一二三四五六七八九十廿千萬億兆]+");
		RPMap.put("{YEAR}", "[一二三四五六七八九十廿0-9]+年代?");
		RPMap.put("{HOUR}", "[0-9]+時");
		RPMap.put("{MINUTE}", "[0-9]+分鐘?");
	}
	
	public static boolean IsNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	public static class ReduceSumFunc implements Function2<Integer, Integer, Integer>{
		@Override
		public Integer call(Integer accum, Integer n) throws Exception {
			return accum + n;
		}
	}
	
	public static class ReduceDSumFunc implements Function2<Double, Double, Double>{

		@Override
		public Double call(Double accum, Double n) throws Exception {
			return accum + n;
		}		
	}
	
	public static class ReduceNSumFunc implements Function2<Double, Double, Double>{
		@Override
		public Double call(Double accum, Double n) throws Exception {
			return accum;
		}
	}
	
	protected static List<String> _b2s(String sent){
		List<String> bs_list = new ArrayList<String>();
		for(int i=0; i<sent.length(); i++) bs_list.add(sent.substring(i, i+1));
		return bs_list;
	}
	
	protected static double _sim_of_token(String etoken, String otoken)
	{
		if(etoken.length()!=otoken.length()) return 0.0;
		if(etoken.length()==1) return 1.0;
		
		List<String> etoken_c_list = _b2s(etoken);
		List<String> otoken_c_list = _b2s(otoken);
		double hc = 0.0;
		//for(String bs:etoken_c_list)
		//{
		//	if(otoken_c_list.contains(bs)) hc +=1;
		//}
		
		for(int i=0; i<otoken_c_list.size(); i++)
		{
			if(otoken_c_list.get(i).equals(etoken_c_list.get(i))) hc+=1;
		}
		
		try {
			double sim = hc / otoken_c_list.size();			
			return sim; 
		} catch(Exception e) {return 0.0;}
	}
	
	public static class FakeCollRDD{
		String ptoken = null;
		Map<String, Integer> rstMap = null;
		
		public FakeCollRDD(String ptoken, Map<String, Integer> rstMap)
		{
			this.ptoken = ptoken;
			this.rstMap = rstMap;
		}
		
		public List<Tuple2> collect() {
			List<Tuple2> rstList = new ArrayList<Tuple2>();			
			for(Map.Entry<String,Integer> e: rstMap.entrySet())
			{
				rstList.add(new Tuple2(new Tuple2(ptoken, e.getKey()), e.getValue()));
			}
			
			return rstList;
		}
	}
	
	public static FakeCollRDD look2CollLocalF(String etokenAsHead, String ntoken)
	{
		try
		{
			Map<String, Integer> rstMap = new HashMap<String,Integer>();			
			Map<String, T2> pkMap = hp.readToken(ntoken);		
			for(String ht:pkMap.getOrDefault(ntoken, new T2(ntoken)).fSet)
			{
				if(ht.length()==etokenAsHead.length())
				{
					//System.out.printf("\t\t\tCheck %s...\n", ht);
					if(_sim_of_token(ht, etokenAsHead) >= 0.5 || ht.length()==1) rstMap.put(ht, 1);
				}
			}
			return new FakeCollRDD(null, rstMap);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public static FakeCollRDD look2CollLocal(String ptoken, String etoken, String ntoken)
	{
		try
		{
			
			if(ptoken!=null)
			{
				if(ptoken.startsWith("+") || ptoken.startsWith("-") || ptoken.startsWith("~"))
				{
					ptoken = ptoken.substring(1, ptoken.length());
				}
				if(ptoken.contains(":"))
				{
					ptoken = ptoken.split(":")[1];
				}
			}
						
			Map<String, Integer> rstMap = new HashMap<String,Integer>();			
			if(ptoken!=null)
			{
				Map<String, T2> pkMap = hp.readToken(ptoken);
				//System.out.printf("\t\t%s %s:\n", ptoken, etoken);
				T2 t2 = pkMap.getOrDefault(ptoken, new T2(ptoken));
				if(!t2.bSet.contains(etoken))
				{
					for(String ht:pkMap.getOrDefault(ptoken, new T2(ptoken)).bSet)
					{					
						if(ht.length()==etoken.length())
						{
							if(ht.equals(etoken))
							{
								
							}
							
							if(_sim_of_token(ht, etoken) >= 0.5 || ht.length()==1) {
								if(ntoken!=null)
								{
									Map<String, T2> nkMap = hp.readToken(ntoken);
									t2 = nkMap.get(ntoken);
									if(t2!=null && !t2.fSet.contains(ht)) continue;
								}
								//System.out.printf("\t\t%s (%s,%s)...\n", ptoken, etoken, ht);
								rstMap.put(ht, 1);
							}
						}					
					}
				}				
			}
			else
			{
				Set<String> hset = hp.readHToken(etoken);
				for(String ht:hset)
				{
					//System.out.printf("\t\tHead(%s,%s)...\n", etoken, ht);
					if(ht.length()==etoken.length())
					{
						if(_sim_of_token(ht, etoken) >= 0.5 || ht.length()==1) rstMap.put(ht, 1);
					}
				}
			}
			
			return new FakeCollRDD(ptoken, rstMap);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Look for suggestion token by collocation.
	 * @param ptoken
	 * @param etoken
	 * @return
	 * 	JavaPairRDD<Tuple2,Integer>: 
	 * 		- _1: Tuple2 with _1 as ptoken; _2 as collocation token after ptoken
	 * 		- _2: Count
	 */
	public static JavaPairRDD<String,Integer> look4Coll(String ptoken, String etoken) {		 
		if(ptoken!=null)
		{
			if(ptoken.startsWith("+") || ptoken.startsWith("-") || ptoken.startsWith("~"))
			{
				ptoken = ptoken.substring(1, ptoken.length());
			}
		}
		
		class FilterPtoken implements org.apache.spark.api.java.function.Function<String, Boolean>{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private String ptoken = null;
			
			public FilterPtoken(String ptoken)
			{
				this.ptoken = ptoken;
			}
			
			@Override
			public Boolean call(String token_line) throws Exception {
				List<String> token_list = Arrays.asList(token_line.split(" "));
				if(this.ptoken!=null) return token_list.contains(this.ptoken);
				else 
				{
					return true;
				}
			}
		}
		
		class FilterSimUp implements Function<Tuple2<String, Integer>, Boolean>{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private String etoken = null;
			
			public FilterSimUp(String etoken)
			{
				this.etoken = etoken;
			}

			public List<String> b2s(String sent){
				List<String> bs_list = new ArrayList<String>();
				for(int i=0; i<sent.length(); i++) bs_list.add(sent.substring(i, i+1));
				return bs_list;
			}
			public double sim_of_token(String etoken, String otoken)
			{
				if(etoken.length()!=otoken.length()) return 0.0;
				if(etoken.length()==1) return 1.0;
				
				List<String> etoken_c_list = b2s(etoken);
				List<String> otoken_c_list = b2s(otoken);
				double hc = 0.0;
				for(String bs:etoken_c_list)
				{
					if(otoken_c_list.contains(bs)) hc +=1;
				}
				
				try {
					double sim = hc / otoken_c_list.size();
					//System.out.printf("\t\t\t%s to %s with sim = %.02f\n", etoken, otoken, sim);
					return sim; 
				} catch(Exception e) {return 0.0;}
			}
			
			@SuppressWarnings("rawtypes")
			@Override
			public Boolean call(Tuple2 t2) throws Exception {
				Tuple2 t = (Tuple2)t2._1;
				Integer s = (Integer)t2._2;
				String stoken = (String)t._2;
				if(stoken != null && etoken.length() == stoken.length())
				{
					return sim_of_token(etoken, stoken) >= 0.5;
				}
				return false;
			}
		}
		
		class CollCheck implements PairFunction<String,String,Integer>{
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private String ptoken = null;
			
			public CollCheck(String ptoken)
			{
				this.ptoken = ptoken;
			}

			@Override
			public Tuple2<String, Integer> call(String token_line) throws Exception {
				List<String> token_list = Arrays.asList(token_line.split(" "));
				long token_list_len = token_list.size();
				
				if(ptoken == null)
				{
					return new Tuple2(new Tuple2(null, token_list.get(0)), 1);
				}
				else
				{
					for(int i=0; i<token_list_len; i++)
					{
						String token = token_list.get(i);
						if(token.equals(ptoken) && i+1 < token_list_len)
						{
							String ntoken = token_list.get(i+1);
							//System.out.printf("\t\tNext token=%s\n", ntoken);
							return new Tuple2(new Tuple2(ptoken, ntoken), 1); 
						}
					}
				}
				return new Tuple2(new Tuple2(ptoken, null), 0);
			}
			
		}
		
		
		if(ptoken!=null)
		{
			JavaRDD<String> tmpRDD = testRDD.filter(new FilterPtoken(ptoken));
			//System.out.printf("\tGot %d sentences from ptoken=%s...\n", tmpRDD.count(), ptoken);
			if(tmpRDD.count() > 0)
			{
				// rstRDD = tmpRDD.map(collCheck).reduceByKey(lambda a, b: a + b).filter(filterSimUp)
				return tmpRDD.mapToPair(new CollCheck(ptoken)).reduceByKey(new ReduceSumFunc()).filter(new FilterSimUp(etoken));
			}
			else
			{
				return null;
			}
		}
		else
		{			
			return testRDD.mapToPair(new CollCheck(ptoken)).reduceByKey(new ReduceSumFunc()).filter(new FilterSimUp(etoken));
		}
	}
	
	public static class Candi implements Comparable<Candi>{
		public List<String> final_token_list = null;
		public String source_sent = null;
		public double score = -1;
		
		public Candi(List<String> final_token_list, String source_sent, double score)
		{
			this.final_token_list = final_token_list;
			this.source_sent = source_sent;
			this.score = score;
		}
		
		public double getScore() {return this.score;}

		@Override
		public int compareTo(Candi oc) {
			if(score > oc.getScore()) return -1;
			else if(score < oc.getScore()) return 1;
			else return 0;
		}
	}
	
	public static class CandiCmp implements Comparator<Candi>{

		@Override
		public int compare(Candi o1, Candi o2) {
			return o1.compareTo(o2);
		}
		
	}

	public static class Colli implements Comparable<Colli>{
		String ptoken;
		String ntoken;
		int count;
		
		public Colli(String p, String n, int c)
		{
			this.ptoken = p; this.ntoken = n; this.count = c;
		}

		@Override
		public int compareTo(Colli o) {
			if(count > o.count) return -1;
			else if(count < o.count) return 1;
			else return 0;
		}
	}
	
	public static class ColliCmp implements Comparator<Colli>{
		@Override
		public int compare(Colli o1, Colli o2) {
			return o1.compareTo(o2);
		}
	}
	
	/**
	 * Check if the token being seen.
	 * @param token
	 * @return
	 */
	public static boolean isTokenSeen(String token)
	{
		// return wordRDD.filter(lambda e: e.encode('utf-8')==token).count() > 0
		return wordRDD.filter((Function<String,Boolean>)t->{return t.equals(token);}).count()>0;
	}
	
	public static boolean isPrvTokenSeen(String ptoken, String ctoken)
	{
		try
		{
			Map<String,T2> tkMap = hp.readToken(ctoken);
			T2 t2 = tkMap.getOrDefault(ctoken, new T2(ctoken));
			return t2.fSet.contains(ptoken);
		}
		catch(Exception e) {}
		return false;
	}
	
	public static boolean isNexTokenSeen(String ctoken, String ntoken)
	{
		try
		{
			Map<String,T2> tkMap = hp.readToken(ctoken);
			T2 t2 = tkMap.getOrDefault(ctoken, new T2(ctoken));
			return t2.bSet.contains(ntoken);
		}
		catch(Exception e) {}
		return false;
	}
	
	
	public static boolean isTokenSeenLocal(String token)
	{
		try
		{
			Map<String,T2> tkMap = hp.readToken(token);
			if(tkMap.containsKey(token)) return true;
			Set<String> hset = hp.readHToken(token);
			if(hset.contains(token)) return true;
			
			return false;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static class TokenCheck implements PairFunction<String,Tuple2,Double>, Serializable
	{
		private static final long serialVersionUID = 1L;
		String target_sent;
		
		public TokenCheck(String target_sent) {
			this.target_sent = target_sent;
		}
		
		@Override
		public Tuple2<Tuple2, Double> call(String token_line) throws Exception {
			token_line = token_line.trim();
			String token_orig_line = token_line.replaceAll(" ", "");
			double sim_score = 0;
			
			if(token_orig_line.length()==this.target_sent.length())
			{
				for(int i=0; i<token_orig_line.length();i++)
				{
					if(token_orig_line.substring(i, i+1).equals(this.target_sent.substring(i, i+1))) sim_score+=0.2;
				}						
			}
			
			List<String> token_list = Arrays.asList(token_line.split(" "));
			List<String> result_token_list = new ArrayList<String>();
			int mc = 0;  						// Missing count
			int hc = 0;  						// Hit count
			int pi = -1;						// Position index
			String sent = this.target_sent;		// Target sentence
			Tuple2<String,String> phdr = null;  // Placeholder
			
			if(token_line.isEmpty())
			{
				return new Tuple2(new Tuple2(String.join(" ", result_token_list), token_line), -10.0);
			}
			
			int tc = 0;							// Token count
			boolean isPHit = false;
			for(String token:token_list)
			{
				int token_len = token.length();
				tc += 1;
				if(token_len==0)
				{
					return new Tuple2(new Tuple2(String.join(" ", result_token_list), token_line), -10.0);
				}
				
				try 
				{
					if(token.startsWith("r:"))
					{
						String ptnStr = token.substring(2, token.length());
						Pattern p = Pattern.compile(ptnStr);
						Matcher mth = p.matcher(sent);
						if(mth.find())
						{							
							token = mth.group(0);
							token_len = token.length();
							pi = sent.indexOf(token);							
						}
						else
						{									
							throw new Exception("Not match");
						}
					}
					else if(token.startsWith("{") && token.endsWith("}")) {
						if(PPSet.contains(token))
						{							
							if(tc == token_list.size() && isPHit)
							{
								String rtoken = RPMap.get(token);
								
								Pattern p = Pattern.compile(rtoken);
								Matcher mth = p.matcher(sent);
								if(mth.find() && mth.group(0).length()==sent.length())
								{
									String mtoken = mth.group(0);
									token_len = mtoken.length();
									pi = 0;
									token = String.format("%s:%s", mtoken, token);
								}
							}
							else
							{
								phdr = new Tuple2<String,String>(token, RPMap.get(token));
								continue;
							}							
						}
						else
						{
							String rtoken = RPMap.get(token);
							Pattern p = Pattern.compile(rtoken);
							Matcher mth = p.matcher(sent);
							if(mth.find())
							{							
								String mtoken = mth.group(0);
								token_len = mtoken.length();
								pi = sent.indexOf(mtoken);
								token = String.format("%s:%s", mtoken, token);							
							}
							else
							{							
								throw new Exception("Not match");
							}
						}
					}
					else
					{
						pi = sent.indexOf(token);
					}
					
					if(pi<0) 
					{
						mc += token.length();
						throw new Exception("Not match");
					}
					else if(pi>0)
					{
						isPHit = true;
						hc += 1;
						String missFrontToken = sent.substring(0, pi);
						if(phdr!=null)
						{							
							String type = phdr._1;
							String rtoken = phdr._2;
							//System.out.printf("MFT=%s; type=%s; rtoken=%s...\n", missFrontToken, type, rtoken);
							Pattern p = Pattern.compile(rtoken);
							Matcher mth = p.matcher(missFrontToken);
							if(mth.find() && mth.group(0).length()==missFrontToken.length())
							{	
								hc += 2;
								result_token_list.add(String.format("+%s:%s", missFrontToken, type));
							}
							else
							{
								result_token_list.add(String.format("*%s", missFrontToken));
							}
						}
						else
						{
							result_token_list.add(String.format("*%s", missFrontToken));
						}
						result_token_list.add(String.format("+%s", token));
						sent = sent.substring(pi+token_len);						
					}
					else
					{			
						isPHit = true;
						hc += 2;
						result_token_list.add(String.format("+%s", token));
						sent = sent.substring(pi+token_len);
					}
					
					// Clean flag/status
					phdr = null;
				}
				catch(Exception e)
				{
					isPHit = false;
					phdr = null;
					mc += 1;
				}
				
				if(sent.length()==0) break;
			}
			
			if(sent.length()>0)
			{
				mc += sent.length();
				result_token_list.add(String.format("*%s", sent));
			}
			
			double score = hc - 0.2 * mc + sim_score;	
			return new Tuple2(new Tuple2(String.join(" ", result_token_list), token_line), score);
		}

	}
	
	public static class Map2Tokenize implements PairFunction<String,Tuple2,Double>
	{
		String sub_line;
		
		public Map2Tokenize(String sub_line) {this.sub_line = sub_line;}

		@Override
		public Tuple2<Tuple2, Double> call(String token_line) throws Exception {
			token_line = token_line.trim();
			
			
			List<String> token_list = Arrays.asList(token_line.split(" "));
			List<String> result_token_list = new ArrayList<String>();
			int mc = 0;  					// Missing count
			int hc = 0;						// Hit count
			int pi = 0;						// Point index of current token in sent
			String sent = this.sub_line;	// Target sentence to look4matching
			
			
			if(token_line.isEmpty())
			{
				return new Tuple2(new Tuple2(String.join(" ", result_token_list), token_line), -10.0);
			}
			
			for(String token:token_list)
			{
				try {
					int token_len = token.length();
					if(token.startsWith("r:"))
					{
						Pattern ptn = Pattern.compile(token.substring(2));
						Matcher mth = ptn.matcher(sent);
						if(mth.find())
						{
							token = mth.group(0);
							token_len = token.length();
							pi = sent.indexOf(token);
						}
						else
						{
							throw new Exception("Not match");
						}						
					}
					else
					{
						pi = sent.indexOf(token);
					}
					
					if(pi<0) 
					{
						throw new Exception("Not match");
					}
					
					hc += 1;
					if(pi > 0)
					{
						String ctoken = sent.substring(0, pi);
						result_token_list.add(String.format("*%s", ctoken));
						result_token_list.add(token);
						sent = sent.substring(pi + token_len);
					}
					else
					{
						hc += 1;
						result_token_list.add(token);
						sent = sent.substring(pi + token_len);
					}
					
					if(sent.length()==2) break;
				}
				catch(Exception e){
					mc += 1;
				}
			}
			
			if(sent.length()>0)
			{
				result_token_list.add(String.format("*%s", sent));
			}
						
			return new Tuple2(new Tuple2(String.join(" ", result_token_list), token_line), hc-0.1*mc);
		}
		
		
	}
	
	public static List<String> tokenize_local(String sub_line, int csize)
	{
		//System.out.printf("\tSubCheck: %s...\n", sub_line);
		List<String> final_token_list = new ArrayList<String>();
		for(int i=1; i<=csize; i++)
		{
			String head = sub_line.substring(0, i);
			String tail = sub_line.substring(i);
			//System.out.printf("\t\t%s\t%s\n", head, tail);
			try {
				Map<String, T2> pkMap = hp.readToken(head);
				T2 t2 = pkMap.getOrDefault(head, new T2(head));
				if(t2.bSet.contains(tail))
				{
					final_token_list.add(String.format("-%s", head));
					final_token_list.add(String.format("-%s", tail));
					break;
				}
			}
			catch(Exception e) {}
		}
		return final_token_list;
	}
	
	public static List<String> tokenize(String sub_line)
	{
		JavaPairRDD<Tuple2,Double> rstRDD = testRDD.mapToPair(new Map2Tokenize(sub_line)).reduceByKey(new ReduceDSumFunc());
		List<String> final_token_list = new ArrayList<String>();
		for(Tuple2<Tuple2, Double> t2:rstRDD.collect())
		{
			Tuple2 t = t2._1;
			double v = t2._2;
			String k = (String)t._1; // result token list
			if(!k.contains(" ")) break;
			for(String token:k.split(" "))
			{
				if(token.startsWith("*"))
				{
					// Optimized to skip
					final_token_list = new ArrayList<String>();
					break;
				}
				else
				{
					final_token_list.add(String.format("-{}".format(token)));
				}
			}
		}
		
		return final_token_list;
	}
	
	public static String List2Str(List<String> alist)
	{
		StringBuffer strBuf = new StringBuffer("[");
		if(alist.size()>0)
		{
			strBuf.append(String.format("\"%s\"", alist.get(0)));
			for(int i=1; i<alist.size(); i++) strBuf.append(String.format(", \"%s\"", alist.get(i)));
		}
		strBuf.append("]");
		return strBuf.toString().trim();
	}
	
	public static String Sug2Str(Map m)
	{
		StringBuffer strBuf = new StringBuffer("{");
		strBuf.append(String.format("\"Position\": %d", m.get("Position")));
		strBuf.append(String.format(", \"Notes\": \"%s\"", m.get("Notes")));
		strBuf.append(String.format(", \"ErrorType\": \"%s\"", m.get("ErrorType")));
		List<String> alist = new ArrayList<String>();
		alist.addAll((Set<String>)m.get("Suggestion"));
		strBuf.append(String.format(", \"Suggestion\": %s", List2Str(alist)));
		strBuf.append("}");
		//System.out.printf("\tSuggestion: %s\n", strBuf.toString());
		return strBuf.toString();
	}
	
	public static String Sug2Json(List<Map> proc_json)
	{
		if(proc_json.size()==0) return "[]";
		
		StringBuffer strBuf = new StringBuffer("[");
		
		//[{"Position": 4, "Notes": "", "ErrorType": "Spell", "Suggestion": ["\u80a1"]}]
		strBuf.append(Sug2Str(proc_json.get(0)));
		for(int i=1; i<proc_json.size(); i++)
		{
			Map mo = proc_json.get(i);
			strBuf.append(String.format(" ,%s", Sug2Str(mo)));
		}
		
		strBuf.append("]");
		return strBuf.toString().trim();
	}
	
	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		long bt = System.currentTimeMillis();  
        String target_sent = args[0];  
        String inputFile = args[1];
        String task = args[2]; 			// Task ID
        int topN = 1;  					// Show at most top 10 candidates 
        List<Map> proc_json = new ArrayList<Map>();
          
        // Create a Java Spark Context
        SparkConf sparkConf = new SparkConf().setAppName(String.format("tokenizer-%s", task));
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        //JavaSparkContext sc = new JavaSparkContext(master,   
        //                                           String.format("tokenizer-%s", task),   
        //                                           System.getenv("SPARK_HOME"),   
        //                                           System.getenv("JARS"));  
                
       
        // Load our input data.  
        testRDD = sc.textFile(inputFile);
        
//        wordRDD = testRDD.flatMap(new FlatMapFunction<String,String>(){
//        	/**
//			 * 
//			 */
//			private static final long serialVersionUID = 1L;
//
//			public Iterator<String> call(String x){  
//                return Arrays.asList(x.split(" ")).iterator();  
//            }
//        }).distinct().persist(StorageLevel.MEMORY_AND_DISK());
        
        
        String ctoken = null;
        String rtoken = null;
        JavaPairRDD<Tuple2,Double> trtRDD = testRDD.mapToPair(new TokenCheck(target_sent)).reduceByKey(new ReduceNSumFunc()).filter(t->{return t._2>0.5;});
        List<Candi> coll_cache_dict = new ArrayList<Candi>();
        System.out.printf(String.format("Candidate number=%,d...(%,d sec)\n", trtRDD.count(), (System.currentTimeMillis()-bt)/1000));
        for(Tuple2 rt:trtRDD.collect())
        {
        	double score = (Double)rt._2;
        	Tuple2 t = (Tuple2)rt._1;
        	String k = (String)t._1;  // Candidate sentence
        	List<String> final_token_list = new ArrayList<String>();        	
        	//Map<String,String> suggest_token_list = new HashMap<String,String>();
        	String ptoken = null;
        	boolean skip = false;
        	for(String token:k.split(" "))
        	{
        		//System.out.printf("\tCheck %s...\n", token);
        		if(token.length()>5 && !token.contains(":"))
        		{
        			skip=true;
        			break;
        		}
        		
        		if(token.startsWith("*"))
        		{
        			ctoken = token.substring(1);
        			if(ctoken.contains(":"))
        			{
        				// We have RP here
        				String rp_items[] = ctoken.split(":");
        				ctoken = rp_items[0];
        				rtoken = rp_items[1];
        			}
        			else 
        			{
        				rtoken = null;
        			}
        			
        			if(ctoken.length()<5 && isTokenSeenLocal(ctoken))
        			{
        				//score += 0.1;
        				final_token_list.add(String.format("~%s", ctoken));
        				ptoken = token;
        				
        			}
        			else
        			{
        				if(ctoken.length()>=2)
        				{
        					List<String> sub_token_list = tokenize_local(ctoken, ctoken.length()/2);
        					if(sub_token_list.size()>0)
        					{
        						score += sub_token_list.size() * 0.5;
        						final_token_list.addAll(sub_token_list);
        						ptoken = token;
        						continue;
        					}
        				}
        				else
        				{
        					score -= 0.3 * ctoken.length();
        				}
        				final_token_list.add(token);
        			}
        		}
        		else
        		{        			
        			final_token_list.add(token);
        			ptoken = token;
        			//System.out.printf("\tptoken=%s\n", ptoken);
        		}
        		
        	} // for(String token:k.split(" "))
        	if(skip) continue;
        	
        	//System.out.printf("%s with score=%.02f...\n", final_token_list, score);
        	if(score>0) coll_cache_dict.add(new Candi(final_token_list, (String)t._2, score));        	
        } // for(Tuple2 rt:trtRDD.collect())
        
        coll_cache_dict.sort(new CandiCmp());
        
        for(Candi c:coll_cache_dict)
        {
        	List<String> tokenized_rst = c.final_token_list;
        	String source_sent = c.source_sent;
        	Map<String,Object> rst = new HashMap<String,Object>();
        	rst.put("tokenized", tokenized_rst);
        	rst.put("score", c.score);
        	rst.put("source_sent", source_sent);
        	rst.put("collsug", new ArrayList<String>());
        	
        	//System.out.printf("Candidate Tokenized Result='%s' (score=%.02f by '%s')\n", List2Str(tokenized_rst), c.score, source_sent);
        	System.out.printf("Candidate Tokenized Result (score=%.02f by '%s'):\n", c.score, source_sent);
        	for(String tk:tokenized_rst)
        	{
        		System.out.printf("\t%s\n", tk);
        	}
        	System.out.println();
        	
        	String ptoken = null;
        	int si = 0;
        	int ti = 0;
        	int tlen = tokenized_rst.size();
        	System.out.printf("Look for collocation correction...(%,d)\n", tokenized_rst.size());
        	for(String token:tokenized_rst)
        	{               		
        		ctoken = token.substring(1);
        		if(IsNumeric(ctoken))
        		{
        			// Skip number
        		}
        		else if(token.startsWith("*") || token.startsWith("~"))
        		{        	
        			//System.out.printf("Look for %s %s...\n", ptoken, token);
        			boolean isSkip = false;
        			if(token.startsWith("~"))
        			{
        				if(ti+1<tlen)
        				{
        					String ntoken = tokenized_rst.get(ti + 1);
            				if(ntoken.startsWith("+"))
            				{
            					isSkip = isNexTokenSeen(ctoken, ntoken.substring(1));
            					//if(isSkip) System.out.printf("\tHit %s->%s!\n", ctoken, ntoken);
            					//else System.out.printf("\tMiss %s->%s\n", ctoken, ntoken);
            				}
        				}
        				else if(ptoken.startsWith("+"))
        				{
        					isSkip = isPrvTokenSeen(ptoken.substring(1), ctoken);
        					//if(isSkip) System.out.printf("\tHit %s->%s!\n", ptoken, ctoken);
        					//else System.out.printf("\tMiss %s->%s\n", ptoken, ctoken);
        				}
        			}
        			
        			if(!isSkip)
        			{
        				//System.out.printf("Look for collocation for token=%s...(ptoken=%s; ctoken=%s)\n", token, ptoken, ctoken);
            			//JavaPairRDD<String,Integer> collRDD = look4Coll(ptoken, ctoken);
            			FakeCollRDD collRDD = null;
            			if(ptoken==null && ti+1 < tokenized_rst.size())
            			{
            				String nntoken = tokenized_rst.get(ti+1);
            				if(!nntoken.startsWith("*"))
            				{        					
            					nntoken = nntoken.substring(1);
            					collRDD = look2CollLocalF(ctoken, nntoken);
            				}
            				else
            				{        					
            					collRDD = look2CollLocal(ptoken, ctoken, null);
            				}
            			}
            			else
            			{     
            				if(ti+1<tokenized_rst.size())
            				{
            					String nntoken = tokenized_rst.get(ti+1);
            					if(!nntoken.startsWith("*"))
                				{        					
                					nntoken = nntoken.substring(1);
                					collRDD = look2CollLocal(ptoken, ctoken, nntoken);
                				}
                				else
                				{        					
                					collRDD = look2CollLocal(ptoken, ctoken, null);
                				}
            				}
            				else
            				{
            					collRDD = look2CollLocal(ptoken, ctoken, null);
            				}
            			}
            			
            			if(collRDD!=null)
            			{        				
            				Map<String,Object> csl = new HashMap<String,Object>();
            				csl.put("sp", si);
            				csl.put("ep", si+ctoken.length());
            				csl.put("org", ctoken);
            				csl.put("sug", new ArrayList<String>());        				
            				List<Colli> colliList = new ArrayList<Colli>();
            				for(Tuple2 t:collRDD.collect())
            				{        					
            					Tuple2 st = (Tuple2)t._1;
            					int v = (Integer)t._2;
            					//System.out.printf("\tGot feedback...%s\n", t);
            					colliList.add(new Colli((String)st._1, (String)st._2, v));
            				}
            				colliList.sort(new ColliCmp());
            				for(Colli ci:colliList)
            				{
            					List<String> sugList = (List<String>)csl.get("sug");
            					System.out.printf("\tAdd suggestion token=%s for %s\n", ci.ntoken, ctoken);
            					sugList.add(ci.ntoken);
            					for(int i=0; i<ctoken.length(); i++)
            					{
            						String nc = ci.ntoken.substring(i, i+1);
            						String cc = ctoken.substring(i, i+1);
            						//System.out.printf("\t\tCheck %s with %s\n", nc, cc);
            						if(!nc.equals(cc))
            						{
            							Map<String,Object> sugMap = new HashMap<String,Object>();
            							sugMap.put("ErrorType", "Spell");
            							sugMap.put("Notes", "");
            							sugMap.put("Position", si + i + 1);
            							Set<String> cSet = new TreeSet<String>();
            							cSet.add(nc);
            							sugMap.put("Suggestion", cSet);
            							proc_json.add(sugMap);
            						}
            					}
            				}
            			}
        			}
        		}
        		
        		ptoken = token;
        		si += ctoken.length();
        		ti++;
        	} // for(String token:tokenized_rst)
        	
        	// Only handle the candidate with highest score
        	break;
        }
        
        // Merge suggestion with same position
        Map<Integer,Map> pos_dict = new HashMap<Integer, Map>();
        for(Map ed:proc_json)
        {        	
        	Map ped = pos_dict.getOrDefault(ed.getOrDefault("Position", Integer.valueOf(-1)), null);
        	if(ped!=null)
        	{
        		Set<String> sugSet = (Set<String>)ped.get("Suggestion");
        		sugSet.addAll((Set<String>)ed.get("Suggestion"));
        	}
        	else
        	{
        		pos_dict.put((Integer)ed.get("Position"), ed);
        	}
        }

        proc_json.clear();
        for(Map m:pos_dict.values()) proc_json.add(m);
        
        try 
        {
        	BufferedWriter bw = new BufferedWriter(new FileWriter(new File(String.format("/tmp/%s.json", task))));
        	bw.write(Sug2Json(proc_json));
        	bw.close();
        }
        catch(Exception e) 
        {
        	System.err.printf("Something wrong:\n%s\n", e);
        	e.printStackTrace();
        }
        
        long diff = System.currentTimeMillis() - bt;
        System.out.printf("\t[Info] Done! (%,d sec)\n\n", diff/1000);
        
        
        sc.close();
	}  // Main
	
}
