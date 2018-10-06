package l2.spark.tokenizer;

import java.io.File;
import java.util.Map;

import l2.spark.tokenizer.utils.HPath;
import l2.spark.tokenizer.utils.T2;

public class ShwToken {
	public static File TokenRootPath = new File("/home/datas/tokens/");

	public static void main(String[] args) throws Exception{
		String token = args[0];
		
		HPath hp = new HPath(TokenRootPath, 2);	
		
		Map<String,T2> tkMap = hp.readToken(token);
		T2 t2 = tkMap.get(token);
	
		if(args.length==1)
		{
			tkMap = hp.readToken(token);
			t2 = tkMap.get(token);
			System.out.printf("\t[Info] Explore token=%s: (%,d)\n", token, t2.bSet.size());
			for(String st:t2.bSet)
			{
				System.out.printf("\t%s\n", st);
			}
			System.out.println();
		}
		else if(args.length>1)
		{
			System.out.printf("\t[Info] Explore token=%s > %s: %s\n", token, args[1], t2.bSet.contains(args[1]));
		}
			
	}
}
