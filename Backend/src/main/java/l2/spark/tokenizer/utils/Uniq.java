package l2.spark.tokenizer.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Set;
import java.util.TreeSet;

public class Uniq {

	public static void main(String[] args) throws Exception{
		if(args.length<2)
		{
			System.out.printf("\t[Info] Give arg1 as input file; arg2 as output file!\n\n");
			return;
		}

		Set<String> lineSet = new TreeSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(new File(args[0])));
		String line = null;
		int lc=0;
		while((line=br.readLine())!=null)
		{
			line = line.trim();
			line = line.replaceAll("[「」]", "");
			line = line.replaceAll("\\s{2,}", " ");
			line = line.replaceAll("\\d+萬", "r:\\\\d+萬");
			line = line.replaceAll("\\d+月", "r:\\\\d+月");
			line = line.replaceAll("\\d+日", "r:\\\\d+日");
			line = line.replaceAll("\\d+年", "r:\\\\d+年");
			if(line.length()>50) continue;
			String lines[] = line.split("[、;]");
			for(String sline:lines)
			{
				sline = sline.trim();
				lineSet.add(sline);
				lc++;
			}			
		}
		System.out.printf("\t[Info] Unique process done! (%d/%d)!\n", lineSet.size(), lc);
		br.close();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(args[1])));
		
		System.out.printf("\t[Info] Output line set into %s...\n", args[1]);
		for(String l:lineSet)
		{
			bw.write(String.format("%s\n", l));
		}
		bw.close();
	}
}
