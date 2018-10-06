package l2.spark.tokenizer;

import static l2.spark.tokenizer.BuildTokens.CNumToken;
import static l2.spark.tokenizer.BuildTokens.DayToken;
import static l2.spark.tokenizer.BuildTokens.FwToken;
import static l2.spark.tokenizer.BuildTokens.HorToken;
import static l2.spark.tokenizer.BuildTokens.MonToken;
import static l2.spark.tokenizer.BuildTokens.NumToken;
import static l2.spark.tokenizer.BuildTokens.PerToken;
import static l2.spark.tokenizer.BuildTokens.YerToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import l2.spark.tokenizer.utils.HPath;
import l2.spark.tokenizer.utils.T2;

public class TPSearch {
	public static HPath hp = new HPath(new File("/home/datas/tokens"), 2);
	public static List<List<String>> Solutions = new ArrayList<List<String>>();	
	public static Pattern NumPtn = Pattern.compile("^([１２３４５６７８９０一二三四五六七八九.0-9]+)");
	public static Pattern CNumPtn = Pattern.compile("^([１２３４５６７８９０.0-9一二三四五六七八九十百千萬億兆]+[多]?)");
	public static Pattern SNumPtn = Pattern.compile("^([一二三四五六七八九十百千萬億兆]+)");
	public static Pattern FwPtn = Pattern.compile("^([-_A-Za-z][-_A-Za-z0-9]+)");
	public static Map<String,Set<String>> MCMap = new TreeMap<String,Set<String>>();

	static {
		String mcList[][] = {
								{"根","跟"}, 
								{"吧", "把"},
								{"很", "狠", "恨"},
								{"髒", "葬"},
								{"從", "重"},
								{"玩", "完"}
							};
		for(String[] mc:mcList)
		{
			Set<String> mcSet = new TreeSet<String>();
			mcSet.addAll(Arrays.asList(mc));
			for(String mcc:mcSet) MCMap.put(mcc, mcSet);
		}
		
		/*File cfsFile = new File("ConfusionSet.txt");
		if(cfsFile.exists())
		{
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(cfsFile));
				String line;
				while((line=br.readLine())!=null)
				{
					Set<String> mcSet = new TreeSet<String>();
					String items[] = line.split(",");
					for(Character c:items[1].toCharArray()) mcSet.add(String.valueOf(c));
					MCMap.put(items[0], mcSet);
				}					
				br.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}*/
	}
	
