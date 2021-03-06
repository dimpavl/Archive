package b_plus_tree.testing;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import b_plus_tree.*;

/// <summary>
/// tests main entry point for BplusJ.  Throws exception on failure.
/// </summary>
public class bplusTest {
	static String tempdirectory = "c:\\tmp"; // set to a directory to test storing to/from files
	static Hashtable allinserts = new Hashtable();
	static Hashtable lastcommittedinserts = new Hashtable();
	static boolean full = true;
	static int keylength = 20;
	static int prefixlength = 6;
	static int nodesize = 6;
	static int buffersize = 100;
	static int bucketsizelimit = 100; // sanity check
	static boolean DoAllTests = true;

	public static void main(final String argv[]) throws Exception {
		if (DoAllTests) {
			//byteStringTest();
			intTests();
			longTests();
			shortTests();
			testBufferFile();
			LinkedFileTest();
			BplusTreeLongTest();
			Test();
			xTest();
			hTest();
			//sTest(); -- not implemented in java yet
		}
		//hTest();
		CompatTest();
	}

	public static String CompatKey(final int i, final int j, final int k, final int l) {
		final String seed = "i=" + i + " j=" + j + " k=" + k + " ";
		final String result = seed;
		//			for (int ii=0; ii<l; ii++) 
		//			{
		//				result += seed;
		//			}
		return "" + l + result;
	}

	public static String CompatValue(final int i, final int j, final int k, final int l) {
		final String result = CompatKey(k, j, l, i) + CompatKey(l, k, j, i);
		return result + result;
	}

	public static void CompatTest() throws Exception {
		if (tempdirectory == null) {
			System.out
					.println(" compatibility test requires temp directory to be defined: please edit test source file");
			return;
		}
		final String otherTreeFileName = tempdirectory + "/CsharpTree.dat";
		final String otherBlocksFileName = tempdirectory + "/CsharpBlocks.dat";
		final String myTreeFileName = tempdirectory + "/JavaTree.dat";
		final String myBlocksFileName = tempdirectory + "/JavaBlocks.dat";
		final Hashtable map = new Hashtable();
		System.out.println(" creating " + myTreeFileName + " and " + myBlocksFileName);
		if ((new java.io.File(myTreeFileName)).exists()) {
			System.out.println(" deleting existing files");
			(new java.io.File(myTreeFileName)).delete();
			(new java.io.File(myBlocksFileName)).delete();
		}
		final BplusTree myTree = hBplusTree.Initialize(myTreeFileName, myBlocksFileName, 6);
		for (int i = 0; i < 10; i++) {

			System.out.println(" " + i);
			for (int j = 0; j < 10; j++)
				for (int k = 0; k < 10; k++)
					for (int l = 0; l < 10; l++) {
						final String TheKey = CompatKey(i, j, k, l);
						final String TheValue = CompatValue(i, j, k, l);
						//map[TheKey] = TheValue;
						map.put(TheKey, TheValue);
						//myTree[TheKey] = TheValue;
						myTree.set(TheKey, TheValue);
					}
		}
		myTree.Commit();
		myTree.Shutdown();
		System.out.println(" trying to test " + otherTreeFileName + " and " + otherBlocksFileName);
		if (!(new java.io.File(otherTreeFileName)).exists()) {
			System.out.println(" file not created yet :(");
			return;
		}
		int count = 0;
		final BplusTree otherTree = hBplusTree.ReadOnly(otherTreeFileName, otherBlocksFileName);
		//foreach (DictionaryEntry D in map) 
		for (final Enumeration e = map.keys(); e.hasMoreElements();) {
			if (count % 1000 == 1) System.out.println(" ... " + count);
			final String TheKey = (String) e.nextElement();
			final String TheValue = (String) map.get(TheKey);
			final String OtherValue = otherTree.get(TheKey);
			if (!OtherValue.equals(TheValue))
				throw new Exception(" Values don't match " + TheValue + " " + OtherValue);
			count++;
		}
		System.out.println(" compatibility test ok");
	}

