package l2.spark.tokenizer.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class HPath {
	int	depth = 2;
	MessageDigest digest = null;
	File rootPath = null;
	
	public HPath(File root, int depth)
	{
		try 
		{
			digest = MessageDigest.getInstance("SHA-256");
		} catch(Exception e)
		{
			e.printStackTrace();
		}
		this.rootPath = root;
		this.depth = depth;
	}
	
	private static String BytesToHex(byte[] hash) {
	    StringBuffer hexString = new StringBuffer();
	    for (int i = 0; i < hash.length; i++) {
	    String hex = Integer.toHexString(0xff & hash[i]);
	    if(hex.length() == 1) hexString.append('0');
	        hexString.append(hex);
	    }
	    return hexString.toString();
	}
	
	public File getFilePath(String token, boolean isHead)
	{		
		String key = token;
		if(isHead) key = token.substring(0, 1);
		
		String fn =  BytesToHex(digest.digest(key.getBytes(StandardCharsets.UTF_8)));
		File rp = rootPath;
		if(isHead)
		{
			rp = new File(rp, "H");
			if(!rp.isDirectory()) rp.mkdirs();
		}
		
		int slen = Math.min(fn.length()/this.depth, 4);
		//List<String> segList = new ArrayList<String>();
		for(int i=0; i<this.depth; i++)
		{
			rp = new File(rp, fn.substring(i*slen, (i+1)*slen));
			if(!rp.isDirectory()) rp.mkdirs();
		}		
		
		return  new File(rp, fn);
	}
	
	@SuppressWarnings("unchecked")
	public Set<String> readHToken(String token) throws Exception
	{
		File f = this.getFilePath(token, true);
		if(f.exists())
		{
			FileInputStream fileIn = null;
			try
			{
				fileIn = new FileInputStream(f);
				ObjectInputStream in = new ObjectInputStream(fileIn);
		        return (TreeSet<String>) in.readObject();
			}
			catch(Exception e)
			{
				System.err.printf("\t[Error] Fail to read %s (%s)\n", f.getAbsolutePath(), token);
				f.delete();
				e.printStackTrace();
			}
			finally
			{
				if(fileIn!=null) fileIn.close();
			}
		}
		else
		{
			//System.out.printf("HToken=%s doesn't exist in token pool...(%s)\n", token, f.getAbsolutePath());
		}
		return new TreeSet<String>();
	}
	
	public boolean delToken(String token, boolean isHead) throws Exception
	{
		File f = this.getFilePath(token, isHead);
		if(f.exists())
		{
			try
			{
				f.deleteOnExit();
				return true;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String,T2> readToken(String token) throws IOException
	{
		File f = this.getFilePath(token, false);
		if(f.exists())
		{
			FileInputStream fileIn = null;
			try
			{
				fileIn = new FileInputStream(f);
				ObjectInputStream in = new ObjectInputStream(fileIn);
		        return (Map<String,T2>) in.readObject();
			}
			catch(Exception e)
			{
				System.err.printf("\t[Error] Fail to read %s (%s)\n", f.getAbsolutePath(), token);
				f.delete();
				e.printStackTrace();				
			}
			finally
			{
				if(fileIn!=null) fileIn.close();
			}
		}
		else
		{
			//System.out.printf("Token=%s doesn't exist in token pool...(%s)\n", token, f.getAbsolutePath());
		}
		return new HashMap<String,T2>();
	}
	
	public void writeHToken(String token, Set<String> obj)
	{
		File f = this.getFilePath(token, true);
		try
		{
			 FileOutputStream fileOut = new FileOutputStream(f);
			 ObjectOutputStream out = new ObjectOutputStream(fileOut);
			 out.writeObject(obj);
			 out.close();
			 fileOut.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void writeToken(String token, Map<String,T2> obj)
	{
		File f = this.getFilePath(token, false);
		try
		{
			 FileOutputStream fileOut = new FileOutputStream(f);
			 ObjectOutputStream out = new ObjectOutputStream(fileOut);
			 out.writeObject(obj);
			 out.close();
			 fileOut.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isHTokenExist(String token)
	{
		File fp = this.getFilePath(token, true);				
		return fp.exists();
	}
	
	public boolean isTokenExist(String token)
	{
		File fp = this.getFilePath(token, false);				
		return fp.exists();
	}
	
	public static void main(String args[])
	{
		HPath hp = new HPath(new File("C:\\tmp"), 2);
		File f = hp.getFilePath("中文", false);
		System.out.printf("%s\n", f.getAbsolutePath());
		System.out.printf("%s\n", hp.getFilePath("中文", true).getAbsolutePath());
	}
}
