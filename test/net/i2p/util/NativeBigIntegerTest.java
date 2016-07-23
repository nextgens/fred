package net.i2p.util;

import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Random;

import freenet.crypt.Global;

public class NativeBigIntegerTest extends TestCase {
	// Run with <code>ant -Dtest.benchmark=true</code> to do benchmark
	// and -XX:CompileThreshold=10 ... but JIT doesn't seem to make a difference.
	private static final boolean BENCHMARK = Boolean.getBoolean("test.benchmark");
	private static int numRuns = BENCHMARK ? 200 : 5;
	private int runsProcessed;

	/*
	 * the sample numbers are elG generator/prime so we can test with reasonable
	 * numbers
	 */
	private final static byte[] _sampleGenerator = Global.DSAgroupBigA.getG().toByteArray();
	private final static byte[] _samplePrime = Global.DSAgroupBigA.getP().toByteArray();

	private Random rand;

	TestIntegers nativeTest;
	TestIntegers javaTest;

	protected void setUp() throws Exception {
		if (!NativeBigInteger.isNative())
			printError("can't load native code");

		printInfo("DEBUG: Warming up the random number generator...");
		rand = new Random(0xAAAAAAAA);
		rand.nextBoolean();
		byte[] randbytes = (new BigInteger(2048, rand)).toByteArray();
		printInfo("DEBUG: Random number generator warmed up");
		javaTest = new TestIntegers(
			"java",
			new BigInteger(1, _sampleGenerator),
			new BigInteger(1, _samplePrime),
			new BigInteger(1, randbytes)
		);

		nativeTest = new TestIntegers(
			"native",
			new NativeBigInteger(1, _sampleGenerator),
			new NativeBigInteger(1, _samplePrime),
			new NativeBigInteger(1, randbytes)
		);
	}

	protected void tearDown() throws Exception {
		if(BENCHMARK) {
			if (numRuns == runsProcessed)
				printInfo("INFO: " + runsProcessed + " runs complete without any errors");
			else
				printError("ERROR: " + runsProcessed + " runs until we got an error");

			printInfo(nativeTest.getReport());
			printInfo(javaTest.getReport());
			printInfo("native = " + (nativeTest.getTime() * 100.0 / javaTest.getTime()) + "% of pure java time");
		}
	}

	public void testModPow() {
		for (runsProcessed = 0; runsProcessed < numRuns; runsProcessed++) {
			BigInteger nativeVal = nativeTest.modPow();
			BigInteger javaVal = javaTest.modPow();

			assertEquals(nativeVal, javaVal);
		}
	}

	private static void printInfo(String info) {
		System.out.println(info);
	}

	private static void printError(String info) {
		System.err.println(info);
	}

	static class TestIntegers {

		final String name;
		final BigInteger g;
		final BigInteger p;
		final BigInteger k;

		protected long time;
		protected int runs;

		public TestIntegers(String n, BigInteger g, BigInteger p, BigInteger k) {
			name = n;
			this.g = g;
			this.p = p;
			this.k = k;
		}

		public BigInteger modPow() {
			long start = System.currentTimeMillis();
			BigInteger r = g.modPow(p, k);
			time += System.currentTimeMillis() - start;
			++runs;
			return r;
		}

		public long getTime() {
			return time;
		}

		public String getReport() {
			return name + " run time: \t" + time + "ms (" + (time / runs) + "ms each)";
		}

	}
}