	public static java.io.RandomAccessFile makeFile(final String name) throws Exception {
		if (tempdirectory == null) {
			System.out
					.println("to run these tests you need to edit the source file, adding a String definition for tempdirectory");
			throw new Exception(
					"to run these tests you need to edit the source file, adding a String definition for tempdirectory");
		}
		final String path = tempdirectory + "/" + name;
		// delete it if it exists
		final java.io.File f = new java.io.File(path);
		if (f.exists()) {
			System.out.println("<br>				DELETING FILE " + path);
			f.delete();
		}
		return new java.io.RandomAccessFile(path, "rw");
	}

	static String keyMaker(final int i, final int j, final int k) {
		final int selector = (i + j + k) % 3;
		String result = "" + i + "." + j + "." + k;
		if (selector == 0)
			result = "" + k + "." + j % 5 + "." + i;
		else if (selector == 1) result = "" + k + "." + j + "." + i;
		return result;
	}

	static String xkeyMaker(final int i, final int j, final int k) {
		String result = keyMaker(i, j, k);
		result = result + result + result;
		result = result + keyMaker(k, i, j);
		return result;
	}

	static String ValueMaker(final int i, final int j, final int k) {
		if ((i + j + k) % 5 == 3) return "";
		//System.Text.StringBuilder sb = new System.Text.StringBuilder();
		String result = "";
		//sb.Append("value");
		result += "value";
		for (int x = 0; x < i + k * 5; x++) {
			//sb.Append(j);
			result += j;
			//sb.Append(k);
			result += k;
		}
		return result;
	}

	public static void Test() throws Exception {
		System.out.println("TESTING BPLUSTREE");
		//System.IO.Stream treefile=null, blockfile=null;
		final java.io.RandomAccessFile treefile = makeFile("bptTree.dat");
		final java.io.RandomAccessFile blockfile = makeFile("bptBlock.dat");
		BplusTree bpt = BplusTree.Initialize(treefile, blockfile, keylength);
		//BplusTree bpt = getTree(treefile, blockfile);
		final Hashtable allmaps = new Hashtable();
		for (int i = 0; i < 10; i++) {
			System.out.println("Pass " + i + " of 10");
			bpt.SetFootPrintLimit(16 - i);
			for (int j = 0; j < 30; j++) {
				exercise(bpt, i, j, allmaps, false);
				if (j % 4 == 2) bpt = BplusTree.ReOpen(treefile, blockfile);
				// now check the structure
				checkStructure(allmaps, bpt, true);
			}
		}
	}

	public static void xTest() throws Exception {
		System.out.println("TESTING BPLUSTREE");
		//System.IO.Stream treefile=null, blockfile=null;
		final java.io.RandomAccessFile treefile = makeFile("bptTreeX.dat");
		final java.io.RandomAccessFile blockfile = makeFile("bptBlockX.dat");
		BplusTree bpt = xBplusTree.Initialize(treefile, blockfile, keylength);
		//BplusTree bpt = getTree(treefile, blockfile);
		final Hashtable allmaps = new Hashtable();
		for (int i = 0; i < 10; i++) {
			System.out.println("Pass " + i + " of 10");
			bpt.SetFootPrintLimit(16 - i);
			for (int j = 0; j < 30; j++) {
				exercise(bpt, i, j, allmaps, true);
				if (j % 4 == 2) bpt = xBplusTree.ReOpen(treefile, blockfile);
				// now check the structure
				checkStructure(allmaps, bpt, true);
			}
		}
	}

	public static void hTest() throws Exception {
		System.out.println("TESTING BPLUSTREE");
		//System.IO.Stream treefile=null, blockfile=null;
		final java.io.RandomAccessFile treefile = makeFile("bptTreeH.dat");
		final java.io.RandomAccessFile blockfile = makeFile("bptBlockH.dat");
		BplusTree bpt = hBplusTree.Initialize(treefile, blockfile, keylength);
		//BplusTree bpt = getTree(treefile, blockfile);
		final Hashtable allmaps = new Hashtable();
		for (int i = 0; i < 10; i++) {
			System.out.println("Pass " + i + " of 10");
			bpt.SetFootPrintLimit(16 - i);
			for (int j = 0; j < 30; j++) {
				exercise(bpt, i, j, allmaps, true);
				if (j % 4 == 2) bpt = hBplusTree.ReOpen(treefile, blockfile);
				// now check the structure
				checkStructure(allmaps, bpt, false);
			}
		}
	}

