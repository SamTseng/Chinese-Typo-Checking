package l2.spark.tokenizer;

import java.io.File;
import java.util.Map;

import l2.spark.tokenizer.utils.HPath;
import l2.spark.tokenizer.utils.T2;

public class DelToken{
	public static File TokenRootPath = new File("/home/datas/tokens/");

	public static void main(String[] args) throws Exception{
		String token = args[0];
		String stoken = args[1];
		
		HPath hp = new HPath(TokenRootPath, 2);	
		
		Map<String,T2> tkMap = hp.readToken(token);
		T2 t2 = tkMap.get(token);
		
		if(stoken.equals("-"))
		{			
			hp.delToken(token, false);
			hp.delToken(token, true);
			if(!hp.getFilePath(token, true).exists() && !hp.getFilePath(token, true).exists())
			{
				System.out.printf("Remove token=%s...Done!\n", token);
			}
			else
			{
				System.out.printf("Remove token=%s...Fail!\n%s\n%s\n", token, hp.isHTokenExist(token), 
						hp.getFilePath(token, true).getAbsolutePath(),
						hp.getFilePath(token, false).getAbsolutePath());
			}
			
			//hp.delToken(token, true);
			
		}
		else
		{
			t2.bSet.remove(stoken);													
			hp.writeToken(token, tkMap);
			System.out.printf("Remove %s>%s...Done!\n", token, stoken);
			
			tkMap = hp.readToken(token);
			t2 = tkMap.get(token);
			for(String st:t2.bSet)
			{
				System.out.printf("\t%s\n", st);
			}
			System.out.println();
		}
	}
}
