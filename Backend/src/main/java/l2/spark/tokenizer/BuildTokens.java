package l2.spark.tokenizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import l2.spark.tokenizer.utils.HPath;
import l2.spark.tokenizer.utils.T2;

public class BuildTokens {
	public static String NumToken = "__NUM__";
	public static String CNumToken = "__CNUM__";
	public static String SNumToken = "__SNUM__";
	public static String PerToken = "__PER__";
	public static String MonToken = "__MONTH__";
	public static String DayToken = "__MDAY__";
	public static String HorToken = "__HOUR__";
	public static String YerToken = "__YEAR__";
	public static String NameToken = "__NAME__";
	public static String FwToken = "__FW__";
	public static Pattern NumPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九十.0-9]+)$");
	public static Pattern SCNumPtn = Pattern.compile("^([一二三四五六七八九十百千萬億兆]+)$");
	public static Pattern CNumPtn = Pattern.compile("^([.１２３４５６７８９０0-9一二三四五六七八九十百千萬億兆]+[多]?)$");
	public static Pattern PertPtn = Pattern.compile("^([0-9]+)(%|％)$");
	public static Pattern YerPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九0-9]{2,4})年$");
	public static Pattern MonPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九0-9]{1,2})月$");
	public static Pattern DayPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九0-9]{1,2})日$");
	public static Pattern HorPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九0-9]{1,2})(點|時|點鐘)$");
	public static Pattern FwPtn = Pattern.compile("^([-_A-Za-z][-_A-Za-z0-9]+)$");
	
	public static Map<String,Pattern> TokenMap = new HashMap<String,Pattern>();
	
	static {
		TokenMap.put(FwToken, FwPtn);
		TokenMap.put(NumToken, NumPtn);
		TokenMap.put(CNumToken, CNumPtn);
		//TokenMap.put(SNumToken, SCNumPtn);
		TokenMap.put(PerToken, PertPtn);
		TokenMap.put(MonToken, MonPtn);
		TokenMap.put(DayToken, DayPtn);
		TokenMap.put(HorToken, HorPtn);
		TokenMap.put(YerToken, YerPtn);
	}
	
	public static boolean IsNumeric(String str) {
		if(str.equals(NumToken)) return true;
		
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	public static void AddLine(String line, HPath hp) throws Exception
	{
		line = line.trim();
		if(line.isEmpty()) return;
		List<String> token_list = Arrays.asList(line.trim().split(" "));
		String ptoken = token_list.get(0);
		Set<String> hset = hp.readHToken(ptoken);
		hset.add(ptoken);
		hp.writeHToken(ptoken, hset);
		
		for(int i=1; i<token_list.size(); i++)
		{
			String token = token_list.get(i);
			
			// Back
			Map<String,T2> tkMap = hp.readToken(ptoken);
			T2 t2 = tkMap.get(ptoken);
			if(t2==null)
			{
				t2 = new T2(ptoken);					
				tkMap.put(ptoken, t2);
			}
			if(t2.token==null) t2.token = ptoken;
			t2.bSet.add(token);															
			hp.writeToken(ptoken, tkMap);
			//System.out.printf("\t\t%s->%s\n", ptoken, token);
			
			// Front
			tkMap = hp.readToken(token);
			t2 = tkMap.get(token);
			if(t2==null)
			{
				t2 = new T2(token);					
				tkMap.put(token, t2);
			}
			if(t2.token==null) t2.token = token;
			t2.fSet.add(ptoken);
			hp.writeToken(token, tkMap);
			//System.out.printf("\t\t%s<-%s\n", ptoken, token);
			
			ptoken = token;
		}
	}
	
	public static String TokenMapping(String token)
	{
		if(token.contains("/"))
		{
			String token_items[] = token.split("/");
			if(token_items.length==2)
			{
				System.out.printf("Receive Special token: %s\t%s...\n", token_items[0], token_items[1]);
				return String.format("__%s__", token_items[1]);
			}
		}
		
		Iterator<Entry<String,Pattern>> itr = TokenMap.entrySet().iterator();
		
		while(itr.hasNext())
		{
			Entry<String,Pattern> ety = itr.next();
			if(ety.getValue().matcher(token).find()) 
			{
				System.out.printf("\t[Test] %s Hit %s!\n", token, ety.getKey());
				return ety.getKey();
			}
		}
		
		return token;
	}
	
	public static void main(String[] args) throws Exception{
		if(args.length==0)
		{
			System.out.printf("\t[Info] Please give arg1=File to process; arg2=Path to build token pool!\n\n");
			return;
		}
		else if(args.length==3)
		{
			HPath hp = new HPath(new File(args[0]), 2);
			if(args[2].equals("H"))
			{
				Set<String> hset = hp.readHToken(TokenMapping(args[1]));
				for(String t:hset) System.out.printf("%s\n", t);
				System.out.printf("\t[Info] Done! (%,d)!\n\n", hset.size());
			}
			else if(args[2].toLowerCase().equals("a"))
			{
				AddLine(args[1], hp);
			}
			else
			{
				Map<String,T2> rstMap = hp.readToken(TokenMapping(args[1]));
				T2 t2= rstMap.get(TokenMapping(args[1]));
				if(t2!=null)
				{
					for(String t:t2.fSet) System.out.printf("F:%s\n", t);
					for(String t:t2.bSet) System.out.printf("B:%s\n", t);
					System.out.printf("\t[Info] Done! (%,d)!\n\n", t2.fSet.size()+t2.bSet.size());
				}
				else
				{
					System.out.printf("\t[Info] Miss!\n\n");
				}
			}
			return;
		}

		File inFile = new File(args[0]);
		File outPath = new File(args[1]);
		
		if(!inFile.exists())
		{
			System.err.printf("\t[Warn] inFile=%s doesn't exist!\n\n", inFile.getAbsolutePath());			
			return;
		}
		
		if(!outPath.isDirectory()) outPath.mkdirs();
		
		
		System.out.printf("\t[Info] Start building token pool...\n");
		HPath hp = new HPath(outPath, 2);		
		BufferedReader br = new BufferedReader(new FileReader(inFile));
		String line = null;
		int lc = 0;
		try
		{
			while((line=br.readLine())!=null)
			{			
				line = line.trim();
				if(line.isEmpty()) continue;
				lc++;
				List<String> token_list = Arrays.asList(line.trim().split(" "));
				String ptoken = TokenMapping(token_list.get(0));
				Set<String> hset = hp.readHToken(ptoken);
				hset.add(ptoken);
				hp.writeHToken(ptoken, hset);
				
				for(int i=1; i<token_list.size(); i++)
				{
					String token = TokenMapping(token_list.get(i));
					if(ptoken.startsWith("r:") || ptoken.equals("%"))
					{
						ptoken = token; continue;
					}
					
					else if(token.startsWith("r:") || token.equals("%"))
					{
						ptoken = token; continue;
					}
					
					if(ptoken.equals(NumToken) || ptoken.equals(CNumToken))
					{
						token = String.format("__UNIT_%s__", token);
						System.out.printf("Got unit: %s\t%s\n", ptoken, token);
					}
					
					// Back
					Map<String,T2> tkMap = hp.readToken(ptoken);
					T2 t2 = tkMap.get(ptoken);
					if(t2==null)
					{
						t2 = new T2(ptoken);				
						tkMap.put(ptoken, t2);
					}
					if(t2.token==null) t2.token = ptoken;
					t2.bSet.add(token);															
					hp.writeToken(ptoken, tkMap);
					//System.out.printf("\t\t%s->%s\n", ptoken, token);
					
					// Front
					tkMap = hp.readToken(token);
					t2 = tkMap.get(token);
					if(t2==null)
					{
						t2 = new T2(token);					
						tkMap.put(token, t2);
					}
					if(t2.token==null) t2.token = token;
					t2.fSet.add(ptoken);
					hp.writeToken(token, tkMap);
					//System.out.printf("\t\t%s<-%s\n", ptoken, token);
					
					ptoken = token;
				}
				
				if(lc%100==0) System.out.printf("\t[Info] %,d lines done...\n", lc);
			}
		}
		catch(Exception e)
		{
			System.err.printf("Fail to handle line=%s!\n\n", line);
			e.printStackTrace();
		}
		
		System.out.printf("\t[Info] Done (%,d)!\n\n", lc);
		br.close();
	}
}