	public static void exercise(final IStringTree bpt, final int i, final int j, final Hashtable allmaps,
			final boolean extended) throws Exception {
		final Hashtable record = new Hashtable();
		for (int k = 0; k < 30; k++) {
			String thiskey;
			if (extended)
				thiskey = xkeyMaker(i, j, k);
			else
				thiskey = keyMaker(i, j, k);
			final String thisvalue = ValueMaker(j, k, i);
			//record[thiskey] = thisvalue;
			record.put(thiskey, thisvalue);
			//bpt[thiskey] = thisvalue;
			bpt.set(thiskey, thisvalue);
		}
		if (j % 3 == 1) bpt.Recover(false);
		if ((i + j) % 2 == 1) {
			bpt.Commit();
			bpt.Abort(); // should have no effect
			bpt.Commit(); // ditto
			if ((i + j) % 5 < 2) {
				//System.out.println(bpt.toHtml());
				//foreach (DictionaryEntry d in record) 
				for (final Enumeration e = record.keys(); e.hasMoreElements();) {
					final String thiskey = (String) e.nextElement();
					bpt.RemoveKey(thiskey);
					if (allmaps.containsKey(thiskey)) allmaps.remove(thiskey);
				}
				//System.out.println(bpt.toHtml());
				bpt.Commit();
				//return;
			} else
				//foreach (DictionaryEntry d in record) 
				for (final Enumeration e = record.keys(); e.hasMoreElements();) {
					//allmaps[d.Key] = d.Value;
					final String key = (String) e.nextElement();
					final String value = (String) record.get(key);
					allmaps.put(key, value);
				}
		} else
			bpt.Abort();
	}

	public static void checkStructure(final Hashtable allmaps, final IStringTree bpt, final boolean ordered)
			throws Exception {
		final TreeSet allkeys = new TreeSet();
		//ArrayList allkeys = new ArrayList();
		//foreach (DictionaryEntry d in allmaps) 
		for (final Enumeration e = allmaps.keys(); e.hasMoreElements();) {
			final String thiskey = (String) e.nextElement();
			final String thisvalue = (String) allmaps.get(thiskey);
			final String treemap = bpt.get(thiskey);
			if (!treemap.equals(thisvalue))
				throw new Exception("key " + thiskey + " maps to " + treemap + " but should map to " + thisvalue);
			allkeys.add(thiskey);
		}
		String currentkey = bpt.FirstKey();
		//allkeys.Sort();
		if (ordered) {
			for (final Iterator e = allkeys.iterator(); e.hasNext();) {
				final Object thing = e.next();
				final String recordedkey = (String) thing;
				if (currentkey == null) throw new Exception("end of keys found when expecting " + recordedkey);
				if (!currentkey.equals(recordedkey)) //System.out.println(bpt.toHtml());
					throw new Exception("key " + currentkey + " found where expecting " + recordedkey);
				currentkey = bpt.NextKey(currentkey);
			}
			if (currentkey != null) throw new Exception("found " + currentkey + " when expecting end of keys");
		}
		// should add test for unordered case too...
	}

	public static void abort(final BplusTreeLong bpt) throws Exception {
		System.out.println(" <h3>ABORT!</H3>");
		bpt.Abort();
		allinserts = (Hashtable) lastcommittedinserts.clone();
		checkit(bpt);
	}

	public static void commit(final BplusTreeLong bpt) throws Exception {
		System.out.println(" <h3>COMMIT!</H3>");
		bpt.Commit();
		lastcommittedinserts = (Hashtable) allinserts.clone();
		checkit(bpt);
	}

	public static BplusTreeLong restart(final BplusTreeLong bpt) throws Exception {
		System.out.println(" <h3>RESTART!</H3>");
		commit(bpt);
		return BplusTreeLong.SetupFromExistingStream(bpt.fromfile, bpt.seekStart);
	}

	public static void inserttest(final BplusTreeLong bpt, final String key, final long map) throws Exception {
		inserttest(bpt, key, map, false);
	}

