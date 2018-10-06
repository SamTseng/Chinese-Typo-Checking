package l2.spark.tokenizer.utils;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

public class T2  implements Serializable{
	private static final long serialVersionUID = 1L;
	public String token = null;
	public Set<String> fSet = new TreeSet<String>();
	public Set<String> bSet = new TreeSet<String>();
	
	public T2(String t) {this.token=t;}
	public T2(String t, Set<String> f, Set<String> b)
	{
		this.token = t;
		this.fSet = f; this.bSet = b;
	}
}