	public static boolean NumCheck(String target_sent, List<String> tokens)
	{
		Matcher mth = NumPtn.matcher(target_sent);
		if (mth.find()) {
			String num = mth.group(1);
			String token = NumToken;
			String rsent = target_sent.substring(num.length());
			if(tokens==null) tokens = new ArrayList<String>();

			if (rsent.startsWith("%") || rsent.startsWith("％")) {
				num = num + "%";
				token = PerToken;
				rsent = rsent.substring(1);
				//System.out.printf("Got pert=%s\t%s\n", num, rsent);
			} else if (rsent.startsWith("月") && num.length() <= 2) {
				num = num + "月";
				token = MonToken;
				rsent = rsent.substring(1);
				//System.out.printf("Got month=%s\t%s\n", num, rsent);
			} else if (rsent.startsWith("日") && num.length() <= 2) {
				num = num + "日";
				token = DayToken;
				rsent = rsent.substring(1);
				//System.out.printf("Got month day=%s\t%s\n", num, rsent);
			} 
			else if(rsent.startsWith("點鐘"))
			{
				num = num + rsent.substring(0, 2);
				token = HorToken;
				rsent = rsent.substring(2);
				//System.out.printf("Got hour=%s\t%s\n", num, rsent);
			}
			else if(rsent.startsWith("時") || rsent.startsWith("點"))
			{
				num = num + rsent.substring(0, 1);
				token = HorToken;
				rsent = rsent.substring(1);
				//System.out.printf("Got hour=%s\t%s\n", num, rsent);
			}
			else if(rsent.startsWith("年") && num.length()>=2 && num.length()<=4)
			{
				num = num + rsent.substring(0, 1);
				token = YerToken;
				rsent = rsent.substring(1);
				//System.out.printf("Got year=%s\t%s\n", num, rsent);
			}
			else {
				token = NumToken;
				//System.out.printf("Got number=%s\t%s\n", num, rsent);
			}

			try {
				Map<String, T2> tkMap = hp.readToken(token);
				T2 t2 = tkMap.get(token);
				// System.out.printf("\tT:%s->%s (%s)\n", tokens.get(tokens.size()-1), token,
				// rsent);
				if (t2 != null) {
					List<String> new_token_list = new ArrayList<String>();
					new_token_list.addAll(tokens);
					new_token_list.add(num);
					SearchTP(new_token_list, t2, rsent, 2);
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}
		return false;
	}
	
	public static boolean CNumCheck(String target_sent, List<String> tokens)
	{
		// Handle Chinese Number
		Matcher mth = CNumPtn.matcher(target_sent);
		if (mth.find()) {
			String cnum = mth.group(1);
			String token = CNumToken;
			String rsent = target_sent.substring(cnum.length());
			if(tokens == null) tokens = new ArrayList<String>();
			//tokens.add(token);
			//System.out.printf("Got cnum=%s\t%s\n", cnum, rsent);
			try {
				Map<String, T2> tkMap = hp.readToken(token);
				T2 t2 = tkMap.get(token);
				// System.out.printf("\tT:%s->%s (%s)\n", tokens.get(tokens.size()-1), token,
				// rsent);
				if (t2 != null) {
					List<String> new_token_list = new ArrayList<String>();
					new_token_list.addAll(tokens);
					new_token_list.add(cnum);
					SearchTP(new_token_list, t2, rsent, 2);
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	public static boolean FwCheck(String target_sent, List<String> tokens)
	{
		// Handle Foreign word
		Matcher mth = FwPtn.matcher(target_sent);
		if (mth.find()) {
			String cnum = mth.group(1);
			String token = FwToken;
			String rsent = target_sent.substring(cnum.length());
			if(tokens == null) tokens = new ArrayList<String>();
			//tokens.add(token);
			System.out.printf("Got fw=%s\t%s\n", cnum, rsent);
			try {
				Map<String, T2> tkMap = hp.readToken(token);
				T2 t2 = tkMap.get(token);				
				if (t2 != null) {
					List<String> new_token_list = new ArrayList<String>();
					new_token_list.addAll(tokens);
					new_token_list.add(cnum);
					SearchTP(new_token_list, t2, rsent, 2);
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}
	
	public static void main(String[] args) {
		String target_sent = args[0];
		String task = args[1].trim(); 			// Task ID

		boolean isHit = false; 
		if(CNumCheck(target_sent, null))			
		{
			//isHit = true;
		}
		
		if(NumCheck(target_sent, null))
		{
			//isHit = true;
		}
		
		if(FwCheck(target_sent, null))
		{
			
		}
		
		if(!isHit)
		{
			// Normal
			
			for(int i=1; i<=Math.min(5, target_sent.length()); i++)
			{
				String token = target_sent.substring(0, i);
				if(token.trim().isEmpty()) continue;
				String rsent = target_sent.substring(i);
				try
				{
					Map<String,T2> tkMap = hp.readToken(token.trim());
					T2 t2 = tkMap.get(token.trim());
					if(t2!=null) 
					{
						List<String> tokens = new ArrayList<String>();
						tokens.add(token);
						//System.out.printf("\tH:%s\t%s\n", token, rsent);
						SearchTP(tokens, t2, rsent, 2);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		List<Map> proc_json = new ArrayList<Map>();
		
		List<List<String>> optSols = new ArrayList<List<String>>();
		
		if(Solutions.size()>0)
		{
			System.out.printf("\t[Info] %,d solution(s) found:\n", Solutions.size());
			
			List<String> op_solution = null;
			int err_cnt = Integer.MAX_VALUE;
			for(List<String> tokens:Solutions)
			{								
				//System.out.printf("\t%s\n", String.join(",", tokens));
				int cur_err_cnt = 0;
				for(String token:tokens)
				{
					if(token.contains(":"))
					{
						cur_err_cnt ++;
					}
				}
				
				if(cur_err_cnt < err_cnt)
				{
					op_solution = tokens;
					err_cnt = cur_err_cnt;
					optSols.clear();
					optSols.add(tokens);
				}
				else if(cur_err_cnt == err_cnt)
				{
					optSols.add(tokens);
				}
			}
			
			//int si=0;			
			for(List<String> tokens:optSols)
			{
				System.out.printf("\t%s\n", String.join("|", tokens));
				int si = 0;
				for(String token:tokens)
				{
					if(token.contains(":"))
					{
						String ot_with_sug[] = token.split(":");
						token = ot_with_sug[0];
						String suggt = ot_with_sug[1];
						Map<String,Object> sugMap = new HashMap<String,Object>();
						sugMap.put("ErrorType", "Spell");
						sugMap.put("Notes", "");
						for(int i=0; i<token.length(); i++)
						{
							String nc = token.substring(i, i+1);
							String cc = suggt.substring(i, i+1);
							if(token.charAt(i)!=suggt.charAt(i))
							{
								sugMap.put("Position", si + i + 1);
								Set<String> cSet = new TreeSet<String>();
								cSet.add(cc);
								sugMap.put("Suggestion", cSet);
								break;
							}
						}						
						proc_json.add(sugMap);
					}
					
					si+=token.length();
				}
			}			
		}
		else
		{
			System.out.printf("\t[Info] No TP found!\n\n");
		}
				
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
	}
	
	public static void SearchTP(List<String> tokens, T2 pt, String sent, int ec)
	{
		if(sent.trim().length()==0) {
			Solutions.add(tokens);
			return;
		} else if(ec<0) return;
		
		// Handle Chinese Number
		boolean isHit = false;
		if(CNumCheck(sent, tokens))
		{
			//isHit = false;
		}
		
		if(NumCheck(sent, tokens))
		{
			//isHit = true;
		}
		
		if(FwCheck(sent, tokens))
		{
			isHit = true;
		}
		
		if(!isHit)
		{
			// Normal process
			for(int i=1; i<=Math.min(5, sent.length()); i++)
			{
				String token = sent.substring(0, i);
				
				if(pt!=null && pt.token!=null && (pt.token.equals(CNumToken) || pt.token.equals(NumToken)))
				{					
					token = String.format("__UNIT_%s__", token);
				}							
				
				String rsent = sent.substring(i);
				//System.out.printf("Normal> %s:%s\n", token, rsent);
				if(pt.bSet.contains(token))
				{				
					try
					{					
						Map<String,T2> tkMap = hp.readToken(token);
						T2 t2 = tkMap.get(token);
						//System.out.printf("\tT:%s->%s (%s)\n", tokens.get(tokens.size()-1), token, rsent);
						if(t2!=null)
						{
							List<String> new_token_list = new ArrayList<String>();
							new_token_list.addAll(tokens);
							new_token_list.add(token);
							SearchTP(new_token_list, t2, rsent ,ec);
						}
					}
					catch(Exception e) {e.printStackTrace();}
				}
				else if(i>1)
				{
					for(String candi_token:pt.bSet)
					{
						double sim_score = _sim_of_token(token, candi_token);					
						if(candi_token.length()==i && sim_score>=0.5)
						{
							//System.out.printf("\t\tC:%s:%s:%.02f\n", token, candi_token, sim_score);
							try
							{
								Map<String,T2> tkMap = hp.readToken(candi_token);
								T2 t2 = tkMap.get(candi_token);
								if(t2!=null)
								{
									List<String> new_token_list = new ArrayList<String>();
									new_token_list.addAll(tokens);
									new_token_list.add(String.format("%s:%s", token, candi_token));
									SearchTP(new_token_list, t2, rsent , ec - 1);
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					// Check again
					/*try 
					{
						Map<String,T2> tkMap = hp.readToken(token);
						T2 t2 = tkMap.get(token);
						System.out.printf("\tS:%s->%s (%s)\n", tokens.get(tokens.size()-1), token, rsent);
						if(t2!=null)
						{
							List<String> new_token_list = new ArrayList<String>();
							new_token_list.addAll(tokens);
							new_token_list.add(token);
							SearchTP(new_token_list, t2, rsent);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}*/
				}
				else if(i==1)
				{
					Set<String> mcSet = MCMap.get(token);
					if(mcSet!=null)
					{
						for(String mc:mcSet)
						{
							if(mc.equals(token)) continue;
							try
							{
								Map<String,T2> tkMap = hp.readToken(mc);
								T2 t2 = tkMap.get(mc);
								List<String> new_token_list = new ArrayList<String>();
								new_token_list.addAll(tokens);
								new_token_list.add(String.format("%s:%s", token, mc));
								//System.out.printf("\t[Test] Try from %s:%s\t%s\n", token, mc, rsent);
								SearchTP(new_token_list, t2, rsent , ec - 1);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}
					/*for(String candi_token:pt.bSet)
					{
						if(candi_token.length()==1 && rsent.length()>1)
						{
							try
							{
								Map<String,T2> tkMap = hp.readToken(candi_token);
								T2 t2 = tkMap.get(candi_token);
								for(String st:t2.bSet)
								{
									if(rsent.startsWith(st))
									{
										List<String> new_token_list = new ArrayList<String>();
										new_token_list.addAll(tokens);
										new_token_list.add(String.format("%s:%s", token, candi_token));
										System.out.printf("\t[Test] Miss from %s:%s\t%s\n", token, candi_token, rsent);
										SearchTP(new_token_list, t2, rsent , ec - 1);
									}
								}
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
					}*/
				}
			}			
		}		
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
		
		for(int i=0; i<otoken_c_list.size(); i++)
		{
			if(otoken_c_list.get(i).equals(etoken_c_list.get(i))) hc+=1;
		}
		
		try {
			double sim = hc / otoken_c_list.size();			
			return sim; 
		} catch(Exception e) {return 0.0;}
	}
}