	public static void deletetest(final BplusTreeLong bpt, final String key, final long map) throws Exception {
		inserttest(bpt, key, map, true);
	}

	public static void inserttest(final BplusTreeLong bpt, final String key, final long map, final boolean del)
			throws Exception {
		if (del) {
			System.out.println(" <h3>DELETE bpt[" + key + "] = " + map + "</h3>");
			bpt.RemoveKey(key);
			allinserts.remove(key);
		} else {
			System.out.println("<h3>bpt[" + key + "] = " + map + "</h3>");
			//bpt[key] = map;
			bpt.set(key, map);
			//allinserts[key] = map;
			allinserts.put(key, new Long(map));
		}
		checkit(bpt);
	}

	public static void checkit(final BplusTreeLong bpt) throws Exception {
		//System.out.println(bpt.toHtml());
		bpt.SanityCheck(true);
		final TreeSet allkeys = new TreeSet();
		//foreach (DictionaryEntry d in allinserts) 
		for (final Enumeration e = allinserts.keys(); e.hasMoreElements();)
			allkeys.add(e.nextElement());
		//allkeys.Sort();
		//allkeys.Reverse();
		//foreach (object thing in allkeys) 
		for (final Iterator e = allkeys.iterator(); e.hasNext();) {
			final Object thing = e.next();
			final String thekey = (String) thing;
			final long thevalue = ((Long) allinserts.get(thing)).longValue();
			if (thevalue != bpt.get(thekey))
				throw new Exception("no match on retrieval " + thekey + " --> " + bpt.get(thekey) + " not "
						+ thevalue);
		}
		//allkeys.Reverse();
		String currentkey = bpt.FirstKey();
		for (final Iterator e = allkeys.iterator(); e.hasNext();) {
			final Object thing = e.next();
			final String testkey = (String) thing;
			if (currentkey == null) throw new Exception("end of keys found when expecting " + testkey);
			if (!testkey.equals(currentkey))
				throw new Exception("when walking found " + currentkey + " when expecting " + testkey);
			currentkey = bpt.NextKey(testkey);
		}
	}

	public static void BplusTreeLongTest() throws Exception {
		System.out.println("TESTING BPLUSTREELONG"); // -- LOTS OF OUTPUT to System.out.println(...)");
		for (int nodesize = 2; nodesize < 6; nodesize++) {
			allinserts = new Hashtable();
			//System.IO.Stream mstream = new System.IO.MemoryStream();
			java.io.RandomAccessFile mstream = makeFile("jbpl.dat");
			final int keylength = 10 + nodesize;
			BplusTreeLong bpt = BplusTreeLong.InitializeInStream(mstream, keylength, nodesize);
			bpt = restart(bpt);
			//bpt["d"] = 15;
			inserttest(bpt, "d", 15);
			deletetest(bpt, "d", 15);
			inserttest(bpt, "d", 15);
			bpt.SerializationCheck();
			//bpt["ab"] = 55;
			inserttest(bpt, "ab", 55);
			//bpt["b"] = -5;
			inserttest(bpt, "b", -5);
			deletetest(bpt, "b", 0);
			inserttest(bpt, "b", -5);
			//return;
			//bpt["c"] = 34;
			inserttest(bpt, "c", 34);
			//bpt["a"] = 8;
			inserttest(bpt, "a", 8);
			commit(bpt);
			System.out.println("<h1>after commit</h1>\r\n");
			//System.out.println(bpt.toHtml());
			//bpt["a"] = 800;
			inserttest(bpt, "a", 800);
			//bpt["ca"]= -999;
			inserttest(bpt, "ca", -999);
			//bpt["da"]= -999;
			inserttest(bpt, "da", -999);
			//bpt["ea"]= -9991;
			inserttest(bpt, "ea", -9991);
			//bpt["aa"]= -9992;
			inserttest(bpt, "aa", -9992);
			//bpt["ba"]= -9995;
			inserttest(bpt, "ba", -9995);
			commit(bpt);
			//bpt["za"]= -9997;
			inserttest(bpt, "za", -9997);
			//bpt[" a"]= -9999;
			inserttest(bpt, " a", -9999);
			commit(bpt);
			deletetest(bpt, "d", 0);
			deletetest(bpt, "da", 0);
			deletetest(bpt, "ca", 0);
			bpt = restart(bpt);
			inserttest(bpt, "aaa", 88);
			System.out.println(" now doing torture test for " + nodesize);
			System.out.println("<h1>now doing torture test for " + nodesize + "</h1>");
			if (full) for (int i = 0; i < 33; i++) {
				for (int k = 0; k < 10; k++) {
					final int m = (i * 5 + k * 23) % 77;
					final String s = "b" + m;
					inserttest(bpt, s, m);
					if (i % 2 == 1 || k % 3 == 1) deletetest(bpt, s, m);
				}
				final int j = i % 3;
				if (j == 0)
					abort(bpt);
				else if (j == 1)
					commit(bpt);
				else
					bpt = restart(bpt);
			}
			commit(bpt);
			deletetest(bpt, "za", 0);
			deletetest(bpt, "ea", 0);
			deletetest(bpt, "c", 0);
			deletetest(bpt, "ba", 0);
			deletetest(bpt, "b", 0);
			deletetest(bpt, "ab", 0);
			abort(bpt);
			inserttest(bpt, "dog", 1);
			commit(bpt);
			deletetest(bpt, "dog", 1);
			inserttest(bpt, "pig", 2);
			abort(bpt);
			inserttest(bpt, "cat", 3);
			bpt.Recover(true);
			mstream.close();
			mstream = null;
		}
	}

	public static void LinkedFileTest() throws Exception {
		System.out.println("TESTING LINKED FILE");
		//System.IO.Stream mstream = new System.IO.MemoryStream();
		final java.io.RandomAccessFile mstream = makeFile("jlf.dat");
		// make a bunch of sample data
		final int asize = 200;
		//int asize = 2;
		final int maxsizing = 53;
		final int prime = 17;
		final int buffersize = 33;
		final String seedData = "a wop bop a loo bop";
		final byte[][] stuff = new byte[asize][];
		for (int i = 0; i < asize; i++)
			stuff[i] = makeSampleData(seedData, i * prime % maxsizing);
		// store them off
		LinkedFile lf = LinkedFile.InitializeLinkedFileInStream(mstream, buffersize, prime);
		lf.checkStructure();
		final long[] seeks = new long[asize];
		for (int i = 0; i < asize; i++) {
			seeks[i] = lf.StoreNewChunk(stuff[i], 0, stuff[i].length);
			// allocated it again and delete it off to mix things up...
			final long dummy = lf.StoreNewChunk(stuff[i], 0, stuff[i].length);
			lf.ReleaseBuffers(dummy);
			lf.checkStructure();
		}
		// delete the last one
		lf.ReleaseBuffers(seeks[asize - 1]);
		lf.checkStructure();
		lf.Flush();
		// create new handle
		lf = LinkedFile.SetupFromExistingStream(mstream, prime);
		// read them back and check (except for last)
		for (int i = 0; i < asize - 1; i++) {
			final byte[] retrieved = lf.GetChunk(seeks[i]);
			testByteArrays(retrieved, stuff[i]);
			// delete every so often
			if (i % prime == 1) {
				lf.ReleaseBuffers(seeks[i]);
				lf.checkStructure();
			}
		}
		lf.checkStructure();
		System.out.println("");
		System.out.println("linked file tests ok");
	}

	public static byte[] makeSampleData(final String testdata, final int sizing) throws Exception {
		if (testdata.length() < 1 || sizing < 1) return new byte[0];
		//System.Text.StringBuilder sb = new System.Text.StringBuilder();
		final java.io.ByteArrayOutputStream sb = new java.io.ByteArrayOutputStream();
		final byte[] bytedata = testdata.getBytes();
		for (int i = 0; i < sizing; i++) {
			//char c = testdata[i % testdata.length];
			//sb.Append(testdata);
			//sb.Append(c);
			sb.write(bytedata);
			final int index = i % bytedata.length;
			sb.write(bytedata, index, 1);
		}
		//String result = sb.ToString();
		//return System.Text.UTF8Encoding.ASCII.GetBytes(result);
		return sb.toByteArray();
	}

	public static void testBufferFile() throws Exception {
		System.out.println("TESTING BUFFERFILE");
		final int buffersize = 17;
		final int writesize = 10;
		//System.IO.Stream mstream = new System.IO.MemoryStream();
		final java.io.RandomAccessFile mstream = makeFile("jbf.dat");
		final int offset = 55;
		BufferFile bf = BufferFile.InitializeBufferFileInStream(mstream, buffersize, offset);
		final byte[] testheader = bf.makeHeader();
		final byte[] inputarray = makeSampleData("THIS IS SOME sample data off the cuff...", 100);
		final byte[] outputarray = new byte[inputarray.length];
		int position = 0;
		// shove in the testdata in reverse order
		for (int i = inputarray.length; i > writesize; i -= writesize) {
			System.out.println(" " + position);
			//Console.Write(" "+position);
			bf.setBuffer(position, inputarray, i - writesize, writesize);
			position++;
		}
		bf.setBuffer(position, inputarray, 0, writesize);
		// extract it again
		bf = BufferFile.SetupFromExistingStream(mstream, offset);
		position = 0;
		System.out.println("");
		//System.out.println("");
		for (int i = inputarray.length; i > writesize; i -= writesize) {
			System.out.println(" " + position);
			//Console.Write(" "+position);
			bf.getBuffer(position, outputarray, i - writesize, writesize);
			position++;
		}
		bf.getBuffer(position, outputarray, 0, writesize);
		testByteArrays(inputarray, outputarray);
		System.out.println("");
		System.out.println(" buffer file test ok");
	}

	public static void testByteArrays(final byte[] a, final byte[] b) throws Exception {
		if (a.length != b.length) throw new Exception("array lengths don't match " + a.length + " " + b.length);
		for (int i = 0; i < b.length; i++)
			if (a[i] != b[i]) throw new Exception("first error at " + i + " " + a[i] + " " + b[i]);
	}

	public static void intTests() throws Exception {
		final int bsize = 13;
		final byte[] buffer = new byte[bsize];
		final int[] ints = { 1, 566, -55, 32888, 4201010, 87878, -8989898 };
		int index = 99;
		//foreach (int theInt in ints) 
		for (final int theInt : ints) {
			index = Math.abs(index) % (bsize - 4);
			BufferFile.Store(theInt, buffer, index);
			final int otherInt = BufferFile.Retrieve(buffer, index);
			if (theInt != otherInt) throw new Exception("encode/decode int failed " + theInt + "!=" + otherInt);
			index = index + theInt;
		}
		System.out.println("encode/decode of ints ok");
	}

	public static void shortTests() throws Exception {
		final int bsize = 13;
		final byte[] buffer = new byte[bsize];
		final short[] shorts = { 1, 566, -32766, 32, 32755, 80, -8989 };
		int index = 99;
		//foreach (short theInt in shorts) 
		for (final short theInt : shorts) {
			index = Math.abs(index) % (bsize - 4);
			BufferFile.Store(theInt, buffer, index);
			final short otherInt = BufferFile.RetrieveShort(buffer, index);
			if (theInt != otherInt) throw new Exception("encode/decode int failed " + theInt + "!=" + otherInt);
			index = index + theInt;
		}
		System.out.println("encode/decode of longs ok");
	}

	public static void longTests() throws Exception {
		final int bsize = 17;
		final byte[] buffer = new byte[bsize];
		final long[] longs = { 1, 566, -55, 32888, 4201010, 87878, -8989898, 0xefaefabbccddeeL, -0xefaefabbccddeeL };
		int index = 99;
		//foreach (long theLong in longs) 
		for (final long theLong : longs) {
			index = Math.abs(index) % (bsize - 8);
			BufferFile.Store(theLong, buffer, index);
			final long otherLong = BufferFile.RetrieveLong(buffer, index);
			if (theLong != otherLong) throw new Exception("encode/decode int failed " + theLong + "!=" + otherLong);
			index = index + (int) (theLong & 0xffffff);
		}
		System.out.println("encode/decode of longs ok");
	}
}
